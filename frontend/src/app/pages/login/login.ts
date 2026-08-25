import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { startAuthentication } from '@simplewebauthn/browser';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { InputOtpModule } from 'primeng/inputotp';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { AuthService } from '../../services/auth/auth.service';

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
    InputOtpModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  loading = false;
  passkeyLoading = false;
  twoFactorLoading = false;
  twoFactorToken: string | null = null;
  twoFactorMethod: 'totp' | 'webauthn' | null = null;
  availableMethods: string[] = [];
  rememberForSecondStep = true;

  private fb = inject(FormBuilder);
  private messageService = inject(MessageService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  form = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
    remember: [true],
  });

  totpForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
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
        detail: 'Molimo popunite sva polja.',
      });

      return;
    }

    this.setLoading(true);
    const { username, password, remember } = this.form.getRawValue();

    this.auth.login({ username: username!, password: password! }, !!remember).subscribe((res) => {
      this.setLoading(false);

      if (!res) {
        this.messageService.add({
          severity: 'error',
          summary: 'Neuspjesna prijava',
          detail: 'Provjeri korisnicko ime/lozinku.',
        });
        return;
      }

      if (res.status === 'TWO_FACTOR_REQUIRED' && res.twoFactorToken) {
        this.twoFactorToken = res.twoFactorToken;
        this.twoFactorMethod = res.primaryMethod ?? 'totp';
        this.availableMethods = res.availableMethods ?? [];
        this.rememberForSecondStep = !!remember;
        this.cdr.detectChanges();

        return;
      }

      if (res.status === 'AUTHENTICATED') {
        this.router.navigateByUrl(this.getReturnUrl());
      }
    });
  }

  submitTotp() {
    if (!this.twoFactorToken) return;
    if (this.totpForm.invalid) {
      this.totpForm.markAllAsTouched();
      return;
    }

    this.setTwoFactorLoading(true);
    const code = this.totpForm.getRawValue().code!;
    this.auth.verifyTotpLogin(this.twoFactorToken, code, this.rememberForSecondStep).subscribe((ok) => {
      this.setTwoFactorLoading(false);
      if (!ok) {
        this.messageService.add({
          severity: 'error',
          summary: 'Neispravan kod',
          detail: 'Provjeri TOTP kod i pokusaj ponovno.',
        });
        return;
      }
      this.router.navigateByUrl(this.getReturnUrl());
    });
  }

  loginWithPasskey() {
    if (this.passkeyLoading) return;

    this.setPasskeyLoading(true);
    const remember = !!this.form.getRawValue().remember;
    this.auth.startPasskeyLogin().subscribe({
      next: async ({ token, optionsJson }) => {
        try {
          const assertion = await startAuthentication({ optionsJSON: JSON.parse(optionsJson) });
          this.auth.finishPasskeyLogin(token, JSON.stringify(assertion), remember).subscribe((ok) => {
            this.setPasskeyLoading(false);
            if (!ok) {
              this.showPasskeyError();
              return;
            }
            this.router.navigateByUrl(this.getReturnUrl());
          });
        } catch {
          this.setPasskeyLoading(false);
          this.showPasskeyError();
        }
      },
      error: () => {
        this.setPasskeyLoading(false);
        this.showPasskeyError();
      },
    });
  }

  verifyWebAuthnSecondFactor() {
    if (!this.twoFactorToken || this.twoFactorLoading) return;

    this.setTwoFactorLoading(true);
    this.auth.startSecondFactorWebAuthn(this.twoFactorToken).subscribe({
      next: async ({ token, optionsJson }) => {
        try {
          const assertion = await startAuthentication({ optionsJSON: JSON.parse(optionsJson) });
          this.auth
            .finishSecondFactorWebAuthn(
              this.twoFactorToken!,
              token,
              JSON.stringify(assertion),
              this.rememberForSecondStep
            )
            .subscribe((ok) => {
              this.setTwoFactorLoading(false);
              if (!ok) {
                this.showPasskeyError();
                return;
              }
              this.router.navigateByUrl(this.getReturnUrl());
            });
        } catch {
          this.setTwoFactorLoading(false);
          this.showPasskeyError();
        }
      },
      error: () => {
        this.setTwoFactorLoading(false);
        this.showPasskeyError();
      },
    });
  }

  useTotp() {
    this.twoFactorMethod = 'totp';
  }

  useWebAuthn() {
    this.twoFactorMethod = 'webauthn';
  }

  resetLogin() {
    this.twoFactorToken = null;
    this.twoFactorMethod = null;
    this.availableMethods = [];
    this.totpForm.reset();
  }

  private showPasskeyError() {
    this.messageService.add({
      severity: 'error',
      summary: 'Passkey nije potvrden',
      detail: 'Pokusaj ponovno ili koristi drugu dostupnu metodu.',
    });
  }

  private setLoading(value: boolean) {
    this.loading = value;
    this.cdr.detectChanges();
  }

  private setPasskeyLoading(value: boolean) {
    this.passkeyLoading = value;
    this.cdr.detectChanges();
  }

  private setTwoFactorLoading(value: boolean) {
    this.twoFactorLoading = value;
    this.cdr.detectChanges();
  }

  private getReturnUrl(): string {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

    if (!returnUrl || !returnUrl.startsWith('/') || returnUrl.startsWith('//')) {
      return '/dashboard';
    }

    return returnUrl;
  }
}
