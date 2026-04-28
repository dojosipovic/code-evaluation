import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Output } from '@angular/core';
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
    MessageModule
],
  templateUrl: './task-create-dialog.html',
  styleUrl: './task-create-dialog.scss',
})
export class TaskCreateDialog {
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

  languageOptions = [
    { label: 'C++', value: 'cpp' }
  ];

  model: ITaskCreate = {
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

  get renderedMarkdown(): SafeHtml {
    const rawHtml = marked.parse(this.model.description) as string;
    return this.sanitizer.bypassSecurityTrustHtml(rawHtml);
  }

  onClose(): void {
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

    if (!this.arePrivateTestsValid) {
      return;
    }

    console.log('Task model:', this.model);
  }
}
