import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, finalize, Subject, throttleTime } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { DrawerModule } from 'primeng/drawer';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { ProgressBarModule } from 'primeng/progressbar';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { MessageService } from 'primeng/api';
import { SortDirection } from '../../config/app-types';
import { SubmissionStatusEnum } from '../../models/enum/SubmissionStatusEnum';
import { ISubmissionListItem } from '../../models/submission/ISubmissionListItem';
import { IUserResponse } from '../../models/user/IUserResponse';
import { SubmissionService } from '../../services/submission.service';
import { SubmissionView } from '../submission-view/submission-view';

@Component({
  selector: 'app-submissions',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    ButtonModule,
    CardModule,
    DatePickerModule,
    DialogModule,
    DrawerModule,
    InputNumberModule,
    InputTextModule,
    PaginatorModule,
    ProgressBarModule,
    SelectModule,
    SkeletonModule,
    TagModule,
    SubmissionView
  ],
  templateUrl: './submissions.html',
  styleUrl: './submissions.scss'
})
export class Submissions implements OnInit {
  private submissionService = inject(SubmissionService);
  private messageService = inject(MessageService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private searchInput$ = new Subject<string>();
  private applyFilters$ = new Subject<void>();
  private lastAppliedSearch = '';
  private applyingQueryParams = false;

  readonly loading = signal(false);

  submissions: ISubmissionListItem[] = [];
  skeletonRows = Array.from({ length: 5 });
  totalRecords = 0;
  rows = 10;
  first = 0;
  sortField = 'submittedAt';
  sortDirection: SortDirection = 'desc';
  filtersDrawerVisible = false;
  submissionDialogOpen = false;
  viewedSubmissionId: number | null = null;

  submissionFilters = {
    search: '',
    assignmentId: null as number | null,
    userId: null as number | null,
    status: null as SubmissionStatusEnum | null,
    submittedAfter: null as Date | null,
    submittedBefore: null as Date | null
  };

  statusOptions = [
    { label: 'Svi statusi', value: null },
    { label: SubmissionStatusEnum.SUBMITTED, value: SubmissionStatusEnum.SUBMITTED },
    { label: SubmissionStatusEnum.QUEUED, value: SubmissionStatusEnum.QUEUED },
    { label: SubmissionStatusEnum.TESTED, value: SubmissionStatusEnum.TESTED },
    { label: SubmissionStatusEnum.FAILED, value: SubmissionStatusEnum.FAILED },
    { label: SubmissionStatusEnum.PLAGIARISM_ANALYZED, value: SubmissionStatusEnum.PLAGIARISM_ANALYZED }
  ];

  sortOptions = [
    { label: 'Predano', value: 'submittedAt' },
    { label: 'Status', value: 'status' },
    { label: 'Bodovi', value: 'finalScore' },
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

        this.submissionFilters.search = value;
        this.lastAppliedSearch = value;
        this.reloadSubmissionsFromFirstPage();
      });

