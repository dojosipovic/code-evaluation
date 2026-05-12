import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, inject, model } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { TextareaModule } from 'primeng/textarea';

import { GroupService } from '../../services/group.service';

@Component({
  selector: 'app-group-create-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    TextareaModule
  ],
  templateUrl: './group-create-dialog.html',
  styleUrl: './group-create-dialog.scss',
})
export class GroupCreateDialog {
  private fb = inject(FormBuilder);
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);

  visible = model<boolean>(false);

  @Output() created = new EventEmitter<void>();

  submitting = false;

  form = this.fb.group({
    name: this.fb.control('', {
      validators: [
        Validators.required,
        Validators.maxLength(100)
      ]
    }),
    description: this.fb.control('', {
      validators: [Validators.maxLength(1000)]
    })
  });

  onVisibleChange(isVisible: boolean): void {
    this.visible.set(isVisible);

    if (!isVisible) {
      this.resetFormState();
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, description } = this.form.getRawValue();
    const trimmedName = name?.trim();

    if (!trimmedName) {
      this.form.controls.name.setErrors({ required: true });
      this.form.controls.name.markAsTouched();
      return;
    }

    this.submitting = true;

    this.groupService.createGroup({
      name: trimmedName,
      description: description?.trim() ?? ''
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Uspjeh',
            detail: 'Grupa je uspjesno kreirana'
          });

          this.visible.set(false);
          this.created.emit();
          this.resetFormState();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Kreiranje grupe nije uspjelo'
          });
        }
      });
  }

  cancel(): void {
    this.resetFormState();
    this.visible.set(false);
  }

  private resetFormState(): void {
    this.form.reset({
      name: '',
      description: ''
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.submitting = false;
  }
}
