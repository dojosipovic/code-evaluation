import { CommonModule, Location } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Observable,
  expand,
  finalize,
  forkJoin,
  map,
  reduce
} from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageService } from 'primeng/api';
import { PopoverModule } from 'primeng/popover';
import { ProgressBarModule } from 'primeng/progressbar';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { IAssignmentResponse } from '../../models/assignment/IAssignmentResponse';
import { IPlagScanCluster } from '../../models/plagscan/IPlagScanCluster';
import { ISubmissionListItem } from '../../models/submission/ISubmissionListItem';
import { IUserResponse } from '../../models/user/IUserResponse';
import { AssignmentService } from '../../services/assignment.service';
import { BreadcrumbService } from '../../services/breadcrumb.service';
import { PlagScanService } from '../../services/plagscan.service';
import { SubmissionService } from '../../services/submission.service';
import { SubmissionView } from '../submission-view/submission-view';

interface SubmissionGradeFormRow {
  submission: ISubmissionListItem;
  calculatedPoints: number;
  grade: number;
  confirmed: boolean;
}

@Component({
  selector: 'app-assignment-evaluation',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    ButtonModule,
    DialogModule,
    InputNumberModule,
    PopoverModule,
    ProgressBarModule,
    ProgressSpinnerModule,
    SkeletonModule,
    TagModule,
    SubmissionView
  ],
  templateUrl: './assignment-evaluation.html',
  styleUrl: './assignment-evaluation.scss'
})
export class AssignmentEvaluation implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);
  private destroyRef = inject(DestroyRef);
  private assignmentService = inject(AssignmentService);
  private submissionService = inject(SubmissionService);
  private plagScanService = inject(PlagScanService);
  private messageService = inject(MessageService);
  private breadcrumbService = inject(BreadcrumbService);
  private readonly pageSize = 100;

  readonly loading = signal(true);
  readonly skeletonRows = Array.from({ length: 4 });

  assignment: IAssignmentResponse | null = null;
  gradingRows: SubmissionGradeFormRow[] = [];
  plagScanClusters: IPlagScanCluster[] = [];
  selectedClusterId: number | null = null;
  submissionDialogOpen = false;
  viewedSubmissionId: number | null = null;

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const assignmentId = Number(params.get('id'));

        if (!Number.isFinite(assignmentId)) {
          this.router.navigate(['/groups'], { replaceUrl: true });
          return;
        }

        this.loadEvaluation(assignmentId);
      });
  }

  goBack(): void {
    this.location.back();
  }

  onGradeChanged(row: SubmissionGradeFormRow): void {
    row.confirmed = false;
  }

  confirmGrade(row: SubmissionGradeFormRow): void {
    row.confirmed = true;
  }

  editGrade(row: SubmissionGradeFormRow): void {
    row.confirmed = false;
  }

  onEvaluate(): void {
    return;
  }

  openSubmissionDialog(submissionId: number, event?: Event): void {
    event?.stopPropagation();
    this.viewedSubmissionId = submissionId;
    this.submissionDialogOpen = true;
  }

  onSubmissionDialogVisibleChange(visible: boolean): void {
    this.submissionDialogOpen = visible;

    if (!visible) {
      this.viewedSubmissionId = null;
    }
  }

  get canEvaluateGrades(): boolean {
    return this.gradingRows.length > 0 && this.gradingRows.every(row => row.confirmed);
  }

  getUserDisplayName(user: IUserResponse | null | undefined): string {
    if (!user) {
      return '-';
    }

    const fullName = `${user.firstname ?? ''} ${user.lastname ?? ''}`.trim();

    return fullName || user.username || user.email || '-';
  }

  getSubmissionTestsLabel(submission: ISubmissionListItem): string {
    return `${submission.passedTests ?? 0}/${submission.totalTests ?? 0}`;
  }

  getSubmissionTestPercent(submission: ISubmissionListItem): number {
    if (!submission.totalTests) {
      return 0;
    }

    return Math.round((submission.passedTests / submission.totalTests) * 100);
  }

  getClusterSimilarityLabel(cluster: IPlagScanCluster): string {
    const similarity = cluster.similarity <= 1 ? cluster.similarity * 100 : cluster.similarity;

    return `${Math.round(similarity)}%`;
  }

  getSubmissionClusterCount(submission: ISubmissionListItem): number {
    return this.plagScanClusters.filter(cluster =>
      cluster.members.some(member => member.submissionId === submission.id)
    ).length;
  }

  getSubmissionSimilarityCount(submission: ISubmissionListItem): number {
    return submission.similarityCount ?? 0;
  }

  isSubmissionInSelectedCluster(submission: ISubmissionListItem): boolean {
    const selectedCluster = this.getSelectedCluster();

    if (!selectedCluster) {
      return false;
    }

    return selectedCluster.members.some(member => member.submissionId === submission.id);
  }

  toggleSelectedCluster(clusterId: number): void {
    this.selectedClusterId = this.selectedClusterId === clusterId ? null : clusterId;
  }

  trackByGradeRow(_: number, row: SubmissionGradeFormRow): number {
    return row.submission.id;
  }

  trackByCluster(_: number, cluster: IPlagScanCluster): number {
    return cluster.id;
  }

  trackByClusterMember(_: number, member: { id: number }): number {
    return member.id;
  }

  private loadEvaluation(assignmentId: number): void {
    this.loading.set(true);
    this.assignment = null;
    this.gradingRows = [];
    this.plagScanClusters = [];
    this.selectedClusterId = null;

    forkJoin({
      assignment: this.assignmentService.getAssignment(assignmentId),
      submissions: this.loadAllSubmissions(assignmentId),
      clusters: this.loadAllClusters(assignmentId)
    })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({ assignment, submissions, clusters }) => {
          this.assignment = assignment;
          this.gradingRows = submissions.map(submission => {
            const calculatedPoints = this.calculateSubmissionPoints(submission, assignment.points);

            return {
              submission,
              calculatedPoints,
              grade: calculatedPoints,
              confirmed: false
            };
          });
          this.plagScanClusters = clusters;
          this.updateBreadcrumb();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti podatke za ocjenjivanje'
          });
          this.router.navigate(['/groups']);
        }
      });
  }

  private loadAllSubmissions(assignmentId: number): Observable<ISubmissionListItem[]> {
    return this.submissionService.getSubmissions({
      page: 0,
      size: this.pageSize,
      assignmentId,
      sortBy: 'submittedAt',
      sortDir: 'desc'
    }).pipe(
      expand(response => response.hasNext
        ? this.submissionService.getSubmissions({
          page: response.page + 1,
          size: this.pageSize,
          assignmentId,
          sortBy: 'submittedAt',
          sortDir: 'desc'
        })
        : EMPTY
      ),
      map(response => response.items),
      reduce((allItems, items) => [...allItems, ...items], [] as ISubmissionListItem[])
    );
  }

  private loadAllClusters(assignmentId: number): Observable<IPlagScanCluster[]> {
    return this.plagScanService.getClusters({
      page: 0,
      size: this.pageSize,
      assignmentId,
      sortBy: 'similarity',
      sortDir: 'desc'
    }).pipe(
      expand(response => response.hasNext
        ? this.plagScanService.getClusters({
          page: response.page + 1,
          size: this.pageSize,
          assignmentId,
          sortBy: 'similarity',
          sortDir: 'desc'
        })
        : EMPTY
      ),
      map(response => response.items),
      reduce((allItems, items) => [...allItems, ...items], [] as IPlagScanCluster[])
    );
  }

  private calculateSubmissionPoints(submission: ISubmissionListItem, assignmentPoints: number): number {
    if (!submission.totalTests) {
      return submission.finalScore ?? 0;
    }

    const points = (submission.passedTests / submission.totalTests) * assignmentPoints;

    return Math.round(points * 100) / 100;
  }

  private getSelectedCluster(): IPlagScanCluster | null {
    if (this.selectedClusterId === null) {
      return null;
    }

    return this.plagScanClusters.find(cluster => cluster.id === this.selectedClusterId) ?? null;
  }

  private updateBreadcrumb(): void {
    if (!this.assignment) {
      return;
    }

    this.breadcrumbService.set([
      { label: 'Grupe', routerLink: '/groups' },
      { label: `Group #${this.assignment.groupId}`, routerLink: `/groups/${this.assignment.groupId}/tasks` },
      { label: this.breadcrumbService.shorten(this.assignment.name) },
      { label: 'Ocjenjivanje' }
    ]);
  }
}
