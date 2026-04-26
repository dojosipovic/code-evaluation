import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { MenuModule } from 'primeng/menu';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { TaskService } from '../../services/task.service';
import { Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, finalize, Subject, throttleTime } from 'rxjs';
import { ITaskListItem } from '../../models/task/ITaskListItem';
import { TaskStatusEnum } from '../../models/enum/TaskStatusEnum';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ITaskQueryParams } from '../../models/task/ITaskQueryParams';
import { TaskCreateDialog } from '../../components/task-create-dialog/task-create-dialog';
import { PopoverModule, Popover } from 'primeng/popover';
import { AuthService } from '../../services/auth/auth.service';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-tasks',
  imports: [
    CommonModule,
    FormsModule,
    TableModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    TagModule,
    DividerModule,
    ConfirmDialogModule,
    MenuModule,
    CheckboxModule,
    TaskCreateDialog,
    PopoverModule,
    SkeletonModule
  ],
  providers: [ConfirmationService],
  templateUrl: './tasks.html',
  styleUrl: './tasks.scss',
})
export class Tasks implements OnInit {

  private taskService = inject(TaskService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private authService = inject(AuthService);

  private searchInput$ = new Subject<string>();
  private applyFilters$ = new Subject<void>();

  readonly loading = signal(false);
  readonly TaskStatusEnum = TaskStatusEnum;

  @ViewChild('taskActions') taskActions!: Popover;

  skeletonRows = Array.from({ length: 5 });

  editorOpen = false;

  tasks: ITaskListItem[] = [];
  selectedTask: ITaskListItem | null = null;
  taskMenuItems: MenuItem[] = [];
  totalRecords = 0;

  rows = 10;
  first = 0;

  taskFilters = {
    search: '',
    status: null as TaskStatusEnum | null,
    enabled: null as boolean | null,
    shared: null as boolean | null,
    excludeCurrentUser: null as boolean | null
  };

  taskSortField = 'id';
  taskSortOrder: 1 | -1 = -1;

  taskStatusOptions = [
    { label: 'Svi statusi', value: null },
    { label: TaskStatusEnum.DRAFT, value: TaskStatusEnum.DRAFT },
    { label: TaskStatusEnum.PUBLISHED, value: TaskStatusEnum.PUBLISHED }
  ];

  enabledOptions = [
    { label: 'Svi statusi', value: null },
    { label: 'Enabled', value: true },
    { label: 'Disabled', value: false }
  ];

  sharedOptions = [
    { label: 'Svi', value: null },
    { label: 'Shared', value: true },
    { label: 'Private', value: false }
  ];

  excludeCurrentUserOptions = [
    { label: 'Svi autori', value: null },
    { label: 'Bez mojih zadataka', value: true },
    { label: 'Samo moji zadaci', value: false }
  ];

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

    this.applyFilters$
      .pipe(
        throttleTime(1000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.first = 0;
        this.loadTasks();
      });
  }

  canEditTask(task: ITaskListItem): boolean {
    return this.authService.isAdmin() || this.isOwner(task);
  }

  private isOwner(task: ITaskListItem): boolean {
    return task.user.username == this.authService.username()
  }

  onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  onFiltersChange(): void {
    this.first = 0;
    this.loadTasks();
  }

  onApplyFilters(): void {
    this.applyFilters$.next();
  }

  resetFilters(): void {
    this.taskFilters = {
      search: '',
      status: null,
      enabled: null,
      shared: null,
      excludeCurrentUser: null
    };

    this.taskSortField = 'id';
    this.taskSortOrder = 1;
    this.first = 0;

    this.applyFilters$.next();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 20;

    if (typeof event.sortField === 'string' && event.sortField) {
      this.taskSortField = event.sortField;
    }

    if (event.sortOrder === 1 || event.sortOrder === -1) {
      this.taskSortOrder = event.sortOrder;
    }

    this.loadTasks();
  }

  openTaskActions(event: Event, task: ITaskListItem): void {

    if (this.selectedTask?.id === task.id) {
      this.taskActions.hide();
      this.selectedTask = null;
      return;
    }

    this.selectedTask = task;
    this.taskActions.show(event);

    if (this.taskActions.container) {
      this.taskActions.align();
    }
  }

  hideTaskActions(): void {
    this.taskActions.hide();
    this.selectedTask = null;
  }

