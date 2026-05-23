import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, computed, effect, inject, input, model } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { TextareaModule } from 'primeng/textarea';

import { GroupService } from '../../services/group.service';
import { IGroupResponse } from '../../models/group/IGroupResponse';

@Component({
  selector: 'app-group-create-update-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    ButtonModule,
    InputTextModule,
    TextareaModule
  ],
  templateUrl: './group-create-update-dialog.html',
  styleUrl: './group-create-update-dialog.scss',
})
export class GroupCreateUpdateDialog {
  private fb = inject(FormBuilder);
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);

  visible = model<boolean>(false);
  group = input<IGroupResponse | null>(null);

  @Output() created = new EventEmitter<void>();
  @Output() updated = new EventEmitter<IGroupResponse>();

  readonly isEditMode = computed(() => !!this.group());
  readonly dialogHeader = computed(() => this.isEditMode() ? 'Uredi grupu' : 'Nova grupa');
  readonly submitLabel = computed(() => this.isEditMode() ? 'Spremi' : 'Kreiraj');
  readonly submitIcon = computed(() => this.isEditMode() ? 'pi pi-check' : 'pi pi-plus');

  submitting = false;

  private populateEditForm = effect(() => {
    const group = this.group();

    if (!this.visible() || !group) {
      return;
    }

    this.form.reset({
      name: group.name,
      description: group.description ?? ''
    });

    this.form.markAsPristine();
    this.form.markAsUntouched();
  });

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

    const payload = {
      name: trimmedName,
      description: description?.trim() ?? ''
    };
    const group = this.group();
    const request = group
      ? this.groupService.updateGroup(group.id, payload)
      : this.groupService.createGroup(payload);

    request
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: response => {
          const isEditMode = !!group;

          this.messageService.add({
            severity: 'success',
            summary: 'Uspjeh',
            detail: isEditMode ? 'Grupa je uspjesno azurirana' : 'Grupa je uspjesno kreirana'
          });

          this.visible.set(false);
          if (isEditMode) {
            this.updated.emit(response);
          } else {
            this.created.emit();
          }
          this.resetFormState();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: group ? 'Azuriranje grupe nije uspjelo' : 'Kreiranje grupe nije uspjelo'
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
