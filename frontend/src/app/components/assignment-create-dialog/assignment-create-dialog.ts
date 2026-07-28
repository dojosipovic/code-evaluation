import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnDestroy,
  Output,
  inject
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { DialogService, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageService } from 'primeng/api';
import { RippleModule } from 'primeng/ripple';
import { TagModule } from 'primeng/tag';

import { AssignmentService } from '../../services/assignment.service';
import { ITaskListItem } from '../../models/task/ITaskListItem';
import { AssignmentTaskSelectDialog } from '../assignment-task-select-dialog/assignment-task-select-dialog';

@Component({
  selector: 'app-assignment-create-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    DatePickerModule,
    DialogModule,
    DynamicDialogModule,
    InputNumberModule,
    InputTextModule,
    RippleModule,
    TagModule
  ],
  providers: [DialogService],
  templateUrl: './assignment-create-dialog.html',
  styleUrl: './assignment-create-dialog.scss',
})
export class AssignmentCreateDialog implements OnDestroy {
  private fb = inject(FormBuilder);
  private assignmentService = inject(AssignmentService);
  private messageService = inject(MessageService);
  private dialogService = inject(DialogService);
  private destroyRef = inject(DestroyRef);
  private cdr = inject(ChangeDetectorRef);

  @Input({ required: true }) groupId!: number;
  @Input() visible = false;

  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() saved = new EventEmitter<void>();

  selectedTask: ITaskListItem | null = null;
  submitting = false;
  taskSelectDialogRef: DynamicDialogRef<AssignmentTaskSelectDialog> | null = null;
  readonly minimumDurationMinutes = 15;
  minSelectableDate = new Date();

  form = this.fb.group(
    {
      name: this.fb.control('', {
        validators: [
          Validators.required,
          Validators.maxLength(15)
        ]
      }),
      startsAt: this.fb.control<Date | null>(null, {
        validators: [Validators.required]
      }),
      endsAt: this.fb.control<Date | null>(null, {
        validators: [Validators.required]
      }),
      points: this.fb.control<number | null>(1, {
        validators: [
          Validators.required,
          Validators.min(1)
        ]
      })
    },
    {
      validators: [this.scheduleValidator()]
    }
  );

  ngOnDestroy(): void {
    this.taskSelectDialogRef?.close();
  }

  onVisibleChange(isVisible: boolean): void {
    this.visible = isVisible;
    this.visibleChange.emit(isVisible);

    if (isVisible) {
      this.refreshMinSelectableDate();
      this.form.updateValueAndValidity();
    } else {
      this.resetFormState();
    }
  }

  openTaskSelectDialog(): void {
    if (this.taskSelectDialogRef) {
      return;
    }

    const dialogRef = this.dialogService.open(AssignmentTaskSelectDialog, {
      header: 'Odaberi zadatak',
      modal: true,
      closable: true,
      width: '82vw',
      contentStyle: { overflow: 'hidden' },
      breakpoints: {
        '960px': '90vw',
        '640px': '96vw'
      },
      data: {
        selectedTaskId: this.selectedTask?.id ?? null,
        onTaskSelected: (task: ITaskListItem) => {
          this.applySelectedTask(task);
        }
      }
    });

    if (!dialogRef) {
      return;
    }

    this.taskSelectDialogRef = dialogRef;
    dialogRef.onClose
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((task?: ITaskListItem) => {
        if (task) {
          this.applySelectedTask(task);
        }

        this.taskSelectDialogRef = null;
      });
  }

  submit(): void {
    this.refreshMinSelectableDate();
    this.form.updateValueAndValidity();

    if (this.form.invalid || !this.selectedTask || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, startsAt, endsAt, points } = this.form.getRawValue();
    const trimmedName = name?.trim();

    if (!trimmedName) {
      this.form.controls.name.setErrors({ required: true });
      this.form.controls.name.markAsTouched();
      return;
    }

    if (!startsAt || !endsAt) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;

    this.assignmentService.createAssignment({
      groupId: this.groupId,
      name: trimmedName,
      taskId: this.selectedTask.id,
      startsAt: startsAt.toISOString(),
      endsAt: endsAt.toISOString(),
      points: points ?? 1
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Uspjeh',
            detail: 'Assignment je kreiran'
          });

          this.saved.emit();
          this.onVisibleChange(false);
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: 'Greska',
            detail: 'Nije moguce kreirati assignment'
          });
        }
      });
  }

  cancel(): void {
    this.onVisibleChange(false);
  }

  onScheduleChanged(): void {
    this.form.updateValueAndValidity();
  }

  get minimumEndDate(): Date {
    const startsAt = this.form.controls.startsAt.value;

    if (!startsAt) {
      return this.minSelectableDate;
    }

    return new Date(startsAt.getTime() + this.minimumDurationMinutes * 60 * 1000);
  }

  private applySelectedTask(task: ITaskListItem): void {
    this.selectedTask = task;
    this.form.markAsDirty();
    this.cdr.detectChanges();
  }

  private resetFormState(): void {
    this.form.reset({
      name: '',
      startsAt: null,
      endsAt: null,
      points: 1
    });

    this.selectedTask = null;
    this.submitting = false;
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.form.updateValueAndValidity();
    this.refreshMinSelectableDate();
  }

  private refreshMinSelectableDate(): void {
    this.minSelectableDate = new Date();
  }

  private scheduleValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const startsAt = control.get('startsAt')?.value as Date | null;
      const endsAt = control.get('endsAt')?.value as Date | null;
      const errors: ValidationErrors = {};

      if (startsAt && startsAt.getTime() < Date.now()) {
        errors['startInPast'] = true;
      }

      if (startsAt && endsAt) {
        const durationMs = endsAt.getTime() - startsAt.getTime();

        if (durationMs < 0) {
          errors['endBeforeStart'] = true;
        } else if (durationMs < this.minimumDurationMinutes * 60 * 1000) {
          errors['minimumDuration'] = true;
        }
      }

      return Object.keys(errors).length ? errors : null;
    };
  }
}
