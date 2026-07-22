import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, OnChanges, OnInit, SimpleChanges, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MessageService } from 'primeng/api';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { GroupService } from '../../services/group.service';
import { IGroupLeaderboardResponse } from '../../models/group/IGroupLeaderboardResponse';
import { IUserResponse } from '../../models/user/IUserResponse';

@Component({
  selector: 'app-group-leaderboard',
  imports: [
    CommonModule,
    ButtonModule,
    SkeletonModule,
    TableModule,
    TagModule
  ],
  templateUrl: './group-leaderboard.html',
  styleUrl: './group-leaderboard.scss',
})
export class GroupLeaderboard implements OnInit, OnChanges {
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  @Input({ required: true }) groupId!: number;

  readonly loading = signal(false);

  leaderboard: IGroupLeaderboardResponse[] = [];
  skeletonRows = Array.from({ length: 5 });

  ngOnInit(): void {
    this.loadLeaderboard();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['groupId'] && !changes['groupId'].firstChange) {
      this.loadLeaderboard();
    }
  }

  refresh(): void {
    this.loadLeaderboard();
  }

  getRankSeverity(rank: number): 'success' | 'info' | 'warn' | 'secondary' {
    if (rank === 1) {
      return 'success';
    }

    if (rank === 2) {
      return 'info';
    }

    if (rank === 3) {
      return 'warn';
    }

    return 'secondary';
  }

  getUserName(user: IUserResponse | null | undefined): string {
    if (!user) {
      return '-';
    }

    const fullName = `${user.firstname ?? ''} ${user.lastname ?? ''}`.trim();

    return fullName || user.username || user.email || '-';
  }

  private loadLeaderboard(): void {
    if (!Number.isFinite(this.groupId)) {
      return;
    }

    this.loading.set(true);

    this.groupService.getLeaderBoard(this.groupId)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: response => {
          this.leaderboard = response;
        },
        error: () => {
          this.leaderboard = [];
          this.messageService.add({
            severity: 'warn',
            summary: 'Greska',
            detail: 'Nije moguce dohvatiti leaderboard grupe'
          });
        }
      });
  }
}
