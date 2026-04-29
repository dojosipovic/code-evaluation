import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import { ButtonModule } from 'primeng/button';
import { TabsModule } from 'primeng/tabs';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { InputText } from "primeng/inputtext";
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { SelectModule } from 'primeng/select';
import { CPP_STARTER_TEMPLATE, TASK_MARKDOWN_TEMPLATE } from '../../config/task-templates';
import { StepperModule } from 'primeng/stepper';
import { MessageModule } from 'primeng/message';
import { ITaskCreate } from '../../models/task/ITaskCreate';
import { ITestCase } from '../../models/task/ITestCase';
import { TaskService } from '../../services/task.service';
import { ConfirmationService } from 'primeng/api';
import { Component, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
import { ITaskResponse } from '../../models/task/ITaskResponse';
import { TestVisibilityEnum } from '../../models/enum/TestVisibilityEnum';
import { ChangeDetectorRef } from '@angular/core';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-task-create-dialog',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TabsModule,
    MonacoEditorModule,
    InputText,
    ToggleSwitchModule,
    SelectModule,
    StepperModule,
    MessageModule,
    ProgressSpinnerModule
],
  templateUrl: './task-create-dialog.html',
  styleUrl: './task-create-dialog.scss',
})
export class TaskCreateDialog implements OnInit {
  @Output() closed = new EventEmitter<void>();

  activeStep = 1;
  isClosing = false;

  readonly MIN_TITLE_LENGTH = 3;
  readonly MIN_DESCRIPTION_LENGTH = 100;
  readonly MIN_PUBLIC_TESTS = 3;
  readonly MIN_PRIVATE_TESTS = 3;

  submitted = false;

  private nextTestId = 1;
  private sanitizer = inject(DomSanitizer);
  private taskService = inject(TaskService);
  private confirmationService = inject(ConfirmationService);
  private cdr = inject(ChangeDetectorRef);

  @Input() taskId: number | null = null;
  @Output() saved = new EventEmitter<void>();

  modelReady = false;

  languageOptions = [
    { label: 'C++', value: 'cpp' }
  ];

  model: ITaskCreate = this.createEmptyModel();

  editorOptions = {
    theme: 'vs-dark',
    language: 'cpp',
    automaticLayout: true,
    minimap: { enabled: false },
    readOnly: false,
    domReadOnly: false
  };

  private updateEditorOptions(): void {
    this.editorOptions = {
      ...this.editorOptions,
      readOnly: !this.model.includeStarterCode,
      domReadOnly: !this.model.includeStarterCode
    };
  }

  onIncludeStarterCodeChange(value: boolean): void {
    this.model.includeStarterCode = value;
    this.updateEditorOptions();
  }

  constructor() {
    this.nextTestId = 2;
    marked.setOptions({
      breaks: true,
      gfm: true
    });

    this.updateEditorOptions();
  }

  ngOnInit(): void {
    this.activeStep = 1;
    this.submitted = false;
    this.modelReady = false;

    if (this.taskId == null) {
      this.model = this.createEmptyModel();
      this.nextTestId = this.getNextTestId();
      this.updateEditorOptions();

      setTimeout(() => {
        this.modelReady = true;
        this.cdr.detectChanges();
      });
      return;
    }

    this.taskService.getTask(this.taskId).subscribe({
      next: task => {
        try {
          this.model = this.mapTaskResponseToEditorModel(task);
          this.nextTestId = this.getNextTestId();
          this.updateEditorOptions();

          setTimeout(() => {
            this.modelReady = true;
            this.cdr.detectChanges();
          });
        } catch (e) {
          console.error('Map task failed:', e, task);
          this.closed.emit();
        }
      },
      error: error => {
        console.error('Get task failed:', error);
        this.closed.emit();
      }
    });
  }

