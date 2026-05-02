import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, EventEmitter, inject, Input, OnInit, Output } from '@angular/core';
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
import { TaskService } from '../../services/task.service';
import { ITaskResponse } from '../../models/task/ITaskResponse';
import { TestVisibilityEnum } from '../../models/enum/TestVisibilityEnum';
import { ConfirmationService, MessageService } from 'primeng/api';
import { finalize } from 'rxjs';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

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
    SelectModule,
    ProgressSpinnerModule
],
  templateUrl: './task-view-dialog.html',
  styleUrl: './task-view-dialog.scss',
})
export class TaskViewDialog implements OnInit {
  @Input() taskId: number | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() cloneRequested = new EventEmitter<number>();
  @Output() changed = new EventEmitter<void>();

  activeTab: ActiveTab = 'preview';
  isClosing = false;
  modelReady = false;
  isActionRunning = false;
  task: ITaskResponse | null = null;


  private nextTestId = 1;
  private sanitizer = inject(DomSanitizer);
  private taskService = inject(TaskService);
  private confirmationService = inject(ConfirmationService);
  private messageService = inject(MessageService);
  private cdr = inject(ChangeDetectorRef);

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
    readOnly: true,
    domReadOnly: true
  };

  private updateEditorOptions(): void {
    this.editorOptions = {
      ...this.editorOptions,
      readOnly: true,
      domReadOnly: true
    };
  }

  onIncludeStarterCodeChange(_value: boolean): void {
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
    if (this.taskId == null) {
      this.closed.emit();
      return;
    }

    this.taskService.getTask(this.taskId).subscribe({
      next: task => {
        this.task = task;
        this.model = this.mapTaskResponseToViewModel(task);
        this.nextTestId = this.getNextTestId();
        this.updateEditorOptions();

        setTimeout(() => {
          this.modelReady = true;
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Greska',
          detail: 'Nije moguce dohvatiti zadatak'
        });
        this.closed.emit();
      }
    });
  }

  private mapTaskResponseToViewModel(task: ITaskResponse): TaskModel {
    let nextTestId = 1;

    return {
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
    this.isClosing = true;

    setTimeout(() => {
      this.closed.emit();
    }, 200);
  }

  onClone(): void {
    if (!this.task || this.isActionRunning) {
      return;
    }

    this.isClosing = true;

    setTimeout(() => {
      this.cloneRequested.emit(this.task!.id);
    }, 200);
  }

  onToggleEnabled(): void {
    if (!this.task || this.isActionRunning) {
      return;
    }

    const actionLabel = this.task.enabled ? 'deaktivirati' : 'aktivirati';
    const successLabel = this.task.enabled ? 'deaktiviran' : 'aktiviran';

    this.confirmationService.confirm({
      message: `Jesi siguran da zelis ${actionLabel} zadatak "${this.task.title}"?`,
      header: 'Potvrda',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.task.enabled ? 'Deaktiviraj' : 'Aktiviraj',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: this.task.enabled ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        if (!this.task) {
          return;
        }

        this.isActionRunning = true;

        const request$ = this.task.enabled
          ? this.taskService.disableTask(this.task.id)
          : this.taskService.enableTask(this.task.id);

        request$
          .pipe(finalize(() => {
            this.isActionRunning = false;
            this.cdr.detectChanges();
          }))
          .subscribe({
            next: () => {
              if (this.task) {
                this.task = { ...this.task, enabled: !this.task.enabled };
              }

              this.messageService.add({
                severity: 'success',
                summary: 'Uspjeh',
                detail: `Zadatak je ${successLabel}`
              });
              this.changed.emit();
            },
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Greska',
                detail: `Nije moguce ${actionLabel} zadatak`
              });
            }
          });
      }
    });
  }

  onToggleShared(): void {
    if (!this.task || this.isActionRunning) {
      return;
    }

    const actionLabel = this.task.shared ? 'prestati dijeliti' : 'podijeliti';
    const successLabel = this.task.shared ? 'privatan' : 'podijeljen';

    this.confirmationService.confirm({
      message: `Jesi siguran da zelis ${actionLabel} zadatak "${this.task.title}"?`,
      header: 'Potvrda',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.task.shared ? 'Sakrij' : 'Podijeli',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: this.task.shared ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        if (!this.task) {
          return;
        }

        this.isActionRunning = true;

        const request$ = this.task.shared
          ? this.taskService.stopShareTask(this.task.id)
          : this.taskService.shareTask(this.task.id);

        request$
          .pipe(finalize(() => {
            this.isActionRunning = false;
            this.cdr.detectChanges();
          }))
          .subscribe({
            next: () => {
              if (this.task) {
                this.task = { ...this.task, shared: !this.task.shared };
              }

              this.messageService.add({
                severity: 'success',
                summary: 'Uspjeh',
                detail: `Zadatak je ${successLabel}`
              });
              this.changed.emit();
            },
            error: () => {
              this.messageService.add({
                severity: 'error',
                summary: 'Greska',
                detail: `Nije moguce ${actionLabel} zadatak`
              });
            }
          });
      }
    });
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
