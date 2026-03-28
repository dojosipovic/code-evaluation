import {
  Component,
  DestroyRef,
  EventEmitter,
  Output,
  inject,
  model
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  debounceTime,
  distinctUntilChanged,
  filter,
  finalize,
  switchMap,
  tap,
  of,
  map,
  catchError
} from 'rxjs';

import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageService } from 'primeng/api';

import { RoleEnum } from '../../models/enum/RoleEnum';
import { InviteService } from '../../services/invite.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-invite-create-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    MessageModule,
    ProgressSpinnerModule
  ],
  templateUrl: './invite-create-dialog.html',
  styleUrl: './invite-create-dialog.scss',
})
export class InviteCreateDialog {
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);
  private inviteService = inject(InviteService);
  private userService = inject(UserService);
  private messageService = inject(MessageService);

  visible = model<boolean>(false);

  @Output() created = new EventEmitter<void>();

  checkingEmail = false;
  submitting = false;
  emailExists = false;

  roleOptions = [
    { label: 'ADMIN', value: RoleEnum.ADMIN },
    { label: 'USER', value: RoleEnum.USER },
    { label: 'MANAGER', value: RoleEnum.MANAGER }
  ];

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    role: [null as RoleEnum | null, [Validators.required]]
  });

  constructor() {
    this.form.controls.email.valueChanges
      .pipe(
        debounceTime(1500),
        distinctUntilChanged(),
        filter((email): email is string => !!email && this.form.controls.email.valid),
        tap(() => {
          this.checkingEmail = true;
          this.emailExists = false;
        }),
        switchMap(email =>
          this.userService.getUserByEmail(email).pipe(
            map(() => true),
            catchError(() => of(false)),
            finalize(() => (this.checkingEmail = false))
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: exists => {
          this.emailExists = exists;

          if (exists) {
            this.form.controls.email.setErrors({ emailExists: true });
          } else {
            const errors = this.form.controls.email.errors;
            if (errors?.['emailExists']) {
              delete errors['emailExists'];
              this.form.controls.email.setErrors(Object.keys(errors).length ? errors : null);
            }
          }
        },
        error: () => {
          this.checkingEmail = false;
        }
      });
  }

  onVisibleChange(isVisible: boolean): void {
    this.visible.set(isVisible);

    if (!isVisible) {
      this.resetFormState();
    }
  }

  private resetFormState(): void {
    this.form.reset({
      email: '',
      role: null
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();

    this.checkingEmail = false;
    this.emailExists = false;
    this.submitting = false;
  }

  submit(): void {
    if (this.form.invalid || this.submitting || this.checkingEmail) {
      this.form.markAllAsTouched();
      return;
    }

    const { email, role } = this.form.getRawValue();
    if (!email || !role) {
      return;
    }

    this.submitting = true;

    this.inviteService.createInvite({ email, role })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Uspjeh',
            detail: 'Invite je uspješno kreiran'
          });

          this.form.reset({
            email: '',
            role: null
          });

          this.visible.set(false);
          this.created.emit();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Greška',
            detail: 'Kreiranje invitea nije uspjelo'
          });
        }
      });
  }

  cancel(): void {
    this.resetFormState();
    this.visible.set(false);
  }
}
