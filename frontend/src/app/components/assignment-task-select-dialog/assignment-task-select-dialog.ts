import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, finalize, Subject, throttleTime } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { TaskService } from '../../services/task.service';
import { ITaskListItem } from '../../models/task/ITaskListItem';
import { TaskStatusEnum } from '../../models/enum/TaskStatusEnum';
import { TaskViewDialog } from '../task-view-dialog/task-view-dialog';

interface AssignmentTaskSelectDialogData {
  selectedTaskId?: number | null;
  onTaskSelected?: (task: ITaskListItem) => void;
}

type TaskSource = 'mine' | 'shared';

@Component({
  selector: 'app-assignment-task-select-dialog',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    SelectButtonModule,
    SkeletonModule,
    TableModule,
    TagModule,
    TaskViewDialog
  ],
  templateUrl: './assignment-task-select-dialog.html',
  styleUrl: './assignment-task-select-dialog.scss',
})
export class AssignmentTaskSelectDialog implements OnInit {
  private taskService = inject(TaskService);
  private destroyRef = inject(DestroyRef);
  private dialogRef = inject(DynamicDialogRef);
  private dialogConfig = inject(DynamicDialogConfig<AssignmentTaskSelectDialogData>);

  private searchInput$ = new Subject<string>();
  private applySearch$ = new Subject<void>();

  readonly loading = signal(false);

  tasks: ITaskListItem[] = [];
  skeletonRows = Array.from({ length: 5 });
  totalRecords = 0;
  rows = 10;
  first = 0;
  sortField = 'id';
  sortOrder: 1 | -1 = -1;
  source: TaskSource = 'mine';
  viewerTaskId: number | null = null;

  taskFilters = {
    search: ''
  };

  sourceOptions = [
    { label: 'Moji zadaci', value: 'mine' },
    { label: 'Dijeljeno', value: 'shared' }
  ];

  get selectedTaskId(): number | null {
    return this.dialogConfig.data?.selectedTaskId ?? null;
  }

  ngOnInit(): void {
    this.searchInput$
      .pipe(
        debounceTime(800),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(value => {
        this.taskFilters.search = value;
        this.first = 0;
        this.loadTasks();
      });

    this.applySearch$
      .pipe(
        throttleTime(1000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.first = 0;
        this.loadTasks();
      });
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;

    if (typeof event.sortField === 'string' && event.sortField) {
      this.sortField = event.sortField;
    }

    if (event.sortOrder === 1 || event.sortOrder === -1) {
      this.sortOrder = event.sortOrder;
    }

    this.loadTasks();
  }

  onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  onApplySearch(): void {
    this.applySearch$.next();
  }

  onSourceChange(): void {
    this.first = 0;
    this.loadTasks();
  }

  resetSearch(): void {
    this.taskFilters = {
      search: ''
    };
    this.first = 0;
    this.applySearch$.next();
  }

  selectTask(task: ITaskListItem): void {
    this.dialogConfig.data?.onTaskSelected?.(task);
    this.dialogRef.close(task);
  }

  openTaskPreview(task: ITaskListItem): void {
    this.viewerTaskId = task.id;
  }

  onTaskViewerClosed(): void {
    this.viewerTaskId = null;
  }

  getStatusSeverity(status: TaskStatusEnum): 'success' | 'warn' | 'danger' | 'info' | 'secondary' {
    switch (status) {
      case TaskStatusEnum.PUBLISHED:
        return 'success';
      case TaskStatusEnum.DRAFT:
        return 'warn';
      default:
        return 'info';
    }
  }

  getAuthorName(task: ITaskListItem): string {
    const user = task.user;

    if (!user) {
      return '-';
    }

    const fullName = `${user.firstname ?? ''} ${user.lastname ?? ''}`.trim();

    return fullName || user.username || user.email || '-';
  }

  private loadTasks(): void {
    this.loading.set(true);

    this.taskService.getTasks({
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      search: this.taskFilters.search?.trim() || null,
      status: TaskStatusEnum.PUBLISHED,
      enabled: true,
      shared: this.source === 'shared' ? true : null,
      excludeCurrentUser: this.source === 'shared',
      sortBy: this.sortField,
      sortDir: this.sortOrder === 1 ? 'asc' : 'desc'
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.tasks = response.items;
          this.totalRecords = response.totalItems;
        },
        error: () => {
          this.tasks = [];
          this.totalRecords = 0;
        }
      });
  }
}