  openTaskDetails(task: ITaskListItem): void {
    this.router.navigate(['/tasks', task.id]);
  }

  editTask(task: ITaskListItem): void {
    this.router.navigate(['/tasks', task.id, 'edit']);
  }

  onToggleTaskEnabled(task: ITaskListItem): void {
    const actionLabel = task.enabled ? 'deaktivirati' : 'aktivirati';
    const successLabel = task.enabled ? 'deaktiviran' : 'aktiviran';

    this.confirmationService.confirm({
      message: `Jesi siguran da želiš ${actionLabel} zadatak "${task.title}"?`,
      header: 'Potvrda',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: task.enabled ? 'Deaktiviraj' : 'Aktiviraj',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: task.enabled ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        const request$ = task.enabled
          ? this.taskService.disableTask(task.id)
          : this.taskService.enableTask(task.id);

        request$.subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Uspjeh',
              detail: `Zadatak je ${successLabel}`
            });

            this.loadTasks();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Greška',
              detail: `Nije moguće ${actionLabel} zadatak`
            });
          }
        });
      }
    });
  }

  onToggleTaskShared(task: ITaskListItem): void {
    const actionLabel = task.shared ? 'prestati dijelii' : 'podijeliti';
    const successLabel = task.shared ? 'privatan' : 'podijeljen';

    this.confirmationService.confirm({
      message: `Jesi siguran da želiš ${actionLabel} zadatak "${task.title}"?`,
      header: 'Potvrda',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: task.shared ? 'Sakrij' : 'Podijeli',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: task.shared ? 'p-button-danger' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        const request$ = task.shared
          ? this.taskService.stopShareTask(task.id)
          : this.taskService.shareTask(task.id);

        request$.subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Uspjeh',
              detail: `Zadatak je ${successLabel}`
            });

            this.loadTasks();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Greška',
              detail: `Nije moguće ${actionLabel} zadatak`
            });
          }
        });
      }
    });
  }

  onToggleTaskStatus(task: ITaskListItem): void {
    const isPublished = task.status === TaskStatusEnum.PUBLISHED;
    const actionLabel = isPublished ? 'vratiti u draft' : 'objaviti';
    const successLabel = isPublished ? 'vraćen u draft' : 'objavljen';

    this.confirmationService.confirm({
      message: `Jesi siguran da želiš ${actionLabel} zadatak "${task.title}"?`,
      header: 'Potvrda',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: isPublished ? 'Vrati u draft' : 'Objavi',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: isPublished ? 'p-button-warning' : 'p-button-success',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        const request$ = isPublished
          ? this.taskService.publishTask(task.id)
          : this.taskService.publishTask(task.id);

        request$.subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Uspjeh',
              detail: `Zadatak je ${successLabel}`
            });

            this.loadTasks();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Greška',
              detail: `Nije moguće ${actionLabel} zadatak`
            });
          }
        });
      }
    });
  }

  onDeleteTask(task: ITaskListItem): void {
    this.confirmationService.confirm({
      message: `Jesi siguran da želiš obrisati zadatak "${task.title}"?`,
      header: 'Brisanje zadatka',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Obriši',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        this.taskService.deleteTask(task.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Uspjeh',
              detail: 'Zadatak je obrisan'
            });

            this.loadTasks();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Greška',
              detail: 'Nije moguće obrisati zadatak'
            });
          }
        });
      }
    });
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

  private buildTaskParams(): ITaskQueryParams {
    return {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      search: this.taskFilters.search?.trim() || null,
      status: this.taskFilters.status,
      enabled: this.taskFilters.enabled,
      shared: this.taskFilters.shared,
      excludeCurrentUser: this.taskFilters.excludeCurrentUser,
      sortBy: this.taskSortField,
      sortDir: this.taskSortOrder === 1 ? 'asc' : 'desc'
    };
  }

  private loadTasks(): void {
    this.loading.set(true);

    this.taskService.getTasks(this.buildTaskParams())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.tasks = response.items;
          this.totalRecords = response.totalItems;
        },
        error: () => {
          this.messageService.add({
            severity: 'warn',
            summary: 'Greška',
            detail: 'Nije moguće dohvatiti zadatke'
          });

          this.tasks = [];
          this.totalRecords = 0;
        }
      });
  }
}
