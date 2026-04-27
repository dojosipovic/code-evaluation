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

interface TestCase {
  id: number;
  input: string;
  output: string;
}

interface StarterCode {
  language: string;
  code: string;
}

interface TaskModel {
  title: string;
  description: string;
  starterCode: StarterCode;
  includeStarterCode: boolean;
  publicTests: TestCase[];
  hiddenTests: TestCase[];
}

type ActiveTab = 'preview' | 'public' | 'private' | 'code';

@Component({
  selector: 'app-task-view-dialog',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TabsModule,
    MonacoEditorModule,
    InputText,
    ToggleSwitchModule,
    SelectModule
],
  templateUrl: './task-view-dialog.html',
  styleUrl: './task-view-dialog.scss',
})
export class TaskViewDialog {
  @Output() closed = new EventEmitter<void>();

  activeTab: ActiveTab = 'preview';
  isClosing = false;


  private nextTestId = 1;
  private sanitizer = inject(DomSanitizer);

  languageOptions = [
    { label: 'C++', value: 'cpp' }
  ];

  model: TaskModel = {
    title: '',
    description: TASK_MARKDOWN_TEMPLATE,
    starterCode: {
      language: 'cpp',
      code: CPP_STARTER_TEMPLATE
    },
    includeStarterCode: true, // ✅ default ON
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
    // this.closed.emit();
    this.isClosing = true;

    setTimeout(() => {
      this.closed.emit();
    }, 200);
  }

  onSave(): void {
    console.log('Task model:', this.model);
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

  trackByTestId(_index: number, test: TestCase): number {
    return test.id;
  }

  onTabChange(value: string | number | undefined): void {
    if (value === 'preview' || value === 'public' || value === 'private' || value === 'code') {
      this.activeTab = value;
    }
  }
}
