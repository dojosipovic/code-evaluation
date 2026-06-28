import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  SimpleChanges,
  inject,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, finalize, Subject, throttleTime } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { PanelModule } from 'primeng/panel';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { SortDirection } from '../../config/app-types';
import { GroupService } from '../../services/group.service';
import { IAssignmentListItem } from '../../models/assignment/IAssignmentListItem';
import { IUserResponse } from '../../models/user/IUserResponse';
import { ITaskResponse } from '../../models/task/ITaskResponse';
import { AssignmentCreateDialog } from '../assignment-create-dialog/assignment-create-dialog';
import { TaskCreateDialog } from '../task-create-dialog/task-create-dialog';
import { TaskViewDialog } from '../task-view-dialog/task-view-dialog';

@Component({
  selector: 'app-group-assignments',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    ButtonModule,
    ConfirmDialogModule,
    InputTextModule,
    PaginatorModule,
    PanelModule,
    SelectModule,
    SkeletonModule,
    TagModule,
    TooltipModule,
    AssignmentCreateDialog,
    TaskCreateDialog,
    TaskViewDialog
  ],
  providers: [ConfirmationService],
  templateUrl: './group-assignments.html',
  styleUrl: './group-assignments.scss',
})
export class GroupAssignments implements OnInit, OnChanges, OnDestroy {
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);
  private searchInput$ = new Subject<string>();
  private applyFilters$ = new Subject<void>();
  private lastAppliedSearch = '';

  @Input({ required: true }) groupId!: number;
  @Input() canManage = false;

  readonly loading = signal(false);
  readonly currentTime = signal(Date.now());

  assignments: IAssignmentListItem[] = [];
  skeletonRows = Array.from({ length: 3 });
  totalRecords = 0;
  rows = 10;
  first = 0;
  sortField = 'endsAt';
  sortDirection: SortDirection = 'desc';
  viewerTaskId: number | null = null;
  editorTaskId: number | null = null;
  editorOpen = false;
  editorCloneMode = false;
  assignmentCreatorOpen = false;
  private countdownTimerId: ReturnType<typeof setTimeout> | null = null;

  assignmentFilters = {
    search: ''
  };

  sortOptions = [
    { label: 'Pocetak', value: 'startsAt' },
    { label: 'Kraj', value: 'endsAt' },
    { label: 'Naziv', value: 'name' },
    { label: 'Bodovi', value: 'points' },
    { label: 'Naslov zadatka', value: 'taskTitle' },
    { label: 'Autor', value: 'createdBy' },
    { label: 'ID', value: 'id' }
  ];

  sortDirectionOptions = [
    { label: 'Uzlazno', value: 'asc' },
    { label: 'Silazno', value: 'desc' }
  ];

  ngOnInit(): void {
    this.searchInput$
      .pipe(
        debounceTime(800),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(value => {
        if (value.trim() === this.lastAppliedSearch.trim()) {
          return;
        }

        this.assignmentFilters.search = value;
        this.lastAppliedSearch = value;
        this.first = 0;
        this.loadAssignments();
      });

    this.applyFilters$
      .pipe(
        throttleTime(2000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.lastAppliedSearch = this.assignmentFilters.search;
        this.first = 0;
        this.loadAssignments();
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['groupId']) {
      this.first = 0;
      this.loadAssignments();
    }
  }

  ngOnDestroy(): void {
    this.clearCountdownTimer();
  }

  onPageChange(event: PaginatorState): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.loadAssignments();
  }

  onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  onApplyFilters(): void {
    this.applyFilters$.next();
  }

  onSortChange(): void {
    this.first = 0;
    this.loadAssignments();
  }

  resetFilters(): void {
    this.assignmentFilters = {
      search: ''
    };
    this.sortField = 'endsAt';
    this.sortDirection = 'desc';
    this.first = 0;
    this.applyFilters$.next();
  }

  openAssignmentCreator(): void {
    if (!this.canManage) {
      return;
    }

    this.assignmentCreatorOpen = true;
  }

  onAssignmentCreatorVisibleChange(visible: boolean): void {
    this.assignmentCreatorOpen = visible;
  }

  onAssignmentCreated(): void {
    this.assignmentCreatorOpen = false;
    this.first = 0;
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

  getStartTimeLabel(assignment: IAssignmentListItem): string {
    const startsAt = new Date(assignment.startsAt).getTime();
    const endsAt = new Date(assignment.endsAt).getTime();
    const now = this.currentTime();

    if (!Number.isFinite(startsAt)) {
      return '-';
    }

    if (Number.isFinite(endsAt) && now > endsAt) {
      return 'Zavrseno';
    }

    if (now >= startsAt) {
      return Number.isFinite(endsAt)
        ? `U tijeku (${this.formatCompactDuration(Math.max(endsAt - now, 0), 1, true)})`
        : 'U tijeku';
    }

    return this.formatCompactDuration(startsAt - now, 1, true);
  }

  getStartTimeTitle(assignment: IAssignmentListItem): string {
    const startsAt = new Date(assignment.startsAt).getTime();
    const endsAt = new Date(assignment.endsAt).getTime();
    const now = this.currentTime();

    if (!Number.isFinite(startsAt)) {
      return 'Vrijeme otvaranja nije dostupno';
    }

    if (Number.isFinite(endsAt) && now > endsAt) {
      return `Zadatak je zavrsio prije ${this.formatCompactDuration(now - endsAt, 1, true)}`;
    }

    if (now >= startsAt) {
      return Number.isFinite(endsAt)
        ? `Zadatak je u tijeku jos ${this.formatCompactDuration(endsAt - now, 1, true)}`
        : 'Zadatak je u tijeku';
    }

    return `Otvara se za ${this.formatCompactDuration(startsAt - now, 1, true)}`;
  }

  getStartTimeClass(assignment: IAssignmentListItem): string {
    const startsAt = new Date(assignment.startsAt).getTime();
    const endsAt = new Date(assignment.endsAt).getTime();
    const now = this.currentTime();

    if (!Number.isFinite(startsAt)) {
      return 'time-neutral';
    }

    if (Number.isFinite(endsAt) && now > endsAt) {
      return 'time-neutral';
    }

    if (now >= startsAt) {
      return 'time-active';
    }

    const distanceFromNow = startsAt - now;

    return distanceFromNow <= 24 * 60 * 60 * 1000 ? 'time-close' : 'time-far';
  }

  getAssignmentPanelClass(assignment: IAssignmentListItem): string {
    return this.isAssignmentActive(assignment)
      ? 'assignment-panel assignment-panel-active'
      : 'assignment-panel';
  }

  getDurationTooltip(assignment: IAssignmentListItem): string {
    const duration = this.getDurationLabel(assignment);

    return duration === '-' ? 'Trajanje nije dostupno' : `Trajanje: ${duration}`;
  }

  getDurationLabel(assignment: IAssignmentListItem): string {
    const startsAt = new Date(assignment.startsAt).getTime();
    const endsAt = new Date(assignment.endsAt).getTime();

    if (!Number.isFinite(startsAt) || !Number.isFinite(endsAt) || endsAt < startsAt) {
      return '-';
    }

    return this.formatCompactDuration(endsAt - startsAt, 3);
  }

  private formatCompactDuration(milliseconds: number, maxParts = 1, roundUpSingleUnit = false): string {
    const totalSeconds = Math.max(0, Math.floor(milliseconds / 1000));
    const units = [
      { label: 'w', seconds: 7 * 24 * 60 * 60 },
      { label: 'd', seconds: 24 * 60 * 60 },
      { label: 'h', seconds: 60 * 60 },
      { label: 'min', seconds: 60 },
      { label: 's', seconds: 1 }
    ];

    if (totalSeconds < 1) {
      return '0s';
    }

    if (roundUpSingleUnit && maxParts === 1) {
      const unit = units.find(item => totalSeconds >= item.seconds) ?? units[units.length - 1];
      const value = Math.max(1, Math.ceil(totalSeconds / unit.seconds));

      return `${value}${unit.label}`;
    }

    let remaining = totalSeconds;
    const parts: string[] = [];

    for (const unit of units) {
      const value = Math.floor(remaining / unit.seconds);

      if (value <= 0) {
        continue;
      }

      parts.push(`${value}${unit.label}`);
      remaining -= value * unit.seconds;

      if (parts.length === maxParts) {
        break;
      }
    }

    return parts.join(' ');
  }

  isAssignmentActive(assignment: IAssignmentListItem): boolean {
    const startsAt = new Date(assignment.startsAt).getTime();
    const endsAt = new Date(assignment.endsAt).getTime();
    const now = this.currentTime();

    return Number.isFinite(startsAt) && Number.isFinite(endsAt) && now >= startsAt && now <= endsAt;
  }

  private loadAssignments(): void {
    if (!Number.isFinite(this.groupId)) {
      return;
    }

    this.loading.set(true);

    this.groupService.getAssignments(this.groupId, {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      search: this.assignmentFilters.search?.trim() || null,
      sortBy: this.sortField,
      sortDirection: this.sortDirection
    })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: response => {
          this.assignments = response.items;
          this.totalRecords = response.totalItems;
          this.currentTime.set(Date.now());
          this.scheduleCountdownRefresh();
        },
        error: () => {
          this.assignments = [];
          this.totalRecords = 0;
          this.clearCountdownTimer();
          this.messageService.add({
            severity: 'warn',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti zadatke grupe'
          });
        }
      });
  }

  private scheduleCountdownRefresh(): void {
    this.clearCountdownTimer();

    if (!this.assignments.length) {
      return;
    }

    const delay = this.getNextCountdownDelay();

    this.countdownTimerId = setTimeout(() => {
      this.currentTime.set(Date.now());
      this.scheduleCountdownRefresh();
    }, delay);
  }

  private clearCountdownTimer(): void {
    if (this.countdownTimerId === null) {
      return;
    }

    clearTimeout(this.countdownTimerId);
    this.countdownTimerId = null;
  }

  private getNextCountdownDelay(): number {
    const now = this.currentTime();
    const distances = this.assignments
      .flatMap(assignment => [
        new Date(assignment.startsAt).getTime(),
        new Date(assignment.endsAt).getTime()
      ])
      .filter(timestamp => Number.isFinite(timestamp) && timestamp > now)
      .map(timestamp => timestamp - now);

    if (!distances.length) {
      return 60 * 1000;
    }

    if (distances.some(distance => distance < 60 * 1000)) {
      return 1000;
    }

    const nextMinuteBoundary = Math.min(
      ...distances.map(distance => {
        const remainder = distance % (60 * 1000);
        return remainder === 0 ? 60 * 1000 : remainder;
      })
    );

    return Math.max(1000, Math.min(nextMinuteBoundary, 60 * 1000));
  }
}
