import {
  Component,
  DestroyRef,
  EventEmitter,
  Output,
  inject,
  model
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, AsyncValidatorFn, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import {
  finalize,
  switchMap,
  of,
  map,
  catchError,
  timer
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
    { label: RoleEnum.ADMIN, value: RoleEnum.ADMIN },
    { label: RoleEnum.PROF, value: RoleEnum.PROF },
    { label: RoleEnum.STUDENT, value: RoleEnum.STUDENT }
  ];

  form = this.fb.group({
    email: this.fb.control('', {
      validators: [
        Validators.required,
        Validators.email,
        Validators.maxLength(100)
      ],
      asyncValidators: [this.emailExistsValidator()],
      updateOn: 'change'
    }),
    role: this.fb.control<RoleEnum | null>(null, {
      validators: [Validators.required]
    })
  });

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

  private emailExistsValidator(): AsyncValidatorFn {
    return (control: AbstractControl) => {
      const email = control.value;

      if (!email) {
        return of(null);
      }

      if (control.invalid) {
        return of(null);
      }

      this.checkingEmail = true;

      return timer(1500).pipe(
        switchMap(() => this.userService.getUserByEmail(email)),
        map((): ValidationErrors | null => ({ emailExists: true })),
        catchError(() => of(null)),
        finalize(() => {
          this.checkingEmail = false;
        })
      );
    };
  }
}
