import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, finalize, Subject, throttleTime } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { GroupService } from '../../services/group.service';
import { IUserResponse } from '../../models/user/IUserResponse';
import { SortDirection } from '../../config/app-types';

interface GroupMemberAddDialogData {
  groupId: number;
  onMemberAdded?: () => void;
}

@Component({
  selector: 'app-group-member-add-dialog',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    InputTextModule,
    SkeletonModule,
    TableModule,
    TagModule
  ],
  templateUrl: './group-member-add-dialog.html',
  styleUrl: './group-member-add-dialog.scss',
})
export class GroupMemberAddDialog implements OnInit {
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);
  private dialogConfig = inject(DynamicDialogConfig<GroupMemberAddDialogData>);
  private destroyRef = inject(DestroyRef);

  private searchInput$ = new Subject<string>();
  private applySearch$ = new Subject<void>();

  readonly loading = signal(false);

  users: IUserResponse[] = [];
  addingUserIds = new Set<number>();
  skeletonRows = Array.from({ length: 5 });
  totalRecords = 0;

  rows = 10;
  first = 0;
  sortField = 'id';
  sortDirection: SortDirection = 'asc';

  userFilters = {
    search: ''
  };

  get groupId(): number {
    return this.dialogConfig.data?.groupId ?? NaN;
  }

  ngOnInit(): void {
    this.searchInput$
      .pipe(
        debounceTime(1500),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(value => {
        this.userFilters.search = value;
        this.first = 0;
        this.loadUsers();
      });

    this.applySearch$
      .pipe(
        throttleTime(2000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.first = 0;
        this.loadUsers();
      });
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;

    if (event.sortField === 'id') {
      this.sortField = 'id';
    }

    if (event.sortOrder === 1 || event.sortOrder === -1) {
      this.sortDirection = event.sortOrder === 1 ? 'asc' : 'desc';
    }

    this.loadUsers();
  }

  onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  onApplySearch(): void {
    this.applySearch$.next();
  }

  resetSearch(): void {
    this.userFilters = {
      search: ''
    };
    this.first = 0;
    this.applySearch$.next();
  }

  addMember(user: IUserResponse): void {
    if (this.addingUserIds.has(user.id)) {
      return;
    }

    this.addingUserIds.add(user.id);

    this.groupService.addMember(this.groupId, user.id)
      .pipe(finalize(() => this.addingUserIds.delete(user.id)))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Uspjeh',
            detail: 'Clan je dodan u grupu'
          });

          this.dialogConfig.data?.onMemberAdded?.();
          this.loadUsers();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce dodati clana u grupu'
          });
        }
      });
  }

  getFullName(user: IUserResponse): string {
    const fullName = `${user.firstname ?? ''} ${user.lastname ?? ''}`.trim();

    return fullName || '-';
  }

  isAdding(user: IUserResponse): boolean {
    return this.addingUserIds.has(user.id);
  }

  private loadUsers(): void {
    if (!Number.isFinite(this.groupId)) {
      return;
    }

    this.loading.set(true);

    this.groupService.getNonMembers(this.groupId, {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      search: this.userFilters.search?.trim() || null,
      sortBy: this.sortField,
      sortDirection: this.sortDirection
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.users = response.items;
          this.totalRecords = response.totalItems;
        },
        error: () => {
          this.users = [];
          this.totalRecords = 0;
          this.messageService.add({
            severity: 'warn',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti korisnike'
          });
        }
      });
  }
}
