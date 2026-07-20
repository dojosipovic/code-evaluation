import { CommonModule, Location } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, expand, finalize, forkJoin, map, reduce } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { ProgressBarModule } from 'primeng/progressbar';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { IAssignmentResponse } from '../../models/assignment/IAssignmentResponse';
import { ISubmissionListItem } from '../../models/submission/ISubmissionListItem';
import { IUserResponse } from '../../models/user/IUserResponse';
import { AssignmentService } from '../../services/assignment.service';
import { AuthService } from '../../services/auth/auth.service';
import { BreadcrumbService } from '../../services/breadcrumb.service';
import { PlagScanService } from '../../services/plagscan.service';
import { SubmissionService } from '../../services/submission.service';
import { SubmissionView } from '../submission-view/submission-view';

@Component({
  selector: 'app-assignment-submissions',
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    DialogModule,
    ProgressBarModule,
    ProgressSpinnerModule,
    TagModule,
    SubmissionView
  ],
  templateUrl: './assignment-submissions.html',
  styleUrl: './assignment-submissions.scss'
})
export class AssignmentSubmissions implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);
  private destroyRef = inject(DestroyRef);
  private authService = inject(AuthService);
  private assignmentService = inject(AssignmentService);
  private plagScanService = inject(PlagScanService);
  private submissionService = inject(SubmissionService);
  private messageService = inject(MessageService);
  private breadcrumbService = inject(BreadcrumbService);
  private readonly pageSize = 100;

  readonly loading = signal(true);
  readonly openingPlagScan = signal(false);

  assignment: IAssignmentResponse | null = null;
  submissions: ISubmissionListItem[] = [];
  plagScanReportExists = false;
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

        this.loadSubmissions(assignmentId);
      });
  }

  goBack(): void {
    this.location.back();
  }

  openPlagScanViewer(): void {
    if (!this.assignment || this.openingPlagScan()) {
      return;
    }

    const viewerWindow = window.open('about:blank', '_blank');

    if (viewerWindow) {
      viewerWindow.opener = null;
    }

    this.openingPlagScan.set(true);

    this.authService.getPlagScanToken(this.assignment.id)
      .pipe(
        finalize(() => this.openingPlagScan.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({ accessToken }) => {
          const viewerUrl = this.plagScanService.getReportViewerUrl(accessToken);

          if (viewerWindow) {
            viewerWindow.location.href = viewerUrl;
            return;
          }

          window.open(viewerUrl, '_blank');
        },
        error: () => {
          viewerWindow?.close();
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce otvoriti PlagScan izvjestaj'
          });
        }
      });
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

  getSubmissionScore(submission: ISubmissionListItem): number {
    if (submission.finalScore !== null && submission.finalScore !== undefined) {
      return submission.finalScore;
    }

    if (!this.assignment || !submission.totalTests) {
      return 0;
    }

    return Math.round((submission.passedTests / submission.totalTests) * this.assignment.points * 100) / 100;
  }

  trackBySubmission(_: number, submission: ISubmissionListItem): number {
    return submission.id;
  }

  private loadSubmissions(assignmentId: number): void {
    this.loading.set(true);
    this.assignment = null;
    this.submissions = [];
    this.plagScanReportExists = false;

    forkJoin({
      assignment: this.assignmentService.getAssignment(assignmentId),
      submissions: this.loadAllSubmissions(assignmentId),
      plagScanReportExists: this.plagScanService.reportExists(assignmentId)
    })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: ({ assignment, submissions, plagScanReportExists }) => {
          this.assignment = assignment;
          this.submissions = submissions;
          this.plagScanReportExists = plagScanReportExists;
          this.updateBreadcrumb();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti predaje'
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

  private updateBreadcrumb(): void {
    if (!this.assignment) {
      return;
    }

    this.breadcrumbService.set([
      { label: 'Grupe', routerLink: '/groups' },
      { label: `Group #${this.assignment.groupId}`, routerLink: `/groups/${this.assignment.groupId}/tasks` },
      { label: this.breadcrumbService.shorten(this.assignment.name) },
      { label: 'Predaje' }
    ]);
  }
}
