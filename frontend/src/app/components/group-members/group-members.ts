import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  inject,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, finalize, Subject, throttleTime } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { Popover, PopoverModule } from 'primeng/popover';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { GroupService } from '../../services/group.service';
import { SortDirection } from '../../config/app-types';
import { GroupMemberAddDialog } from '../group-member-add-dialog/group-member-add-dialog';
import { IGroupMember } from '../../models/group/IGroupMember';

@Component({
  selector: 'app-group-members',
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    ConfirmDialogModule,
    DynamicDialogModule,
    InputTextModule,
    PopoverModule,
    SkeletonModule,
    TableModule,
    TagModule
  ],
  providers: [ConfirmationService, DialogService],
  templateUrl: './group-members.html',
  styleUrl: './group-members.scss',
})
export class GroupMembers implements OnChanges, OnInit, OnDestroy {
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private dialogService = inject(DialogService);
  private destroyRef = inject(DestroyRef);

  private searchInput$ = new Subject<string>();
  private applySearch$ = new Subject<void>();

  @Input({ required: true }) groupId!: number;
  @Input() canManage = false;

  @Output() memberRemoved = new EventEmitter<void>();
  @Output() memberAdded = new EventEmitter<void>();

  @ViewChild('memberActions') memberActions!: Popover;

  readonly loading = signal(false);

  members: IGroupMember[] = [];
  selectedMember: IGroupMember | null = null;
  addMembersDialogRef: DynamicDialogRef<GroupMemberAddDialog> | null = null;
  skeletonRows = Array.from({ length: 5 });
  totalRecords = 0;

  rows = 10;
  first = 0;
  sortField = 'id';
  sortDirection: SortDirection = 'asc';

  memberFilters = {
    search: ''
  };

  ngOnInit(): void {
    this.searchInput$
      .pipe(
        debounceTime(1500),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(value => {
        this.memberFilters.search = value;
        this.first = 0;
        this.loadMembers();
      });

    this.applySearch$
      .pipe(
        throttleTime(2000, undefined, { leading: true, trailing: false }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.first = 0;
        this.loadMembers();
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['groupId'] && !changes['groupId'].firstChange) {
      this.first = 0;
      this.loadMembers();
    }
  }

  ngOnDestroy(): void {
    this.addMembersDialogRef?.close();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.first = event.first ?? 0;
    this.rows = event.rows ?? 10;

    if (event.sortField === 'id' || event.sortField === 'enabled' || event.sortField === 'addedAt') {
      this.sortField = event.sortField;
    }

    if (event.sortOrder === 1 || event.sortOrder === -1) {
      this.sortDirection = event.sortOrder === 1 ? 'asc' : 'desc';
    }

    this.loadMembers();
  }

  onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  onApplySearch(): void {
    this.applySearch$.next();
  }

  resetSearch(): void {
    this.memberFilters = {
      search: ''
    };
    this.first = 0;
    this.applySearch$.next();
  }

  openAddMembersDialog(): void {
    if (!this.canManage || this.addMembersDialogRef) {
      return;
    }

    this.addMembersDialogRef = this.dialogService.open(GroupMemberAddDialog, {
      header: 'Dodaj clanove',
      modal: true,
      closable: true,
      width: '78vw',
      contentStyle: { overflow: 'hidden' },
      breakpoints: {
        '960px': '88vw',
        '640px': '96vw'
      },
      data: {
        groupId: this.groupId,
        onMemberAdded: () => {
          this.memberAdded.emit();
          this.loadMembers();
        }
      }
    });

    this.addMembersDialogRef?.onClose
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.addMembersDialogRef = null;
      });
  }

  openMemberActions(event: Event, member: IGroupMember): void {
    if (!this.canManage) {
      return;
    }

    if (this.selectedMember?.id === member.id) {
      this.memberActions.hide();
      this.selectedMember = null;
      return;
    }

    this.selectedMember = member;
    this.memberActions.hide();

    setTimeout(() => {
      this.memberActions.show(event);
    });
  }

  removeMember(member: IGroupMember): void {
    this.memberActions.hide();

    this.confirmationService.confirm({
      message: `Jesi siguran da zelis ukloniti korisnika "${this.getMemberName(member)}" iz grupe?`,
      header: 'Uklanjanje clana',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Ukloni',
      rejectLabel: 'Odustani',
      acceptButtonStyleClass: 'p-button-danger',
      rejectButtonStyleClass: 'p-button-secondary p-button-outlined',
      accept: () => {
        this.groupService.removeMember(this.groupId, member.id).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Uspjeh',
              detail: 'Clan je uklonjen iz grupe'
            });

            this.selectedMember = null;
            this.memberRemoved.emit();
            this.loadMembers();
          },
          error: () => {
            this.messageService.add({
              severity: 'error',
              summary: 'Greska',
              detail: 'Nije moguce ukloniti clana iz grupe'
            });
          }
        });
      }
    });
  }

  getMemberName(member: IGroupMember): string {
    const fullName = `${member.firstname ?? ''} ${member.lastname ?? ''}`.trim();

    return fullName || member.username || member.email || '-';
  }

  private loadMembers(): void {
    if (!Number.isFinite(this.groupId)) {
      return;
    }

    this.loading.set(true);

    this.groupService.getMembers(this.groupId, {
      page: Math.floor(this.first / this.rows),
      size: this.rows,
      search: this.memberFilters.search?.trim() || null,
      sortBy: this.sortField,
      sortDirection: this.sortDirection
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: response => {
          this.members = response.items;
          this.totalRecords = response.totalItems;
        },
        error: () => {
          this.members = [];
          this.totalRecords = 0;
          this.messageService.add({
            severity: 'warn',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti clanove grupe'
          });
        }
      });
  }
}
