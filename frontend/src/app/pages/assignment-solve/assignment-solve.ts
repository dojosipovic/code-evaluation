import { CommonModule, Location } from '@angular/common';
import { ChangeDetectorRef, Component, DestroyRef, ElementRef, NgZone, OnDestroy, OnInit, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { marked } from 'marked';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { IAssignmentResponse } from '../../models/assignment/IAssignmentResponse';
import { IAssignmentRunResponse } from '../../models/assignment/IAssignmentRunResponse';
import { IAssignmentRunTestResult } from '../../models/assignment/IAssignmentRunTestResult';
import { TestResultEnum } from '../../models/enum/TestResultEnum';
import { TestVisibilityEnum } from '../../models/enum/TestVisibilityEnum';
import { ITestResponse } from '../../models/task/ITestResponse';
import { AssignmentService } from '../../services/assignment.service';

type SolveTab = 'tests' | 'results';

@Component({
  selector: 'app-assignment-solve',
  imports: [
    CommonModule,
    FormsModule,
    MonacoEditorModule,
    ButtonModule,
    MessageModule,
    ProgressSpinnerModule,
    TabsModule,
    TagModule,
    TooltipModule
  ],
  templateUrl: './assignment-solve.html',
  styleUrl: './assignment-solve.scss',
})
export class AssignmentSolve implements OnInit, OnDestroy {
  private readonly horizontalGutterPx = 8;
  private readonly minLeftPanePx = 260;
  private readonly minRightPanePx = 640;
  private readonly minCodePanePx = 260;
  private readonly minTestsPanePx = 220;
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
  private monacoEditor: any = null;
  private layoutFrameId: number | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private readonly onPointerMove = (event: MouseEvent) => this.handlePointerMove(event);
  private readonly onPointerUp = () => this.stopDragging();

  @ViewChild('workspace') workspaceRef?: ElementRef<HTMLElement>;
  @ViewChild('rightColumn') rightColumnRef?: ElementRef<HTMLElement>;
  @ViewChild('editorShell') editorShellRef?: ElementRef<HTMLElement>;

  readonly loading = signal(true);
  readonly running = signal(false);

  assignment: IAssignmentResponse | null = null;
  runResponse: IAssignmentRunResponse | null = null;
  activeTab: SolveTab = 'tests';
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

    return `${this.runResponse.passedCount}/${this.runResponse.totalCount}`;
  }

  onTabChange(value: string | number | undefined): void {
    if (value === 'tests' || value === 'results') {
      this.activeTab = value;
    }
  }

  runCode(): void {
    if (!this.assignment || this.running()) {
      return;
    }

    this.running.set(true);

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
          this.activeTab = 'results';
        },
        error: () => {
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

  onEditorInit(editor: any): void {
    this.monacoEditor = editor;
    this.bindEditorResizeObserver();
    this.scheduleEditorLayout();
  }

  trackByTestIndex(index: number): number {
    return index;
  }

  isHiddenResult(result: IAssignmentRunTestResult): boolean {
    return result.testVisibility === TestVisibilityEnum.HIDDEN;
  }

  canShowExpectedOutput(result: IAssignmentRunTestResult): boolean {
    return !this.isHiddenResult(result) && !!result.expectedOutput?.trim();
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
    if (!this.monacoEditor) {
      return;
    }

    if (this.layoutFrameId !== null) {
      cancelAnimationFrame(this.layoutFrameId);
    }

    this.layoutFrameId = requestAnimationFrame(() => {
      this.layoutFrameId = null;
      this.monacoEditor.layout();
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

  private loadAssignment(assignmentId: number): void {
    this.loading.set(true);
    this.runResponse = null;

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
          this.editorOptions = {
            ...this.editorOptions,
            language: assignment.task.starterCode.language || 'cpp'
          };
        },
        error: () => {
          this.assignment = null;
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti assignment'
          });
          this.router.navigate(['/dashboard']);
        }
      });
  }
}
