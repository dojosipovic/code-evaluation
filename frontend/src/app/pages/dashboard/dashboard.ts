import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

@Component({
  selector: 'app-dashboard',
  imports: [ButtonModule, CardModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {

  me: string | null = null;

  private http = inject(HttpClient);
  private messageService = inject(MessageService);

  loadMe() {
    this.http.get('/auth/me', { responseType: 'text' }).subscribe({
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
