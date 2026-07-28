import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';
import { AuthService } from '../../services/auth/auth.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    CheckboxModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  loading = false;
  
  private fb = inject(FormBuilder);
  private messageService = inject(MessageService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  form = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
    remember: [true]
  });

  isInvalid(name: 'username' | 'password') {
    const c = this.form.get(name);
    return !!c && c.invalid && (c.dirty || c.touched);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();

      this.messageService.add({
        severity: 'warn',
        summary: 'Forma nije ispravna',
        detail: 'Molimo popunite sva polja.'
      });

      return;
    }

    this.loading = true;
    const { username, password, remember } = this.form.getRawValue();

    this.auth.login({ username: username!, password: password! }, !!remember).subscribe(ok => {
      this.loading = false;

      if (!ok) {
        this.messageService.add({
          severity: 'error',
          summary: 'Neuspješna prijava',
          detail: 'Provjeri korisničko ime/lozinku.'
        });
        return;
      }

      this.router.navigateByUrl(this.getReturnUrl());
    });
  }

  private getReturnUrl(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

    if (!returnUrl || !returnUrl.startsWith('/') || returnUrl.startsWith('//')) {
      return '/dashboard';
    }

    return returnUrl;
  }
}