    this.applyFilters$
      .pipe(
        throttleTime(1000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.lastAppliedSearch = this.submissionFilters.search;
        this.reloadSubmissionsFromFirstPage();
      });

    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.applyQueryParams(params);
        this.loadSubmissions();
      });
  }

  onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  onApplyFilters(): void {
    this.applyFilters$.next();
  }

  onApplyFiltersAndCloseDrawer(): void {
    this.filtersDrawerVisible = false;
    this.onApplyFilters();
  }

  onPageChange(event: PaginatorState): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;
    this.reloadSubmissions();
  }

  resetFilters(): void {
    this.submissionFilters = {
      search: '',
      assignmentId: null,
      userId: null,
      status: null,
      submittedAfter: null,
      submittedBefore: null
    };
    this.sortField = 'submittedAt';
    this.sortDirection = 'desc';
    this.first = 0;
    this.lastAppliedSearch = '';
    this.reloadSubmissions();
  }

  openSubmissionDialog(submissionId: number, event?: Event): void {
    event?.stopPropagation();
    this.viewedSubmissionId = submissionId;
    this.submissionDialogOpen = true;
  }

  onSubmissionDialogVisibleChange(visible: boolean): void {
    this.submissionDialogOpen = visible;

    if (!visible) {
      this.viewedSubmissionId = null;
    }
  }

  getUserDisplayName(user: IUserResponse | null | undefined): string {
    if (!user) {
      return '-';
    }

    const fullName = `${user.firstname ?? ''} ${user.lastname ?? ''}`.trim();

    return fullName || user.username || user.email || '-';
  }

  getSubmissionTestsLabel(submission: ISubmissionListItem): string {
    return `${submission.passedTests ?? 0}/${submission.totalTests ?? 0}`;
  }

  getSubmissionTestPercent(submission: ISubmissionListItem): number {
    if (!submission.totalTests) {
      return 0;
    }

    return Math.round((submission.passedTests / submission.totalTests) * 100);
  }

  getSubmissionScore(submission: ISubmissionListItem): string {
    return submission.finalScore !== null && submission.finalScore !== undefined
      ? String(submission.finalScore)
      : '-';
  }

  getStatusSeverity(status: SubmissionStatusEnum): 'success' | 'warn' | 'danger' | 'info' | 'secondary' {
    switch (status) {
      case SubmissionStatusEnum.TESTED:
      case SubmissionStatusEnum.PLAGIARISM_ANALYZED:
        return 'success';
      case SubmissionStatusEnum.QUEUED:
      case SubmissionStatusEnum.SUBMITTED:
        return 'warn';
      case SubmissionStatusEnum.FAILED:
        return 'danger';
      default:
        return 'info';
    }
  }

  trackBySubmission(_: number, submission: ISubmissionListItem): number {
    return submission.id;
  }

  private loadSubmissions(): void {
    this.loading.set(true);

    this.submissionService.getSubmissions({
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      search: this.submissionFilters.search?.trim() || null,
      assignmentId: this.submissionFilters.assignmentId,
      userId: this.submissionFilters.userId,
      status: this.submissionFilters.status,
      submittedAfter: this.toApiDate(this.submissionFilters.submittedAfter),
      submittedBefore: this.toApiDate(this.submissionFilters.submittedBefore),
      sortBy: this.sortField,
      sortDir: this.sortDirection
    })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: response => {
          this.submissions = response.items;
          this.totalRecords = response.totalItems;
        },
        error: () => {
          this.submissions = [];
          this.totalRecords = 0;
          this.messageService.add({
            severity: 'warn',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti predaje'
          });
        }
      });
  }

  private reloadSubmissionsFromFirstPage(): void {
    this.first = 0;
    this.reloadSubmissions();
  }

  private reloadSubmissions(): void {
    this.updateQueryParams();
  }

  private applyQueryParams(params: ParamMap): void {
    this.applyingQueryParams = true;

    const page = this.parseNonNegativeInteger(params.get('page'), 0);
    this.rows = this.parsePositiveInteger(params.get('size'), 10);
    this.first = page * this.rows;
    this.submissionFilters = {
      search: params.get('search') ?? '',
      assignmentId: this.parseNullableInteger(params.get('assignmentId')),
      userId: this.parseNullableInteger(params.get('userId')),
      status: this.parseStatus(params.get('status')),
      submittedAfter: this.parseDateParam(params.get('submittedAfter')),
      submittedBefore: this.parseDateParam(params.get('submittedBefore'))
    };
    this.lastAppliedSearch = this.submissionFilters.search;
    this.sortField = this.parseSortField(params.get('sortBy'));
    this.sortDirection = this.parseSortDirection(params.get('sortDir'));

    this.applyingQueryParams = false;
  }

  private updateQueryParams(): void {
    if (this.applyingQueryParams) {
      return;
    }

    const queryParams = {
      page: this.first > 0 ? Math.floor(this.first / this.rows) : null,
      size: this.rows !== 10 ? this.rows : null,
      search: this.submissionFilters.search?.trim() || null,
      assignmentId: this.submissionFilters.assignmentId,
      userId: this.submissionFilters.userId,
      status: this.submissionFilters.status,
      submittedAfter: this.toApiDate(this.submissionFilters.submittedAfter),
      submittedBefore: this.toApiDate(this.submissionFilters.submittedBefore),
      sortBy: this.sortField !== 'submittedAt' ? this.sortField : null,
      sortDir: this.sortDirection !== 'desc' ? this.sortDirection : null
    };

    if (this.queryParamsMatchSnapshot(queryParams)) {
      this.loadSubmissions();
      return;
    }

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
      replaceUrl: false
    });
  }

  private queryParamsMatchSnapshot(queryParams: Record<string, string | number | boolean | null>): boolean {
    return Object.entries(queryParams).every(([key, value]) => {
      const currentValue = this.route.snapshot.queryParamMap.get(key);

      if (value === null || value === undefined || value === '') {
        return currentValue === null;
      }

      return currentValue === String(value);
    });
  }

  private parseStatus(value: string | null): SubmissionStatusEnum | null {
    return Object.values(SubmissionStatusEnum).includes(value as SubmissionStatusEnum)
      ? value as SubmissionStatusEnum
      : null;
  }

  private parseSortField(value: string | null): string {
    return this.sortOptions.some(option => option.value === value) ? value as string : 'submittedAt';
  }

  private parseSortDirection(value: string | null): SortDirection {
    return value === 'asc' || value === 'desc' ? value : 'desc';
  }

  private parseNullableInteger(value: string | null): number | null {
    const parsed = Number(value);

    return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
  }

  private parsePositiveInteger(value: string | null, fallback: number): number {
    const parsed = Number(value);

    return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
  }

  private parseNonNegativeInteger(value: string | null, fallback: number): number {
    const parsed = Number(value);

    return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
  }

  private parseDateParam(value: string | null): Date | null {
    if (!value) {
      return null;
    }

    const date = new Date(value);

    return Number.isFinite(date.getTime()) ? date : null;
  }

  private toApiDate(value: Date | null): string | null {
    return value ? value.toISOString() : null;
  }
}
