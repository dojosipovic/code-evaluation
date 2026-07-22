import { CommonModule, Location } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Observable, expand, finalize, forkJoin, map, reduce } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChartModule } from 'primeng/chart';
import { DialogModule } from 'primeng/dialog';
import { ProgressBarModule } from 'primeng/progressbar';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import type { ChartData, ChartOptions, Plugin } from 'chart.js';
import { IAssignmentResponse } from '../../models/assignment/IAssignmentResponse';
import { ISubmissionListItem } from '../../models/submission/ISubmissionListItem';
import { IUserResponse } from '../../models/user/IUserResponse';
import { AssignmentService } from '../../services/assignment.service';
import { AuthService } from '../../services/auth/auth.service';
import { BreadcrumbService } from '../../services/breadcrumb.service';
import { PlagScanService } from '../../services/plagscan.service';
import { SubmissionService } from '../../services/submission.service';
import { SubmissionView } from '../submission-view/submission-view';

interface ScoreDistributionPoint {
  from: number;
  to: number;
  count: number;
  range: string;
}

@Component({
  selector: 'app-assignment-submissions',
  imports: [
    CommonModule,
    RouterModule,
    ButtonModule,
    ChartModule,
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
  scoreDistributionData: ChartData<'bar', number[], string> | null = null;
  scoreDistributionOptions: ChartOptions<'bar'> = {};
  scoreDistributionWidth = '100%';
  readonly averageLinePlugins: Plugin<'bar'>[] = [
    {
      id: 'averageLine',
      afterDatasetsDraw: chart => {
        const averageLine = (chart.options.plugins as {
          averageLine?: { average?: number; scaleValue?: number };
        })?.averageLine;
        const average = averageLine?.average;
        const scaleValue = averageLine?.scaleValue;

        if (
          average === undefined ||
          scaleValue === undefined ||
          !Number.isFinite(average) ||
          !Number.isFinite(scaleValue)
        ) {
          return;
        }

        const { ctx, chartArea, scales } = chart;
        const xScale = scales['x'];

        if (!xScale) {
          return;
        }

        const x = xScale.getPixelForValue(scaleValue);

        ctx.save();
        ctx.strokeStyle = '#e11d48';
        ctx.lineWidth = 2;
        ctx.setLineDash([6, 5]);
        ctx.beginPath();
        ctx.moveTo(x, chartArea.top);
        ctx.lineTo(x, chartArea.bottom);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.fillStyle = '#e11d48';
        ctx.font = '700 12px system-ui, -apple-system, Segoe UI, sans-serif';
        ctx.textAlign = x > chartArea.right - 64 ? 'right' : 'left';
        ctx.textBaseline = 'top';
        ctx.fillText(`Prosjek ${average.toFixed(2)}`, x + (x > chartArea.right - 64 ? -8 : 8), chartArea.top + 6);
        ctx.restore();
      }
    }
  ];
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
          this.updateScoreDistributionChart();
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

  private updateScoreDistributionChart(): void {
    if (!this.assignment || !this.submissions.length) {
      this.scoreDistributionData = null;
      this.scoreDistributionOptions = {};
      this.scoreDistributionWidth = '100%';
      return;
    }

    const maxPoints = Math.max(this.assignment.points, 0);
    const scores = this.submissions.map(submission => this.clampScore(this.getSubmissionScore(submission), maxPoints));
    const averageScore = scores.reduce((sum, score) => sum + score, 0) / scores.length;
    const bucketSize = this.getScoreBucketSize(maxPoints);
    const bucketCount = Math.max(1, Math.ceil(maxPoints / bucketSize));
    const buckets: ScoreDistributionPoint[] = Array.from({ length: bucketCount }, (_, index) => {
      const from = index * bucketSize;
      const to = index === bucketCount - 1 ? maxPoints : Math.min(maxPoints, from + bucketSize);

      return {
        from,
        to,
        count: 0,
        range: `${this.formatPoints(from)} - ${this.formatPoints(to)}`
      };
    });

    for (const score of scores) {
      const bucketIndex = maxPoints === 0
        ? 0
        : Math.min(bucketCount - 1, Math.floor(score / bucketSize));

      buckets[bucketIndex].count += 1;
    }

    this.scoreDistributionWidth = `max(100%, ${Math.max(720, bucketCount * 46)}px)`;
    this.scoreDistributionData = {
      labels: buckets.map(bucket => bucket.range),
      datasets: [
        {
          label: 'Broj predaja',
          data: buckets.map(bucket => bucket.count),
          backgroundColor: '#2563eb',
          borderColor: '#1d4ed8',
          borderWidth: 1,
          borderRadius: 5,
          barPercentage: 0.9,
          categoryPercentage: 0.9
        }
      ]
    };

    this.scoreDistributionOptions = {
      maintainAspectRatio: false,
      responsive: true,
      plugins: {
        averageLine: {
          average: averageScore,
          scaleValue: this.getAverageScaleValue(averageScore, bucketSize, bucketCount)
        },
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            title: items => `Bodovi ${items[0]?.label ?? ''}`,
            label: item => `Predaje: ${item.parsed.y}`
          }
        }
      },
      scales: {
        x: {
          title: {
            display: true,
            text: 'Bodovi'
          },
          ticks: {
            autoSkip: true,
            maxRotation: 0,
            minRotation: 0
          },
          grid: {
            color: 'rgba(148, 163, 184, 0.22)'
          }
        },
        y: {
          beginAtZero: true,
          title: {
            display: true,
            text: 'Broj predaja'
          },
          ticks: {
            precision: 0
          },
          grid: {
            color: 'rgba(148, 163, 184, 0.22)'
          }
        }
      }
    } as ChartOptions<'bar'>;
  }

  private getScoreBucketSize(maxPoints: number): number {
    if (maxPoints <= 0) {
      return 1;
    }

    const targetBucketCount = 40;
    const rawBucketSize = maxPoints / targetBucketCount;
    const bucketSizes = [
      0.05,
      0.1,
      0.2,
      0.25,
      0.5,
      1,
      1.25,
      2,
      2.5,
      5,
      10,
      12.5,
      20,
      25,
      50
    ];

    return bucketSizes.find(bucketSize => bucketSize >= rawBucketSize) ?? Math.ceil(rawBucketSize);
  }

  private getAverageScaleValue(averageScore: number, bucketSize: number, bucketCount: number): number {
    if (bucketCount <= 1) {
      return 0;
    }

    return Math.min(bucketCount - 1, Math.max(0, averageScore / bucketSize - 0.5));
  }

  private clampScore(score: number, maxPoints: number): number {
    if (!Number.isFinite(score)) {
      return 0;
    }

    return Math.min(Math.max(score, 0), maxPoints);
  }

  private formatPoints(points: number): string {
    if (Number.isInteger(points)) {
      return points.toString();
    }

    return Number(points.toFixed(2)).toString();
  }
}