  private mapTaskResponseToEditorModel(task: ITaskResponse): ITaskCreate {
    let nextTestId = 1;

    return {
      id: task.id,
      title: task.title,
      description: task.description,
      starterCode: {
        language: task.starterCode.language,
        code: task.starterCode.code
      },
      includeStarterCode: task.includeStarterCode,

      publicTests: task.tests
        .filter(test => test.visibility === TestVisibilityEnum.PUBLIC)
        .map(test => ({
          id: nextTestId++,
          input: test.input,
          output: test.output
        })),

      hiddenTests: task.tests
        .filter(test => test.visibility === TestVisibilityEnum.HIDDEN)
        .map(test => ({
          id: nextTestId++,
          input: test.input,
          output: test.output
        }))
    };
  }

  get isEditMode(): boolean {
    return !!this.model.id;
  }

  private getNextTestId(): number {
    const allTests = [...this.model.publicTests, ...this.model.hiddenTests];
    const maxId = allTests.length ? Math.max(...allTests.map(test => test.id)) : 0;
    return maxId + 1;
  }

  get renderedMarkdown(): SafeHtml {
    const rawHtml = marked.parse(this.model.description) as string;
    return this.sanitizer.bypassSecurityTrustHtml(rawHtml);
  }

  onClose(): void {
    this.confirmationService.confirm({
      message: `Jesi siguran da želiš prekinuti kreiranje zadatka?`,
      header: 'Potvrda',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Izlaz',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        this.closeDialog();
      }
    });
  }

  private closeDialog(): void {
    this.isClosing = true;

    setTimeout(() => {
      this.closed.emit();
    }, 200);
  }

  addPublicTest(): void {
    this.model.publicTests.push({
      id: this.nextTestId++,
      input: '',
      output: ''
    });
  }

  addPrivateTest(): void {
    this.model.hiddenTests.push({
      id: this.nextTestId++,
      input: '',
      output: ''
    });
  }

  removePublicTest(id: number): void {
    this.model.publicTests = this.model.publicTests.filter(test => test.id !== id);
  }

  removePrivateTest(id: number): void {
    this.model.hiddenTests = this.model.hiddenTests.filter(test => test.id !== id);
  }

  trackByTestId(_index: number, test: ITestCase): number {
    return test.id;
  }

  get isTitleValid(): boolean {
    const title = this.model.title.trim();
    return title.length >= this.MIN_TITLE_LENGTH && /[a-zA-ZčćžšđČĆŽŠĐ]/.test(title);
  }

  get isDescriptionValid(): boolean {
    return this.model.description.trim().length >= this.MIN_DESCRIPTION_LENGTH;
  }

  get arePublicTestsValid(): boolean {
    return this.model.publicTests.length >= this.MIN_PUBLIC_TESTS
  }

  get arePrivateTestsValid(): boolean {
    return this.model.hiddenTests.length >= this.MIN_PRIVATE_TESTS;
  }

  canGoToStep(step: number): boolean {
    if (step === 1) return true;
    if (step === 2) return this.isTitleValid && this.isDescriptionValid;
    if (step === 3) return this.isTitleValid && this.isDescriptionValid && this.arePublicTestsValid;
    if (step === 4) return this.isTitleValid && this.isDescriptionValid && this.arePublicTestsValid && this.arePrivateTestsValid;

    return false;
  }

  goToStep(step: number): void {
    this.submitted = true;

    if (this.canGoToStep(step)) {
      this.activeStep = step;
    }
  }

  onSave(): void {
    this.submitted = true;

    if (!this.canGoToStep(4)) {
      return;
    }

    const request$ = this.isEditMode
      ? this.taskService.updateTask(this.model)
      : this.taskService.createTask(this.model);

    request$.subscribe({
      next: () => {
        this.saved.emit();
        this.closeDialog();
      },
      error: (error) => {
        console.error('Task save failed:', error);
      }
    });
  }

  createEmptyModel(): ITaskCreate {
    return {
      title: '',
      description: TASK_MARKDOWN_TEMPLATE,
      starterCode: {
        language: 'cpp',
        code: CPP_STARTER_TEMPLATE
      },
      includeStarterCode: true,
      publicTests: [],
      hiddenTests: []
    };
  }
}
