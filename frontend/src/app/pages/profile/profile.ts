import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { startRegistration } from '@simplewebauthn/browser';
import QRCode from 'qrcode';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputOtpModule } from 'primeng/inputotp';
import { TagModule } from 'primeng/tag';
import { ITotpSetupResponse, ITwoFactorSettings } from '../../models/auth/ITwoFactor';
import { AuthService } from '../../services/auth/auth.service';

@Component({
  selector: 'app-profile',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    TagModule,
    InputOtpModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {
  private authService = inject(AuthService);
  private messageService = inject(MessageService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  readonly loggingOut = signal(false);
  readonly loggingOutEverywhere = signal(false);
  readonly loadingSecurity = signal(false);
  readonly totpSetup = signal<ITotpSetupResponse | null>(null);
  readonly totpQrCode = signal<string | null>(null);
  readonly webauthnLoading = signal(false);

  readonly username = this.authService.username;
  readonly roles = this.authService.roles;
  readonly settings = signal<ITwoFactorSettings | null>(null);

  totpForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  ngOnInit(): void {
    this.loadSecuritySettings();
  }

  loadSecuritySettings(): void {
    this.loadingSecurity.set(true);
    this.authService.getTwoFactorSettings().subscribe({
      next: (settings) => {
        this.settings.set(settings);
        this.loadingSecurity.set(false);
      },
      error: () => {
        this.loadingSecurity.set(false);
      },
    });
  }

  startTotpSetup(): void {
    this.authService.startTotpSetup().subscribe({
      next: async (setup) => {
        this.totpSetup.set(setup);
        this.totpForm.reset();
        this.totpQrCode.set(await QRCode.toDataURL(setup.otpauthUrl, { width: 192, margin: 1 }));
      },
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'TOTP setup nije pokrenut',
        detail: 'Pokusaj ponovno.'
      }),
    });
  }

  confirmTotpSetup(): void {
    if (this.totpForm.invalid) {
      this.totpForm.markAllAsTouched();
      return;
    }

    this.authService.confirmTotpSetup(this.totpForm.getRawValue().code!).subscribe({
      next: () => {
        this.totpSetup.set(null);
        this.totpQrCode.set(null);
        this.totpForm.reset();
        this.loadSecuritySettings();
        this.messageService.add({
          severity: 'success',
          summary: 'TOTP je ukljucen',
          detail: 'Authenticator kodovi su sada aktivni za prijavu.'
        });
      },
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'Kod nije ispravan',
        detail: 'Provjeri kod iz authenticator aplikacije.'
      }),
    });
  }

  disableTotp(): void {
    this.authService.disableTotp().subscribe({
      next: () => {
        this.loadSecuritySettings();
        this.messageService.add({
          severity: 'success',
          summary: 'TOTP je iskljucen',
          detail: 'Authenticator kod vise nije potreban.'
        });
      },
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'TOTP nije iskljucen',
        detail: 'Pokusaj ponovno.'
      }),
    });
  }

  registerPasskey(): void {
    if (this.webauthnLoading()) return;

    if (!window.isSecureContext || !window.PublicKeyCredential) {
      this.showPasskeyError('Passkey radi samo na HTTPS-u ili localhostu u browseru koji podrzava WebAuthn.');
      return;
    }

    this.webauthnLoading.set(true);
    this.authService.startWebAuthnRegistration().subscribe({
      next: async ({ token, optionsJson }) => {
        try {
          const response = await startRegistration({ optionsJSON: JSON.parse(optionsJson) });
          this.authService.finishWebAuthnRegistration(token, JSON.stringify(response)).subscribe({
            next: () => {
              this.webauthnLoading.set(false);
              this.loadSecuritySettings();
              this.messageService.add({
                severity: 'success',
                summary: 'Passkey je spremljen',
                detail: 'Mozes ga koristiti za login.'
              });
            },
            error: () => {
              this.webauthnLoading.set(false);
              this.showPasskeyError('Backend nije prihvatio passkey provjeru. Provjeri backend log za WebAuthn detalj.');
            },
          });
        } catch (error) {
          this.webauthnLoading.set(false);
          this.showPasskeyError(this.passkeyErrorDetail(error));
        }
      },
      error: () => {
        this.webauthnLoading.set(false);
        this.showPasskeyError('Backend nije mogao pokrenuti passkey registraciju.');
      },
    });
  }

  deletePasskey(id: number): void {
    this.authService.deleteWebAuthnCredential(id).subscribe({
      next: () => {
        this.loadSecuritySettings();
        this.messageService.add({
          severity: 'success',
          summary: 'Passkey je obrisan',
          detail: 'Credential vise nije aktivan.'
        });
      },
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'Passkey nije obrisan',
        detail: 'Pokusaj ponovno.'
      }),
    });
  }

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

  private showPasskeyError(detail = 'Provjera nije dovrsena.'): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Passkey nije spremljen',
      detail
    });
  }

  private passkeyErrorDetail(error: unknown): string {
    console.error('WebAuthn registration failed', error);

    if (error instanceof Error) {
      if (error.name === 'SecurityError') {
        return 'Origin aplikacije ne odgovara WebAuthn RP ID postavci.';
      }
      if (error.name === 'NotAllowedError') {
        return 'Passkey potvrda je odbijena, prekinuta ili istekla.';
      }
      return error.message || 'Browser nije dovrsio WebAuthn provjeru.';
    }

    return 'Browser nije dovrsio WebAuthn provjeru.';
  }
}
