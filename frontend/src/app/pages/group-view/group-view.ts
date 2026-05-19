import { CommonModule } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter, finalize } from 'rxjs';
import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { BadgeModule } from 'primeng/badge';
import { CardModule } from 'primeng/card';
import { ScrollPanelModule } from 'primeng/scrollpanel';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { MessageService } from 'primeng/api';
import { GroupService } from '../../services/group.service';
import { BreadcrumbService } from '../../services/breadcrumb.service';
import { IGroupResponse } from '../../models/group/IGroupResponse';
import { GroupCreateDialog } from '../../components/group-create-dialog/group-create-dialog';

type GroupTab = 'users' | 'tasks';

@Component({
  selector: 'app-group-view',
  imports: [
    CommonModule,
    AccordionModule,
    BadgeModule,
    ButtonModule,
    CardModule,
    RouterModule,
    ScrollPanelModule,
    SkeletonModule,
    TabsModule,
    GroupCreateDialog
  ],
  templateUrl: './group-view.html',
  styleUrl: './group-view.scss',
})
export class GroupView implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);
  private breadcrumbService = inject(BreadcrumbService);

  readonly loading = signal(false);
  readonly activeTab = signal<GroupTab>('users');

  group: IGroupResponse | null = null;
  updateGroupDialogVisible = false;

  ngOnInit(): void {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        const tab = params.get('tab');
        const groupId = Number(params.get('id'));

        if (tab !== 'users' && tab !== 'tasks') {
          this.router.navigate(['/groups', groupId, 'users'], { replaceUrl: true });
          return;
        }

        this.activeTab.set(tab);

        if (!Number.isFinite(groupId)) {
          this.router.navigate(['/groups'], { replaceUrl: true });
          return;
        }

        if (this.group?.id !== groupId) {
          this.loadGroup(groupId);
        } else {
          this.updateBreadcrumb();
        }
      });

    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.updateBreadcrumb());
  }

  onTabChange(value: string | number | undefined): void {
    if ((value !== 'users' && value !== 'tasks') || !this.group) {
      return;
    }

    this.router.navigate(['/groups', this.group.id, value]);
  }

  openUpdateGroupDialog(event?: Event): void {
    event?.stopPropagation();
    this.updateGroupDialogVisible = true;
  }

  onGroupUpdated(group: IGroupResponse): void {
    this.group = group;
    this.updateBreadcrumb();
  }

  private loadGroup(id: number): void {
    this.loading.set(true);

    this.groupService.getGroup(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: group => {
          this.group = group;
          this.updateBreadcrumb();
        },
        error: () => {
          this.group = null;
          this.messageService.add({
            severity: 'warn',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti grupu'
          });
          this.router.navigate(['/groups']);
        }
      });
  }

  private updateBreadcrumb(): void {
    if (!this.group) {
      return;
    }

    this.breadcrumbService.set([
      { label: 'Grupe', routerLink: '/groups' },
      { label: this.breadcrumbService.shorten(this.group.name) }
    ]);
  }
}
