import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { InviteService } from '../../services/invite.service';
import { UserService } from '../../services/user.service';
import { IInviteResponse } from '../../models/invite/IInviteResponse';
import { IUserResponse } from '../../models/user/IUserResponse';
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
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { InviteCreateDialog } from '../../components/invite-create-dialog/invite-create-dialog';
import { IUserQueryParams } from '../../models/user/IUserQueryParams';

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
    ProgressSpinnerModule,
    ConfirmDialogModule,
    InviteCreateDialog
  ],
  providers: [ConfirmationService],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class Users implements OnInit {

  private inviteService = inject(InviteService);
  private userService = inject(UserService);
  private messageService = inject(MessageService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private confirmationService = inject(ConfirmationService);

  private emailInput$ = new Subject<string>();
  private userSearchInput$ = new Subject<string>();
  private applyFilters$ = new Subject<void>();

  readonly currentView = signal<ViewMode>('users');
  readonly loading = signal(false);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const tab = params.get('tab');

      if (tab === 'users' || tab === 'invites') {
        this.currentView.set(tab);

        this.first = 0;

        if (tab === 'invites') {
          this.loadInvites();
        } else {
          this.loadUsers();
        }

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

    this.userSearchInput$
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

    this.applyFilters$
      .pipe(
        throttleTime(2000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.first = 0;

        if (this.currentView() === 'invites') {
          this.loadInvites();
        } else {
          this.loadUsers();
        }
      });
  }

  createInviteDialogVisible = false;

  openCreateInviteDialog(): void {
    this.createInviteDialogVisible = true;
  }

  onInviteCreated(): void {
    this.createInviteDialogVisible = false;
    this.loadInvites();
  }

  onEmailInput(value: string): void {
    this.emailInput$.next(value);
  }

  onUserSearchInput(value: string): void {
    this.userSearchInput$.next(value);
  }

  onApplyFilters(): void {
    this.applyFilters$.next();
  }

  viewOptions = [
    { label: 'Users', value: 'users' as ViewMode },
    { label: 'Invites', value: 'invites' as ViewMode }
  ];

  invites: IInviteResponse[] = [];
  users: IUserResponse[] = [];
  totalRecords = 0;

  rows = 10;
  first = 0;

  inviteFilters = {
    email: '',
    status: null as InviteStatusEnum | null,
    role: null as RoleEnum | null
  };

  userFilters = {
    search: '',
    enabled: null as boolean | null,
    role: null as RoleEnum | null
  };

  sortField = 'createdAt';
  sortOrder: 1 | -1 = -1;

  roleOptions = [
    { label: 'Sve role', value: null },
    { label: RoleEnum.ADMIN, value: RoleEnum.ADMIN },
    { label: RoleEnum.STUDENT, value: RoleEnum.STUDENT },
    { label: RoleEnum.PROF, value: RoleEnum.PROF }
  ];

  enabledOptions = [
    { label: 'Svi statusi', value: null },
    { label: 'Enabled', value: true },
    { label: 'Disabled', value: false }
  ];

  inviteStatusOptions = [
    { label: 'Svi statusi', value: null },
    { label: InviteStatusEnum.PENDING, value: InviteStatusEnum.PENDING },
    { label: InviteStatusEnum.ACCEPTED, value: InviteStatusEnum.ACCEPTED },
    { label: InviteStatusEnum.EXPIRED, value: InviteStatusEnum.EXPIRED },
    { label: InviteStatusEnum.REVOKED, value: InviteStatusEnum.REVOKED }
  ];

  sortDirectionOptions = [
    { label: 'Silazno', value: 'desc' as SortDirection },
    { label: 'Uzlazno', value: 'asc' as SortDirection }
  ];

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

  onUserFiltersChange(): void {
    this.first = 0;
    this.loadUsers();
  }

  resetInviteFilters(): void {
    this.inviteFilters = {
      email: '',
      status: null,
      role: null
    };

    this.sortField = 'createdAt';
    this.sortOrder = -1;
    this.first = 0;

    this.applyFilters$.next();
  }

  resetUserFilters(): void {
    this.userFilters = {
      search: '',
      enabled: null,
      role: null
    };

    this.sortField = 'username';
    this.sortOrder = 1;
    this.first = 0;

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

    if (this.currentView() === 'invites') {
      this.loadInvites();
    } else {
      this.loadUsers();
    }
  }

  onRevokeInvite(id: number): void {
    this.confirmationService.confirm({
      message: 'Jesi siguran da želiš opozvati ovaj invite?',
      header: 'Potvrda',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Opozovi',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        this.inviteService.revokeInvite(id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Uspjeh',
              detail: 'Invite je opozvan'
            });

            this.loadInvites();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Greška',
              detail: 'Nije moguće opozvati invite'
            });
          }
        });
      }
    });
  }

  onToggleUserStatus(user: IUserResponse): void {
    const isEnabled = user.enabled;
    const actionLabel = isEnabled ? 'deaktivirati' : 'aktivirati';
    const successLabel = isEnabled ? 'deaktiviran' : 'aktiviran';

    this.confirmationService.confirm({
      message: `Jesi siguran da želiš ${actionLabel} korisnika ${user.username}?`,
      header: 'Potvrda',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: isEnabled ? 'Deaktiviraj' : 'Aktiviraj',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: isEnabled
        ? 'p-button-danger'
        : 'p-button-success',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        const request$ = isEnabled
          ? this.userService.disableUser(user.id)
          : this.userService.enableUser(user.id);

        request$.subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Uspjeh',
              detail: `Korisnik je ${successLabel}`
            });

            this.loadUsers();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Greška',
              detail: `Nije moguće ${actionLabel} korisnika`
            });
          }
        });
      }
    });
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

  private buildUserParams(): IUserQueryParams {
    return {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      search: this.userFilters.search?.trim() || null,
      role: this.userFilters.role,
      enabled: this.userFilters.enabled,
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

  private loadUsers(): void {
    this.loading.set(true);

    this.userService.getUsers(this.buildUserParams())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.users = response.items;
          this.totalRecords = response.totalItems;
        },
        error: () => {
          this.messageService.add({
            severity: 'warn',
            summary: 'Greška',
            detail: 'Nije moguće dohvatiti korisnike'
          });
          this.users = [];
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
