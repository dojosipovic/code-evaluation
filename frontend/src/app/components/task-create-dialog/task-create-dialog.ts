import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';
import { ButtonModule } from 'primeng/button';
import { TabsModule } from 'primeng/tabs';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';

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
  statementMd: string;
  starterCode: StarterCode;
  includeStarterCode: boolean;
  publicTests: TestCase[];
  privateTests: TestCase[];
}

type ActiveTab = 'preview' | 'public' | 'private';

@Component({
  selector: 'app-task-create-dialog',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    TabsModule,
    MonacoEditorModule
  ],
  templateUrl: './task-create-dialog.html',
  styleUrl: './task-create-dialog.scss',
})
export class TaskCreateDialog {
  @Output() closed = new EventEmitter<void>();

  activeTab: ActiveTab = 'preview';
  isClosing = false;


  private nextTestId = 1;
  private sanitizer = inject(DomSanitizer);

  editorOptions = {
    theme: 'vs-dark',
    language: 'cpp',
    automaticLayout: true,
    minimap: { enabled: false }
  };

  model: TaskModel = {
    title: '',
    statementMd: `# Naziv zadatka

      ## Opis
      Ovdje napiši opis zadatka.

      ## Ulaz
      Opiši ulaz.

      ## Izlaz
      Opiši izlaz.

      ## Ograničenja
      - 1 <= n <= 1000

      ## Primjer
      **Ulaz**
      \`\`\`
      5
      \`\`\`

      **Izlaz**
      \`\`\`
      10
      \`\`\`
      `,
    starterCode: {
      language: 'cpp',
      code: `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    // input
    


    // output
    


    return 0;
}`
    },
    includeStarterCode: true, // ✅ default ON
    publicTests: [],
    privateTests: []
  };

  monacoOptions = {
    theme: 'vs-dark',
    language: 'cpp',
    automaticLayout: true,
    minimap: { enabled: false },
    fontSize: 14
  };

  constructor() {
    this.nextTestId = 2;
    marked.setOptions({
      breaks: true,
      gfm: true
    });
  }

  get renderedMarkdown(): SafeHtml {
    const rawHtml = marked.parse(this.model.statementMd) as string;
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
    this.model.privateTests.push({
      id: this.nextTestId++,
      input: '',
      output: ''
    });
  }

  removePublicTest(id: number): void {
    this.model.publicTests = this.model.publicTests.filter(test => test.id !== id);
  }

  removePrivateTest(id: number): void {
    this.model.privateTests = this.model.privateTests.filter(test => test.id !== id);
  }

  trackByTestId(_index: number, test: TestCase): number {
    return test.id;
  }

  onTabChange(value: string | number | undefined): void {
    if (value === 'preview' || value === 'public' || value === 'private') {
      this.activeTab = value;
    }
  }

  onLanguageChange(lang: string) {
    this.editorOptions = {
      ...this.editorOptions,
      language: lang
    };
  }
}
