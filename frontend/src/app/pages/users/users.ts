import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { InviteService } from '../../services/invite.service';
import { IInviteResponse } from '../../models/invite/IInviteResponse';
import { InviteStatusEnum } from '../../models/enum/InviteStatusEnum';
import { RoleEnum } from '../../models/enum/RoleEnum';
import { SortDirection } from '../../config/app-types';
import { TableLazyLoadEvent } from 'primeng/types/table';
import { IInviteQueryParams } from '../../models/invite/IInviteQueryParams';
import { debounceTime, distinctUntilChanged, finalize, Subject, throttleTime } from 'rxjs';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { SelectButtonChangeEvent, SelectButtonModule } from 'primeng/selectbutton';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  private emailInput$ = new Subject<string>();
  private applyFilters$ = new Subject<void>();

  readonly currentView = signal<ViewMode>('users');
  readonly loading = signal(false);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const tab = params.get('tab');

      if (tab === 'users' || tab === 'invites') {
        this.currentView.set(tab);
        return;
      }

      this.currentView.set('users');
      this.router.navigate(['/users/users'], { replaceUrl: true });
    });

    this.emailInput$
      .pipe(
        debounceTime(1500),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(value => {
        this.inviteFilters.email = value;
        this.first = 0;
        this.loadInvites();
      });

    this.applyFilters$
      .pipe(
        throttleTime(2000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.first = 0;
        this.loadInvites();
      });
  }

  onEmailInput(value: string): void {
    this.emailInput$.next(value);
  }

  onApplyFilters(): void {
    this.applyFilters$.next();
  }

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
    role: null as RoleEnum | null
  };

  sortField = 'createdAt';
  sortOrder: 1 | -1 = -1; // desc

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

  onViewChange(event: SelectButtonChangeEvent): void {
    const view: ViewMode = event?.value;

    if (view !== 'users' && view !== 'invites') {
      return;
    }

    const currentTab = this.route.snapshot.paramMap.get('tab');
    if (currentTab === view) {
      return;
    }

    this.router.navigate(['/users', view]);
  }

  onInviteFiltersChange(): void {
    this.first = 0;
    this.loadInvites();
  }

  resetInviteFilters(): void {
    this.inviteFilters = {
      email: '',
      status: null,
      role: null
    };

    this.sortField = 'createdAt';
    this.sortOrder = -1; // desc
    this.first = 0;
    //this.loadInvites();

    this.applyFilters$.next();
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

    this.loadInvites();
  }

  private buildInviteParams(): IInviteQueryParams {
    return {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      email: this.inviteFilters.email?.trim() || null,
      status: this.inviteFilters.status,
      role: this.inviteFilters.role,
      sortBy: this.sortField,
      sortDirection: this.sortOrder === 1 ? 'asc' : 'desc'
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
        error: () => {
          this.messageService.add({
            severity: 'warn',
            summary: 'Greška',
            detail: 'Nije moguće dohvatiti podatke'
          });
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
