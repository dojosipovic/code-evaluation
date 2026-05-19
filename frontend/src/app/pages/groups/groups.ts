import { CommonModule, DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, finalize, Subject, throttleTime } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DataViewModule } from 'primeng/dataview';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';
import { Router } from '@angular/router';
import { GroupService } from '../../services/group.service';
import { AuthService } from '../../services/auth/auth.service';
import { GroupCreateUpdateDialog } from '../../components/group-create-update-dialog/group-create-update-dialog';
import { IGroupListItem } from '../../models/group/IGroupListItem';
import { IPagedQueryParams } from '../../models/IPagedQueryParams';

type DataViewLayout = 'list' | 'grid';
type SortOrder = 1 | -1;

interface GroupDataViewEvent {
  first?: number;
  rows?: number;
  sortField?: string | string[] | null;
  sortOrder?: number | null;
}

@Component({
  selector: 'app-groups',
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    DataViewModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    SelectButtonModule,
    SkeletonModule,
    GroupCreateUpdateDialog
  ],
  templateUrl: './groups.html',
  styleUrl: './groups.scss',
})
export class Groups implements OnInit {

  private groupService = inject(GroupService);
  private authService = inject(AuthService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  private searchInput$ = new Subject<string>();
  private applyFilters$ = new Subject<void>();

  readonly loading = signal(false);
  readonly canCreateGroup = computed(() => this.authService.isAdmin() || this.authService.isProf());

  groups: IGroupListItem[] = [];
  skeletonRows = Array.from({ length: 6 });
  totalRecords = 0;
  createGroupDialogVisible = false;

  rows = 10;
  first = 0;
  layout: DataViewLayout = 'grid';

  groupFilters = {
    search: ''
  };

  groupSortField = 'id';
  groupSortOrder: SortOrder = -1;
  sortKey = '!id';

  layoutOptions = [
    { label: 'List', icon: 'pi pi-bars', value: 'list' as DataViewLayout },
    { label: 'Grid', icon: 'pi pi-table', value: 'grid' as DataViewLayout }
  ];

  sortOptions = [
    { label: 'ID silazno', value: '!id' },
    { label: 'ID uzlazno', value: 'id' },
    { label: 'Naziv A-Z', value: 'name' },
    { label: 'Naziv Z-A', value: '!name' },
    { label: 'Najnovije', value: '!createdAt' },
    { label: 'Najstarije', value: 'createdAt' },
    { label: 'Najvise clanova', value: '!memberCount' },
    { label: 'Najmanje clanova', value: 'memberCount' }
  ];

  ngOnInit(): void {
    this.searchInput$
      .pipe(
        debounceTime(1500),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(value => {
        this.groupFilters.search = value;
        this.loadFromFirstPage();
      });

    this.applyFilters$
      .pipe(
        throttleTime(1000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.loadFromFirstPage();
      });
  }

  onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  onApplyFilters(): void {
    this.applyFilters$.next();
  }

  openCreateGroupDialog(): void {
    this.createGroupDialogVisible = true;
  }

  onGroupCreated(): void {
    this.loadFromFirstPage();
  }

  openGroup(group: IGroupListItem): void {
    this.router.navigate(['/groups', group.id, 'users']);
  }

  resetFilters(): void {
    this.groupFilters = {
      search: ''
    };

    this.groupSortField = 'id';
    this.groupSortOrder = -1;
    this.sortKey = '!id';

    this.applyFilters$.next();
  }

  onSortChange(value: string): void {
    this.sortKey = value;
    this.groupSortOrder = value.startsWith('!') ? -1 : 1;
    this.groupSortField = value.replace('!', '');
    this.loadFromFirstPage();
  }

  onLazyLoad(event: GroupDataViewEvent): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;

    this.loadGroups();
  }

  getOwnerName(group: IGroupListItem): string {
    const owner = group.owner;

    if (!owner) {
      return '-';
    }

    const fullName = `${owner.firstname ?? ''} ${owner.lastname ?? ''}`.trim();

    return fullName || owner.username || owner.email || '-';
  }

  private buildGroupParams(): IPagedQueryParams {
    return {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      search: this.groupFilters.search?.trim() || null,
      sortBy: this.groupSortField,
      sortDirection: this.groupSortOrder === 1 ? 'asc' : 'desc'
    };
  }

  private loadGroups(): void {
    this.loading.set(true);

    this.groupService.getGroups(this.buildGroupParams())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.groups = response.items;
          this.totalRecords = response.totalItems;
        },
        error: () => {
          this.messageService.add({
            severity: 'warn',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti grupe'
          });

          this.groups = [];
          this.totalRecords = 0;
        }
      });
  }

  private loadFromFirstPage(): void {
    if (this.first === 0) {
      this.loadGroups();
      return;
    }

    this.first = 0;
  }
}
