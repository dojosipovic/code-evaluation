import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-profile',
  imports: [
    CommonModule,
    ButtonModule,
    TagModule
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile {
  private authService = inject(AuthService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  readonly loggingOut = signal(false);
  readonly loggingOutEverywhere = signal(false);

  readonly username = this.authService.username;
  readonly roles = this.authService.roles;

  logout(): void {
    if (this.loggingOut() || this.loggingOutEverywhere()) {
      return;
    }

    this.loggingOut.set(true);

    this.authService.logout().subscribe(ok => {
      this.loggingOut.set(false);

      if (!ok) {
        this.messageService.add({
          severity: 'warn',
          summary: 'Odjava',
          detail: 'Lokalna sesija je zatvorena.'
        });
      }

      this.router.navigateByUrl('/login');
    });
  }

  logoutEverywhere(): void {
    if (this.loggingOut() || this.loggingOutEverywhere()) {
      return;
    }

    this.loggingOutEverywhere.set(true);

    this.authService.logoutEverywhere().subscribe(ok => {
      this.loggingOutEverywhere.set(false);

      if (!ok) {
        this.messageService.add({
          severity: 'warn',
          summary: 'Odjava',
          detail: 'Lokalna sesija je zatvorena.'
        });
      }

      this.router.navigateByUrl('/login');
    });
  }
}
