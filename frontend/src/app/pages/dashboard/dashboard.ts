import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TaskCreateDialog } from '../../components/task-create-dialog/task-create-dialog';
import { ConfigService } from '../../services/config.service';

@Component({
  selector: 'app-dashboard',
  imports: [ButtonModule, CardModule, TaskCreateDialog],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {

  me: string | null = null;
  editorOpen = false;

  private http = inject(HttpClient);
  private messageService = inject(MessageService);
  private config = inject(ConfigService);

  loadMe() {
    this.http.get(`${this.config.apiUrl}/api/auth/me`, { responseType: 'text' }).subscribe({
      next: (data: string) => {
        this.me = data;

        this.messageService.add({
          severity: 'success',
          summary: 'Uspjeh',
          detail: 'Podaci dohvaćeni'
        });
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: 'Greška',
          detail: 'Nije moguće dohvatiti podatke'
        });
      }
    });
  }
}
