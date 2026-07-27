import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../services/auth/auth.service';
import { MessageService } from 'primeng/api';
import { finalize } from 'rxjs';
import { UserService } from '../../services/user.service';
import { usernameAvailableValidator } from '../../validator/username-available.validator';

@Component({
  selector: 'app-register',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    InputTextModule,
    PasswordModule,
    ButtonModule
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register implements OnInit {

  loading = false;
  token: string | null = null;
  isRegisterSuccessful = false;

  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private messageService = inject(MessageService);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private cdr = inject(ChangeDetectorRef);

  form = this.fb.group(
    {
      firstname: this.fb.control('', {
        validators: [Validators.required, Validators.maxLength(30)]
      }),
      lastname: this.fb.control('', {
        validators: [Validators.required, Validators.maxLength(40)]
      }),
      username: this.fb.control(
        '',
        {
          validators: [Validators.required, Validators.minLength(3), Validators.maxLength(50)],
          asyncValidators: [usernameAvailableValidator(this.userService)],
          updateOn: 'change'
        }
      ),
      password: ['', [Validators.required, Validators.minLength(8)]],
      repeatPassword: ['', [Validators.required]],
    },
    {
      validators: [this.passwordMatchValidator]
    }
  );

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
  }

  get usernameControl() {
    return this.form.get('username');
  }

  isInvalid(name: 'firstname' | 'lastname' | 'username' | 'password' | 'repeatPassword') {
    const c = this.form.get(name);
    return !!c && c.invalid && (c.dirty || c.touched);
  }

  submit(): void {
    if (!this.token) {
      this.messageService.add({
        severity: 'error',
        summary: 'Neispravan link',
        detail: 'Token za registraciju nije pronađen.'
      });
      return;
    }

    if (this.form.pending) {
      this.messageService.add({
        severity: 'info',
        summary: 'Provjera u tijeku',
        detail: 'Pričekaj da se provjeri username.'
      });
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();

      this.messageService.add({
        severity: 'warn',
        summary: 'Forma nije ispravna',
        detail: 'Provjeri unesene podatke.'
      });
      return;
    }

    const { username, password, firstname, lastname } = this.form.getRawValue();

    this.loading = true;

    this.authService.register({
      token: this.token,
      username: username!,
      firstname: firstname!,
      lastname: lastname!,
      password: password!
    }).pipe(
      finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: () => {
        this.isRegisterSuccessful = true
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Registracija nije uspjela',
          detail: err?.error?.message || 'Provjeri podatke i pokušaj ponovno.'
        });
      }
    });
  }

  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const repeatPassword = control.get('repeatPassword')?.value;

    if (!password || !repeatPassword) {
      return null;
    }

    return password === repeatPassword ? null : { passwordMismatch: true };
  }
}
