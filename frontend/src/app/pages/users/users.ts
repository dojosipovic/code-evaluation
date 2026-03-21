import { Component, inject, OnInit, signal } from '@angular/core';
import { InviteService } from '../../services/invite.service';
import { IInviteResponse } from '../../models/invite/IInviteResponse';
import { InviteStatusEnum } from '../../models/enum/InviteStatusEnum';
import { RoleEnum } from '../../models/enum/RoleEnum';
import { SortDirection } from '../../config/app-types';
import { TableLazyLoadEvent } from 'primeng/types/table';
import { IInviteQueryParams } from '../../models/invite/IInviteQueryParams';
import { finalize } from 'rxjs';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';

type ViewMode = 'users' | 'invites';

@Component({
  selector: 'app-users',
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    TableModule,
    CardModule,
    ButtonModule,
    SelectButtonModule,
    InputTextModule,
    SelectModule,
    TagModule,
    DividerModule,
    ProgressSpinnerModule
  ],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class Users implements OnInit {

  private inviteService = inject(InviteService);
  private messageService = inject(MessageService);

  readonly currentView = signal<ViewMode>('invites');
  readonly loading = signal(false);

  viewOptions = [
    { label: 'Users', value: 'users' as ViewMode },
    { label: 'Invites', value: 'invites' as ViewMode }
  ];

  invites: IInviteResponse[] = [];
  totalRecords = 0;

  rows = 10;
  first = 0;

  inviteFilters = {
    email: '',
    status: null as InviteStatusEnum | null,
    role: null as RoleEnum | null,
    createdByAdminId: '',
    sortBy: 'createdAt',
    sortDirection: 'desc' as SortDirection
  };

  roleOptions = [
    { label: 'Sve role', value: null },
    { label: 'ADMIN', value: RoleEnum.ADMIN },
    { label: 'USER', value: RoleEnum.USER },
    { label: 'MANAGER', value: RoleEnum.MANAGER }
  ];

  inviteStatusOptions = [
    { label: 'Svi statusi', value: null },
    { label: 'PENDING', value: InviteStatusEnum.PENDING },
    { label: 'ACCEPTED', value: InviteStatusEnum.ACCEPTED },
    { label: 'EXPIRED', value: InviteStatusEnum.EXPIRED },
    { label: 'REVOKED', value: InviteStatusEnum.REVOKED }
  ];

  inviteSortByOptions = [
    { label: 'Datum kreiranja', value: 'createdAt' },
    { label: 'Datum isteka', value: 'expiresAt' },
    { label: 'Email', value: 'email' },
    { label: 'Status', value: 'status' },
    { label: 'Rola', value: 'role' }
  ];

  sortDirectionOptions = [
    { label: 'Silazno', value: 'desc' as SortDirection },
    { label: 'Uzlazno', value: 'asc' as SortDirection }
  ];

  // samo primjer da se filteri razlikuju od invite pogleda
  userFilters = {
    search: '',
    active: null,
    role: null
  };

  ngOnInit(): void {
    this.loadInvites();
  }

  onViewChange(): void {
    if (this.currentView() === 'invites') {
      this.first = 0;
      this.loadInvites();
    }
  }

  onInviteFiltersChange(): void {
    this.first = 0;
    this.loadInvites();
  }

  resetInviteFilters(): void {
    this.inviteFilters = {
      email: '',
      status: null,
      role: null,
      createdByAdminId: '',
      sortBy: 'createdAt',
      sortDirection: 'desc'
    };
    this.first = 0;
    this.loadInvites();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;

    const sortField = typeof event.sortField === 'string' ? event.sortField : null;
    if (sortField) {
      this.inviteFilters.sortBy = sortField;
      this.inviteFilters.sortDirection = event.sortOrder === 1 ? 'asc' : 'desc';
    }

    this.loadInvites();
  }

  private buildInviteParams(): IInviteQueryParams {
    return {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      email: this.inviteFilters.email?.trim() || null,
      status: this.inviteFilters.status,
      role: this.inviteFilters.role,
      createdByAdminId: this.inviteFilters.createdByAdminId?.trim() || null,
      sortBy: this.inviteFilters.sortBy,
      sortDirection: this.inviteFilters.sortDirection
    };
  }

  private loadInvites(): void {
    this.loading.set(true);

    this.inviteService.getInvites(this.buildInviteParams())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.invites = response.items;
          this.totalRecords = response.totalItems;
        },
        // TODO: dodaj da se na toast to ispise
        error: (err) => {
          this.messageService.add({
            severity: 'warn',
            summary: 'Greška',
            detail: 'Nije moguće dohvatiti podatke'
          });
          console.error('Greška pri dohvaćanju inviteova', err);
          this.invites = [];
          this.totalRecords = 0;
        }
      });
  }

  getInviteSeverity(status: InviteStatusEnum): 'success' | 'warn' | 'danger' | 'info' | 'secondary' {
    switch (status) {
      case InviteStatusEnum.ACCEPTED:
        return 'success';
      case InviteStatusEnum.PENDING:
        return 'info';
      case InviteStatusEnum.EXPIRED:
        return 'warn';
      case InviteStatusEnum.REVOKED:
        return 'danger';
      default:
        return 'info';
    }
  }

  isExpired(expiresAt: string): boolean {
    return new Date(expiresAt).getTime() < Date.now();
  }
}
