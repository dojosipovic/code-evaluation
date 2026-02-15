import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DividerModule } from 'primeng/divider';
import { MessageService } from 'primeng/api';

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
    DividerModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  loading = false;
  form: any;

  constructor(
    private fb: FormBuilder,
    private messageService: MessageService
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required]],
      remember: [true]
    })
  }

  

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

    // za sada samo demo
    console.log('login form', this.form.getRawValue());
    setTimeout(() => {
      this.loading = false;

      this.messageService.add({
        severity: 'success',
        summary: 'Prijava uspješna',
        detail: 'Dobrodošao natrag!'
      });

    }, 800);
  }
}
