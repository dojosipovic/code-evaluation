import { CommonModule, Location } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { diffChars } from 'diff';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { KnobModule } from 'primeng/knob';
import { ProgressBarModule } from 'primeng/progressbar';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { SubmissionStatusEnum } from '../../models/enum/SubmissionStatusEnum';
import { SubmissionTestRunStatusEnum } from '../../models/enum/SubmissionTestRunStatusEnum';
import { TestResultEnum } from '../../models/enum/TestResultEnum';
import { ISubmissionDetailResponse } from '../../models/submission/ISubmissionDetailResponse';
import { ISubmissionSimilarityResponse } from '../../models/submission/ISubmissionSimilarityResponse';
import { ISubmissionTestResultResponse } from '../../models/submission/ISubmissionTestResultResponse';
import { ISubmissionTestRunResponse } from '../../models/submission/ISubmissionTestRunReponse';
import { BreadcrumbService } from '../../services/breadcrumb.service';
import { SubmissionService } from '../../services/submission.service';

interface DiffSegment {
  value: string;
  kind: 'same' | 'expected' | 'actual';
}

interface MonacoLayoutEditor {
  layout: () => void;
}

interface AnimatedSummaryMetrics {
  runtimeMs: number;
}

@Component({
  selector: 'app-submission-view',
  imports: [
    CommonModule,
    FormsModule,
    MonacoEditorModule,
    ButtonModule,
    KnobModule,
    ProgressBarModule,
    ProgressSpinnerModule,
    TagModule
  ],
  templateUrl: './submission-view.html',
  styleUrl: './submission-view.scss'
})
export class SubmissionView implements OnInit, OnDestroy {
  private readonly splitterGutterPx = 8;
  private readonly minCodeSectionPx = 360;
  private readonly minSideSectionPx = 320;
  private readonly similarityAnimationDelayMs = 450;
  private readonly similarityAnimationDurationMs = 1500;
  private readonly summaryAnimationDelayMs = 450;
  private readonly summaryAnimationDurationMs = 1200;
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);
  private submissionService = inject(SubmissionService);
  private breadcrumbService = inject(BreadcrumbService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private similarityAnimationFrameId: number | null = null;
  private summaryAnimationFrameId: number | null = null;
  private testsProgressAnimationFrameId: number | null = null;
  private testsProgressAnimationTimeoutId: number | null = null;
  private editorLayoutFrameId: number | null = null;
  private editorResizeObserver: ResizeObserver | null = null;
  private monacoEditor: MonacoLayoutEditor | null = null;
  private resizing = false;
  private readonly onPointerMove = (event: MouseEvent) => this.handlePointerMove(event);
  private readonly onPointerUp = () => this.stopResize();

  @ViewChild('contentGrid') contentGridRef?: ElementRef<HTMLElement>;
  @ViewChild('editorShell') editorShellRef?: ElementRef<HTMLElement>;

  readonly loading = signal(true);
  readonly animatedSimilarities = signal<Record<number, number>>({});
  readonly animatedSummaryMetrics = signal<AnimatedSummaryMetrics>({
    runtimeMs: 0
  });
  readonly testsProgressPercent = signal(0);

  submission: ISubmissionDetailResponse | null = null;
  activeCaseIndex = 0;
  sideSectionPx = 480;
  private sourceGroupId: number | null = null;

  readonly editorOptions = {
    theme: 'vs-dark',
    language: 'cpp',
    automaticLayout: true,
    readOnly: true,
    minimap: { enabled: false },
    fontSize: 14,
    lineHeight: 22,
    scrollBeyondLastLine: false,
    wordWrap: 'off',
    scrollbar: {
      alwaysConsumeMouseWheel: false,
      horizontal: 'auto',
      horizontalScrollbarSize: 10,
      verticalScrollbarSize: 10
    }
  };

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const submissionId = Number(params.get('id'));

        if (!Number.isFinite(submissionId)) {
          this.router.navigate(['/dashboard'], { replaceUrl: true });
          return;
        }

        const groupId = Number(this.route.snapshot.queryParamMap.get('groupId'));
        this.sourceGroupId = Number.isFinite(groupId) ? groupId : null;
        this.loadSubmission(submissionId);
      });
  }

  ngOnDestroy(): void {
    this.stopSimilarityAnimation();
    this.stopSummaryAnimation();
    this.stopTestsProgressAnimation();
    this.stopResize();
    this.stopEditorLayout();

    this.editorResizeObserver?.disconnect();
    this.editorResizeObserver = null;
  }

  get latestTestRun(): ISubmissionTestRunResponse | null {
    const runs = this.submission?.testRuns ?? [];

    if (runs.length === 0) {
      return null;
    }

    return [...runs].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())[0];
  }

  get cases(): ISubmissionTestResultResponse[] {
    return this.latestTestRun?.testResults ?? [];
  }

  get selectedCase(): ISubmissionTestResultResponse | null {
    return this.cases[this.activeCaseIndex] ?? this.cases[0] ?? null;
  }

  get passedLabel(): string {
    const testRun = this.latestTestRun;

    if (!testRun) {
      return '-';
    }

    return `${testRun.passedTests}/${testRun.totalTests}`;
  }

  get runtimeLabel(): string {
    const testRun = this.latestTestRun;

    if (!testRun || testRun.runtimeMs === null) {
      return '-';
    }

    return `${this.animatedSummaryMetrics().runtimeMs} ms`;
  }

  get testsProgressValue(): number {
    const testRun = this.latestTestRun;

    if (!testRun || testRun.totalTests <= 0) {
      return 0;
    }

    return this.testsProgressPercent();
  }

  get scoreLabel(): string {
    const score = this.submission?.finalScore;

    return score === null || score === undefined ? '-' : `${score}`;
  }

  get submittedAtLabel(): string {
    return this.submission ? this.formatDateTime(this.submission.submittedAt) : '-';
  }

  get hasCode(): boolean {
    return !!this.submission?.code?.trim();
  }

  goBack(): void {
    this.location.back();
  }

  selectCase(index: number): void {
    this.activeCaseIndex = index;
  }

  onEditorInit(editorInstance: MonacoLayoutEditor): void {
    this.monacoEditor = editorInstance;
    this.bindEditorResizeObserver();
    this.scheduleEditorLayout();
  }

  startResize(event: MouseEvent): void {
    event.preventDefault();
    this.resizing = true;
    document.body.classList.add('submission-resizing');
    window.addEventListener('mousemove', this.onPointerMove);
    window.addEventListener('mouseup', this.onPointerUp);
  }

  trackByCaseIndex(index: number): number {
    return index;
  }

  trackBySimilarityId(_: number, similarity: ISubmissionSimilarityResponse): number {
    return similarity.id;
  }

  getStatusLabel(status: SubmissionStatusEnum): string {
    return status.replace(/_/g, ' ');
  }

  getStatusSeverity(status: SubmissionStatusEnum): 'success' | 'danger' | 'info' | 'warn' | 'secondary' {
    switch (status) {
      case SubmissionStatusEnum.TESTED:
      case SubmissionStatusEnum.PLAGIARISM_ANALYZED:
        return 'success';
      case SubmissionStatusEnum.FAILED:
        return 'danger';
      case SubmissionStatusEnum.QUEUED:
        return 'warn';
      case SubmissionStatusEnum.SUBMITTED:
      default:
        return 'info';
    }
  }

  getRunStatusSeverity(status: SubmissionTestRunStatusEnum): 'success' | 'danger' | 'info' | 'warn' | 'secondary' {
    switch (status) {
      case SubmissionTestRunStatusEnum.COMPLETED:
        return 'success';
      case SubmissionTestRunStatusEnum.FAILED:
        return 'danger';
      case SubmissionTestRunStatusEnum.RUNNING:
      case SubmissionTestRunStatusEnum.QUEUED:
        return 'warn';
      default:
        return 'secondary';
    }
  }

  getCaseStatusClass(testCase: ISubmissionTestResultResponse): string {
    return testCase.result === TestResultEnum.PASSED ? 'status-passed' : 'status-failed';
  }

  getCaseStatusLabel(testCase: ISubmissionTestResultResponse): string {
    switch (testCase.result) {
      case TestResultEnum.PASSED:
        return 'Accepted';
      case TestResultEnum.WRONG_ANSWER:
        return 'Wrong Answer';
      case TestResultEnum.RUNTIME_ERROR:
        return 'Runtime Error';
      case TestResultEnum.TIME_LIMIT_EXCEEDED:
        return 'Time Limit';
      case TestResultEnum.INTERNAL_ERROR:
      default:
        return 'Internal Error';
    }
  }

  getCaseStatusSeverity(testCase: ISubmissionTestResultResponse): 'success' | 'danger' | 'warn' {
    if (testCase.result === TestResultEnum.PASSED) {
      return 'success';
    }

    return testCase.result === TestResultEnum.TIME_LIMIT_EXCEEDED ? 'warn' : 'danger';
  }

  getAnimatedSimilarityPercent(similarity: ISubmissionSimilarityResponse): number {
    return this.animatedSimilarities()[similarity.id] ?? 0;
  }

  getSimilarityPercent(similarity: ISubmissionSimilarityResponse): number {
    const score = similarity.similarityScore;
    const normalized = score <= 1 ? score * 100 : score;

    return Math.round(this.clamp(normalized, 0, 100));
  }

  getSimilaritySeverity(similarity: ISubmissionSimilarityResponse): string {
    const percent = this.getSimilarityPercent(similarity);

    if (percent >= 80) {
      return 'high';
    }

    if (percent >= 45) {
      return 'medium';
    }

    return 'low';
  }

  canShowExpectedOutput(testCase: ISubmissionTestResultResponse): boolean {
    return testCase.showExpectedOutput;
  }

  hasCaseInput(input: string | null | undefined): boolean {
    return !!input?.trim();
  }

  getExpectedOutputText(testCase: ISubmissionTestResultResponse): string {
    if (testCase.expectedOutput === null) {
      return 'No output is expected.';
    }

    return testCase.expectedOutput;
  }

  getActualOutput(testCase: ISubmissionTestResultResponse): string {
    return testCase.actualOutput || testCase.errorOutput || '';
  }

  canShowOutputDiff(testCase: ISubmissionTestResultResponse | null): boolean {
    return !!testCase
      && this.canShowExpectedOutput(testCase)
      && testCase.expectedOutput !== null
      && !!this.getActualOutput(testCase);
  }

  getExpectedDiffSegments(testCase: ISubmissionTestResultResponse): DiffSegment[] {
    return this.getDiffSegments(testCase, 'expected');
  }

  getActualDiffSegments(testCase: ISubmissionTestResultResponse): DiffSegment[] {
    return this.getDiffSegments(testCase, 'actual');
  }

  formatTestText(value: string | null | undefined): string {
    if (!value) {
      return '';
    }

    return value
      .replace(/\r\n/g, '\\r\\n\r\n')
      .replace(/\n/g, '\\n\n')
      .replace(/\r/g, '\\r\r');
  }

  formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('hr-HR', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  formatBytes(value: number | null): string {
    if (value === null) {
      return '-';
    }

    if (value < 1024) {
      return `${value} B`;
    }

    const units = ['KB', 'MB', 'GB'];
    let size = value / 1024;
    let unitIndex = 0;

    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex++;
    }

    return `${size.toFixed(size >= 10 ? 0 : 1)} ${units[unitIndex]}`;
  }

  private loadSubmission(submissionId: number): void {
    this.loading.set(true);
    this.submission = null;
    this.activeCaseIndex = 0;
    this.animatedSimilarities.set({});
    this.animatedSummaryMetrics.set({
      runtimeMs: 0
    });
    this.testsProgressPercent.set(0);
    this.stopSimilarityAnimation();
    this.stopSummaryAnimation();
    this.stopTestsProgressAnimation();

    this.submissionService.getSubmission(submissionId)
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.cdr.detectChanges();
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: submission => {
          this.submission = submission;
          this.updateBreadcrumb();
          this.animateSummaryMetrics();
          this.animateTestsProgress();
          this.animateSimilarities(submission.similarities);
          this.scheduleEditorLayout();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti submission'
          });
          this.router.navigate(['/dashboard']);
        }
      });
  }

  private updateBreadcrumb(): void {
    const submissionLabel = this.submission ? `Submission #${this.submission.id}` : 'Submission';

    if (!this.sourceGroupId) {
      this.breadcrumbService.set([
        { label: submissionLabel }
      ]);
      return;
    }

    this.breadcrumbService.set([
      { label: 'Grupe', routerLink: '/groups' },
      { label: `Group #${this.sourceGroupId}`, routerLink: `/groups/${this.sourceGroupId}/tasks` },
      { label: submissionLabel }
    ]);
  }

  private animateSimilarities(similarities: ISubmissionSimilarityResponse[]): void {
    this.stopSimilarityAnimation();

    if (similarities.length === 0) {
      return;
    }

    const startTime = performance.now() + this.similarityAnimationDelayMs;

    const step = (now: number) => {
      const elapsed = Math.max(now - startTime, 0);
      const progress = Math.min(elapsed / this.similarityAnimationDurationMs, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const values = similarities.reduce<Record<number, number>>((acc, similarity) => {
        acc[similarity.id] = Math.round(this.getSimilarityPercent(similarity) * eased);
        return acc;
      }, {});

      this.animatedSimilarities.set(values);

      if (progress < 1) {
        this.similarityAnimationFrameId = requestAnimationFrame(step);
      } else {
        this.similarityAnimationFrameId = null;
      }
    };

    this.similarityAnimationFrameId = requestAnimationFrame(step);
  }

  private animateSummaryMetrics(): void {
    this.stopSummaryAnimation();

    const testRun = this.latestTestRun;

    if (!testRun) {
      return;
    }

    const targetMetrics: AnimatedSummaryMetrics = {
      runtimeMs: testRun.runtimeMs ?? 0
    };
    const startTime = performance.now() + this.summaryAnimationDelayMs;

    const step = (now: number) => {
      const elapsed = Math.max(now - startTime, 0);
      const progress = Math.min(elapsed / this.summaryAnimationDurationMs, 1);
      const eased = 1 - Math.pow(1 - progress, 3);

      this.animatedSummaryMetrics.set({
        runtimeMs: Math.round(targetMetrics.runtimeMs * eased)
      });

      if (progress < 1) {
        this.summaryAnimationFrameId = requestAnimationFrame(step);
      } else {
        this.animatedSummaryMetrics.set(targetMetrics);
        this.summaryAnimationFrameId = null;
      }
    };

    this.summaryAnimationFrameId = requestAnimationFrame(step);
  }

  private animateTestsProgress(): void {
    this.stopTestsProgressAnimation();

    const testRun = this.latestTestRun;

    if (!testRun || testRun.totalTests <= 0) {
      this.testsProgressPercent.set(0);
      return;
    }

    const targetPercent = (testRun.passedTests / testRun.totalTests) * 100;

    this.testsProgressPercent.set(0);
    this.testsProgressAnimationTimeoutId = window.setTimeout(() => {
      this.testsProgressAnimationTimeoutId = null;
      this.testsProgressAnimationFrameId = requestAnimationFrame(() => {
        this.testsProgressAnimationFrameId = requestAnimationFrame(() => {
          this.testsProgressPercent.set(targetPercent);
          this.testsProgressAnimationFrameId = null;
        });
      });
    }, this.summaryAnimationDelayMs);
  }

  private stopSimilarityAnimation(): void {
    if (this.similarityAnimationFrameId !== null) {
      cancelAnimationFrame(this.similarityAnimationFrameId);
      this.similarityAnimationFrameId = null;
    }
  }

  private stopSummaryAnimation(): void {
    if (this.summaryAnimationFrameId !== null) {
      cancelAnimationFrame(this.summaryAnimationFrameId);
      this.summaryAnimationFrameId = null;
    }
  }

  private stopTestsProgressAnimation(): void {
    if (this.testsProgressAnimationTimeoutId !== null) {
      clearTimeout(this.testsProgressAnimationTimeoutId);
      this.testsProgressAnimationTimeoutId = null;
    }

    if (this.testsProgressAnimationFrameId !== null) {
      cancelAnimationFrame(this.testsProgressAnimationFrameId);
      this.testsProgressAnimationFrameId = null;
    }
  }

  private stopEditorLayout(): void {
    if (this.editorLayoutFrameId !== null) {
      cancelAnimationFrame(this.editorLayoutFrameId);
      this.editorLayoutFrameId = null;
    }
  }

  private stopResize(): void {
    if (!this.resizing) {
      return;
    }

    this.resizing = false;
    document.body.classList.remove('submission-resizing');
    window.removeEventListener('mousemove', this.onPointerMove);
    window.removeEventListener('mouseup', this.onPointerUp);
  }

  private handlePointerMove(event: MouseEvent): void {
    if (!this.resizing) {
      return;
    }

    this.zone.run(() => {
      const contentGrid = this.contentGridRef?.nativeElement;

      if (!contentGrid) {
        return;
      }

      const rect = contentGrid.getBoundingClientRect();
      const rawSidePx = rect.right - event.clientX;
      const maxSidePx = Math.max(
        this.minSideSectionPx,
        rect.width - this.minCodeSectionPx - this.splitterGutterPx
      );

      this.sideSectionPx = this.clamp(rawSidePx, this.minSideSectionPx, maxSidePx);
      this.cdr.detectChanges();
      this.scheduleEditorLayout();
    });
  }

  private scheduleEditorLayout(): void {
    const editorInstance = this.monacoEditor;

    if (!editorInstance) {
      return;
    }

    this.stopEditorLayout();
    this.editorLayoutFrameId = requestAnimationFrame(() => {
      this.editorLayoutFrameId = requestAnimationFrame(() => {
        this.editorLayoutFrameId = null;
        editorInstance.layout();
      });
    });
  }

  private bindEditorResizeObserver(): void {
    const editorShell = this.editorShellRef?.nativeElement;

    if (!editorShell || typeof ResizeObserver === 'undefined') {
      return;
    }

    this.editorResizeObserver?.disconnect();
    this.editorResizeObserver = new ResizeObserver(() => {
      this.scheduleEditorLayout();
    });
    this.editorResizeObserver.observe(editorShell);
  }

  private getDiffSegments(testCase: ISubmissionTestResultResponse, side: 'expected' | 'actual'): DiffSegment[] {
    const expected = testCase.expectedOutput ?? '';
    const actual = this.getActualOutput(testCase);

    return diffChars(expected, actual)
      .filter(part => {
        if (side === 'expected') {
          return !part.added;
        }

        return !part.removed;
      })
      .map(part => ({
        value: this.formatTestText(part.value),
        kind: part.added ? 'actual' : part.removed ? 'expected' : 'same'
      }));
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }
}
