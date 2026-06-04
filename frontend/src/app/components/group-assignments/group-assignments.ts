import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { PanelModule } from 'primeng/panel';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { GroupService } from '../../services/group.service';
import { IAssignmentListItem } from '../../models/assignment/IAssignmentListItem';
import { IUserResponse } from '../../models/user/IUserResponse';
import { ITaskResponse } from '../../models/task/ITaskResponse';
import { TaskCreateDialog } from '../task-create-dialog/task-create-dialog';
import { TaskViewDialog } from '../task-view-dialog/task-view-dialog';

@Component({
  selector: 'app-group-assignments',
  imports: [
    CommonModule,
    ButtonModule,
    ConfirmDialogModule,
    PaginatorModule,
    PanelModule,
    SkeletonModule,
    TagModule,
    TaskCreateDialog,
    TaskViewDialog
  ],
  providers: [ConfirmationService],
  templateUrl: './group-assignments.html',
  styleUrl: './group-assignments.scss',
})
export class GroupAssignments implements OnChanges {
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  @Input({ required: true }) groupId!: number;

  readonly loading = signal(false);

  assignments: IAssignmentListItem[] = [];
  skeletonRows = Array.from({ length: 3 });
  totalRecords = 0;
  rows = 10;
  first = 0;
  viewerTaskId: number | null = null;
  editorTaskId: number | null = null;
  editorOpen = false;
  editorCloneMode = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['groupId']) {
      this.first = 0;
      this.loadAssignments();
    }
  }

  onPageChange(event: PaginatorState): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadAssignments();
  }

  openTask(task: ITaskResponse, event?: Event): void {
    event?.stopPropagation();
    this.viewerTaskId = task.id;
  }

  onTaskViewerClosed(): void {
    this.viewerTaskId = null;
  }

  onTaskViewerCloneRequested(taskId: number): void {
    this.viewerTaskId = null;
    this.editorTaskId = taskId;
    this.editorCloneMode = true;
    this.editorOpen = true;
  }

  onTaskEditorClosed(): void {
    this.editorOpen = false;
    this.editorTaskId = null;
    this.editorCloneMode = false;
  }

  onTaskEditorSaved(): void {
    this.onTaskEditorClosed();
  }

  getCreatorName(user: IUserResponse | null | undefined): string {
    if (!user) {
      return '-';
    }

    const fullName = `${user.firstname ?? ''} ${user.lastname ?? ''}`.trim();

    return fullName || user.username || user.email || '-';
  }

  private loadAssignments(): void {
    if (!Number.isFinite(this.groupId)) {
      return;
    }

    this.loading.set(true);

    this.groupService.getAssignments(this.groupId, {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      sortBy: 'id',
      sortDirection: 'desc'
    })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: response => {
          this.assignments = response.items;
          this.totalRecords = response.totalItems;
        },
        error: () => {
          this.assignments = [];
          this.totalRecords = 0;
          this.messageService.add({
            severity: 'warn',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti zadatke grupe'
          });
        }
      });
  }
}
