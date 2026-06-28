import { CommonModule, Location } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import confetti from 'canvas-confetti';
import { diffChars } from 'diff';
import { marked } from 'marked';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ProgressBarModule } from 'primeng/progressbar';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { IAssignmentResponse } from '../../models/assignment/IAssignmentResponse';
import { IAssignmentRunResponse } from '../../models/assignment/IAssignmentRunResponse';
import { IAssignmentRunTestResult } from '../../models/assignment/IAssignmentRunTestResult';
import { TestResultEnum } from '../../models/enum/TestResultEnum';
import { TestVisibilityEnum } from '../../models/enum/TestVisibilityEnum';
import { ITestResponse } from '../../models/task/ITestResponse';
import { AssignmentService } from '../../services/assignment.service';

interface MonacoLayoutEditor {
  layout: () => void;
}

interface DiffSegment {
  value: string;
  kind: 'same' | 'expected' | 'actual';
}

@Component({
  selector: 'app-assignment-solve',
  imports: [
    CommonModule,
    FormsModule,
    MonacoEditorModule,
    ButtonModule,
    MessageModule,
    ProgressBarModule,
    ProgressSpinnerModule,
    TagModule,
    TooltipModule
  ],
  templateUrl: './assignment-solve.html',
  styleUrl: './assignment-solve.scss',
})
export class AssignmentSolve implements OnInit, OnDestroy {
  private readonly horizontalGutterPx = 8;
  private readonly minLeftPanePx = 0;
  private readonly minRightPanePx = 320;
  private readonly minCodePanePx = 260;
  private readonly minTestsPanePx = 220;
  private readonly dangerTimerThresholdMs = 2 * 60 * 1000;
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);
  private sanitizer = inject(DomSanitizer);
  private assignmentService = inject(AssignmentService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);
  private dragMode: 'horizontal' | 'vertical' | null = null;
  private monacoEditor: MonacoLayoutEditor | null = null;
  private layoutFrameId: number | null = null;
  private progressAnimationFrameId: number | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private countdownTimerId: ReturnType<typeof setTimeout> | null = null;
  private hasExpiredAssignment = false;
  private readonly onPointerMove = (event: MouseEvent) => this.handlePointerMove(event);
  private readonly onPointerUp = () => this.stopDragging();

  @ViewChild('workspace') workspaceRef?: ElementRef<HTMLElement>;
  @ViewChild('rightColumn') rightColumnRef?: ElementRef<HTMLElement>;
  @ViewChild('editorShell') editorShellRef?: ElementRef<HTMLElement>;

  readonly loading = signal(true);
  readonly running = signal(false);
  readonly compilePopupVisible = signal(false);
  readonly runProgressMode = signal<'hidden' | 'indeterminate' | 'determinate'>('hidden');
  readonly runProgressValue = signal(0);
  readonly runProgressPassed = signal(0);
  readonly runProgressTotal = signal(0);
  readonly currentTime = signal(Date.now());

  assignment: IAssignmentResponse | null = null;
  runResponse: IAssignmentRunResponse | null = null;
  activeCaseIndex = 0;
  code = '';
  leftPanePx = 430;
  topPanePx = 420;

  editorOptions = {
    theme: 'vs-dark',
    language: 'cpp',
    automaticLayout: true,
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

  constructor() {
    marked.setOptions({
      breaks: true,
      gfm: true
    });
  }

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const assignmentId = Number(params.get('id'));

        if (!Number.isFinite(assignmentId)) {
          this.router.navigate(['/dashboard'], { replaceUrl: true });
          return;
        }

        this.loadAssignment(assignmentId);
      });
  }

  ngOnDestroy(): void {
    this.stopDragging();
    this.stopProgressAnimation();
    this.clearCountdownTimer();

    if (this.layoutFrameId !== null) {
      cancelAnimationFrame(this.layoutFrameId);
      this.layoutFrameId = null;
    }

    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
  }

  get renderedMarkdown(): SafeHtml {
    const description = this.assignment?.task.description ?? '';
    const rawHtml = marked.parse(description) as string;
    return this.sanitizer.bypassSecurityTrustHtml(rawHtml);
  }

  get publicTests(): ITestResponse[] {
    return this.assignment?.task.tests.filter(test => test.visibility === TestVisibilityEnum.PUBLIC) ?? [];
  }

  get hiddenTestsCount(): number {
    return this.assignment?.task.tests.filter(test => test.visibility === TestVisibilityEnum.HIDDEN).length ?? 0;
  }

  get languageLabel(): string {
    return this.assignment?.task.starterCode.language?.toUpperCase() || 'CPP';
  }

  get passedLabel(): string {
    if (!this.runResponse) {
      return '-';
    }

    return `${this.runProgressPassed()}/${this.runProgressTotal()}`;
  }

  get showRunProgress(): boolean {
    return this.runProgressMode() !== 'hidden';
  }

  get countdownLabel(): string {
    const totalSeconds = Math.max(0, Math.floor(this.remainingTimeMs / 1000));
    const days = Math.floor(totalSeconds / (24 * 60 * 60));
    const hours = Math.floor((totalSeconds % (24 * 60 * 60)) / (60 * 60));
    const minutes = Math.floor((totalSeconds % (60 * 60)) / 60);
    const seconds = totalSeconds % 60;
    const timeLabel = `${this.padTimeUnit(hours)}:${this.padTimeUnit(minutes)}:${this.padTimeUnit(seconds)}`;

    return days > 0 ? `${days}d ${timeLabel}` : timeLabel;
  }

  get countdownAccentColor(): string {
    if (this.remainingTimeMs <= 0) {
      return 'var(--p-red-500)';
    }

    if (this.remainingTimeMs <= this.dangerTimerThresholdMs) {
      return 'var(--p-red-500)';
    }

    return 'var(--p-text-color)';
  }

  get countdownBorderColor(): string {
    return this.remainingTimeMs <= this.dangerTimerThresholdMs
      ? 'color-mix(in srgb, var(--p-red-500) 48%, var(--p-content-border-color))'
      : 'var(--p-content-border-color)';
  }

  get countdownBackground(): string {
    return this.remainingTimeMs <= this.dangerTimerThresholdMs
      ? 'color-mix(in srgb, var(--p-red-500) 10%, var(--p-content-hover-background))'
      : 'color-mix(in srgb, var(--p-content-hover-background) 60%, transparent)';
  }

  selectCase(index: number): void {
    this.activeCaseIndex = index;
  }

  runCode(): void {
    if (!this.assignment || this.running()) {
      return;
    }

    this.running.set(true);
    this.compilePopupVisible.set(false);
    this.stopProgressAnimation();
    this.runProgressMode.set('indeterminate');
    this.runProgressValue.set(0);
    this.runProgressPassed.set(0);
    this.runProgressTotal.set(0);

    this.assignmentService.runAssignment(this.assignment.id, { code: this.code })
      .pipe(
        finalize(() => {
          this.running.set(false);
          this.cdr.detectChanges();
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: response => {
          this.runResponse = response;
          this.compilePopupVisible.set(response.compile.exitCode !== 0 && !!(response.compile.stderr || response.compile.stdout));
          this.animateProgressTo(response.passedCount, response.totalCount);
          this.triggerPerfectRunCelebration(response.passedCount, response.totalCount);
        },
        error: () => {
          this.runProgressMode.set('hidden');
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce pokrenuti kod'
          });
        }
      });
  }

  goBack(): void {
    this.location.back();
  }

  startHorizontalResize(event: MouseEvent): void {
    event.preventDefault();
    this.startDragging('horizontal');
  }

  startVerticalResize(event: MouseEvent): void {
    event.preventDefault();
    this.startDragging('vertical');
  }

  onEditorInit(editorInstance: MonacoLayoutEditor): void {
    this.monacoEditor = editorInstance;
    this.bindEditorResizeObserver();
    this.scheduleEditorLayout();
  }

  trackByCaseIndex(index: number): number {
    return index;
  }

  isHiddenResult(result: IAssignmentRunTestResult): boolean {
    return result.visibility === TestVisibilityEnum.HIDDEN;
  }

  canShowExpectedOutput(result: IAssignmentRunTestResult): boolean {
    return result.showExpectedOutput;
  }

  canShowRunOutput(result: IAssignmentRunTestResult): boolean {
    return !this.isHiddenResult(result) && (!!result.stdout?.trim() || !!result.stderr?.trim());
  }

  hasPublicExpectedOutput(test: ITestResponse): boolean {
    return !!test.output?.trim();
  }

  getResultLabel(result: TestResultEnum): string {
    switch (result) {
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

  getResultSeverity(result: TestResultEnum): 'success' | 'danger' | 'warn' {
    if (result === TestResultEnum.PASSED) {
      return 'success';
    }

    return result === TestResultEnum.TIME_LIMIT_EXCEEDED ? 'warn' : 'danger';
  }

  getResultClass(result: TestResultEnum): string {
    return result === TestResultEnum.PASSED ? 'result-passed' : 'result-failed';
  }

  get cases(): {
    index: number;
    test: ITestResponse;
    result: IAssignmentRunTestResult | null;
  }[] {
    const tests = this.assignment?.task.tests ?? [];
    const resultsByIndex = new Map((this.runResponse?.results ?? []).map(result => [result.index, result]));

    return tests.map((test, index) => ({
      index,
      test,
      result: resultsByIndex.get(index) ?? null
    }));
  }

  get selectedCase(): {
    index: number;
    test: ITestResponse;
    result: IAssignmentRunTestResult | null;
  } | null {
    return this.cases[this.activeCaseIndex] ?? this.cases[0] ?? null;
  }

  getExpectedOutputText(result: IAssignmentRunTestResult): string {
    if (result.expectedOutput === null) {
      return 'No output is expected.';
    }

    return result.expectedOutput;
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

  hasCaseInput(input: string | null | undefined): boolean {
    return !!input?.trim();
  }

  getCaseResult(index: number): IAssignmentRunTestResult | null {
    return this.runResponse?.results.find(result => result.index === index) ?? null;
  }

  hasCompileFailed(): boolean {
    return !!this.runResponse && this.runResponse.compile.exitCode !== 0;
  }

  getCompileOutput(): string {
    if (!this.runResponse) {
      return '';
    }

    return this.runResponse.compile.stderr || this.runResponse.compile.stdout || '';
  }

  closeCompilePopup(): void {
    this.compilePopupVisible.set(false);
  }

  getCaseOutput(result: IAssignmentRunTestResult | null): string {
    if (!result) {
      return '';
    }

    return result.stdout || result.stderr || '';
  }

  canShowOutputDiff(result: IAssignmentRunTestResult | null): boolean {
    return !!result
      && this.canShowExpectedOutput(result)
      && result.expectedOutput !== null
      && !!this.getCaseOutput(result);
  }

  getExpectedDiffSegments(result: IAssignmentRunTestResult): DiffSegment[] {
    return this.getDiffSegments(result, 'expected');
  }

  getActualDiffSegments(result: IAssignmentRunTestResult): DiffSegment[] {
    return this.getDiffSegments(result, 'actual');
  }

  getCaseStatusClass(result: IAssignmentRunTestResult | null): string {
    if (!result) {
      return 'status-neutral';
    }

    return result.testResult === TestResultEnum.PASSED ? 'status-passed' : 'status-failed';
  }

  getCaseStatusLabel(result: IAssignmentRunTestResult | null): string {
    if (!result) {
      return 'Not run';
    }

    return result.testResult === TestResultEnum.PASSED ? 'Passed' : 'Failed';
  }

  private getDiffSegments(result: IAssignmentRunTestResult, side: 'expected' | 'actual'): DiffSegment[] {
    const expected = result.expectedOutput ?? '';
    const actual = this.getCaseOutput(result);

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

  private triggerPerfectRunCelebration(passedCount: number, totalCount: number): void {
    if (totalCount === 0 || passedCount !== totalCount) {
      return;
    }

    void this.zone.runOutsideAngular(() => {
      const burst = (originX: number, angle: number) => confetti({
        particleCount: 140,
        angle,
        spread: 100,
        startVelocity: 58,
        gravity: 0.9,
        scalar: 1.05,
        origin: { x: originX, y: 1 }
      });

      burst(0, 50);
      burst(1, 130);

      setTimeout(() => {
        burst(0.08, 60);
        burst(0.92, 120);
      }, 180);
    });
  }

  private animateProgressTo(passedCount: number, totalCount: number): void {
    this.stopProgressAnimation();

    this.runProgressTotal.set(totalCount);
    this.runProgressPassed.set(0);
    this.runProgressValue.set(0);
    this.runProgressMode.set('determinate');

    const durationMs = 600;
    const startTime = performance.now();

    const step = (now: number) => {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / durationMs, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const interpolatedPassed = Math.round(passedCount * eased);
      const interpolatedValue = totalCount > 0 ? Math.round((interpolatedPassed / totalCount) * 100) : 0;

      this.runProgressPassed.set(interpolatedPassed);
      this.runProgressValue.set(interpolatedValue);

      if (progress < 1) {
        this.progressAnimationFrameId = requestAnimationFrame(step);
      } else {
        this.progressAnimationFrameId = null;
        this.runProgressPassed.set(passedCount);
        this.runProgressValue.set(totalCount > 0 ? Math.round((passedCount / totalCount) * 100) : 0);
      }
    };

    this.progressAnimationFrameId = requestAnimationFrame(step);
  }

  private stopProgressAnimation(): void {
    if (this.progressAnimationFrameId !== null) {
      cancelAnimationFrame(this.progressAnimationFrameId);
      this.progressAnimationFrameId = null;
    }
  }

  private startDragging(mode: 'horizontal' | 'vertical'): void {
    this.dragMode = mode;
    document.body.classList.add('solve-resizing');
    window.addEventListener('mousemove', this.onPointerMove);
    window.addEventListener('mouseup', this.onPointerUp);
  }

  private stopDragging(): void {
    this.dragMode = null;
    document.body.classList.remove('solve-resizing');
    window.removeEventListener('mousemove', this.onPointerMove);
    window.removeEventListener('mouseup', this.onPointerUp);
  }

  private handlePointerMove(event: MouseEvent): void {
    this.zone.run(() => {
      if (this.dragMode === 'horizontal') {
        const workspace = this.workspaceRef?.nativeElement;

        if (!workspace) {
          return;
        }

        const rect = workspace.getBoundingClientRect();
        const rawLeftPx = event.clientX - rect.left;
        const maxLeftPx = Math.max(this.minLeftPanePx, rect.width - this.minRightPanePx - this.horizontalGutterPx);
        this.leftPanePx = this.clamp(rawLeftPx, this.minLeftPanePx, maxLeftPx);
        this.cdr.detectChanges();
        this.scheduleEditorLayout();
        return;
      }

      if (this.dragMode === 'vertical') {
        const rightColumn = this.rightColumnRef?.nativeElement;

        if (!rightColumn) {
          return;
        }

        const rect = rightColumn.getBoundingClientRect();
        const rawTopPx = event.clientY - rect.top;
        const maxTopPx = Math.max(this.minCodePanePx, rect.height - this.minTestsPanePx - this.horizontalGutterPx);
        this.topPanePx = this.clamp(rawTopPx, this.minCodePanePx, maxTopPx);
        this.cdr.detectChanges();
        this.scheduleEditorLayout();
      }
    });
  }

  private scheduleEditorLayout(): void {
    const editorInstance = this.monacoEditor;

    if (!editorInstance) {
      return;
    }

    if (this.layoutFrameId !== null) {
      cancelAnimationFrame(this.layoutFrameId);
    }

    this.layoutFrameId = requestAnimationFrame(() => {
      this.layoutFrameId = null;
      editorInstance.layout();
    });
  }

  private bindEditorResizeObserver(): void {
    const editorShell = this.editorShellRef?.nativeElement;

    if (!editorShell || typeof ResizeObserver === 'undefined') {
      return;
    }

    this.resizeObserver?.disconnect();
    this.resizeObserver = new ResizeObserver(() => {
      this.scheduleEditorLayout();
    });
    this.resizeObserver.observe(editorShell);
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), max);
  }

  private get remainingTimeMs(): number {
    const endsAt = this.assignment ? new Date(this.assignment.endsAt).getTime() : NaN;

    if (!Number.isFinite(endsAt)) {
      return 0;
    }

    return Math.max(endsAt - this.currentTime(), 0);
  }

  private padTimeUnit(value: number): string {
    return value.toString().padStart(2, '0');
  }

  private loadAssignment(assignmentId: number): void {
    this.loading.set(true);
    this.hasExpiredAssignment = false;
    this.clearCountdownTimer();
    this.runResponse = null;
    this.compilePopupVisible.set(false);
    this.runProgressMode.set('hidden');
    this.runProgressValue.set(0);
    this.runProgressPassed.set(0);
    this.runProgressTotal.set(0);
    this.stopProgressAnimation();

    this.assignmentService.getAssignment(assignmentId)
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.cdr.detectChanges();
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: assignment => {
          this.assignment = assignment;
          this.code = assignment.task.includeStarterCode ? assignment.task.starterCode.code : '';
          this.activeCaseIndex = 0;
          this.currentTime.set(Date.now());
          this.editorOptions = {
            ...this.editorOptions,
            language: assignment.task.starterCode.language || 'cpp'
          };
          this.handleCountdownTick();
        },
        error: () => {
          this.assignment = null;
          this.runProgressMode.set('hidden');
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti assignment'
          });
          this.router.navigate(['/dashboard']);
        }
      });
  }

  private handleCountdownTick(): void {
    if (!this.assignment) {
      this.clearCountdownTimer();
      return;
    }

    const endsAt = new Date(this.assignment.endsAt).getTime();

    if (!Number.isFinite(endsAt)) {
      this.clearCountdownTimer();
      return;
    }

    const now = Date.now();
    this.currentTime.set(now);

    if (now >= endsAt) {
      this.clearCountdownTimer();
      this.closeExpiredAssignment();
      return;
    }

    this.scheduleCountdownRefresh();
  }

  private scheduleCountdownRefresh(): void {
    this.clearCountdownTimer();

    const delay = 1000 - (Date.now() % 1000);
    this.countdownTimerId = setTimeout(() => this.handleCountdownTick(), Math.max(250, delay));
  }

  private clearCountdownTimer(): void {
    if (this.countdownTimerId === null) {
      return;
    }

    clearTimeout(this.countdownTimerId);
    this.countdownTimerId = null;
  }

  private closeExpiredAssignment(): void {
    if (this.hasExpiredAssignment) {
      return;
    }

    this.hasExpiredAssignment = true;
    this.messageService.add({
      severity: 'info',
      summary: 'Vrijeme je isteklo',
      detail: 'Assignment je zatvoren jer vise nema preostalog vremena.'
    });
    this.router.navigate(['/dashboard'], { replaceUrl: true });
  }
}
