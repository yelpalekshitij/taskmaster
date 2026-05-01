import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TaskService } from '../../core/services/task.service';
import { Task, TaskPriority } from '../../core/models/task.model';

export interface TaskFormDialogData {
  task?: Task;
}

@Component({
  selector: 'app-task-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data?.task ? 'Edit Task' : 'Create New Task' }}</h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="task-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Title *</mat-label>
          <input matInput formControlName="title" placeholder="Enter task title">
          <mat-error *ngIf="form.get('title')?.hasError('required')">Title is required</mat-error>
          <mat-error *ngIf="form.get('title')?.hasError('minlength')">Title must be at least 3 characters</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="3" placeholder="Describe the task..."></textarea>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Priority</mat-label>
            <mat-select formControlName="priority">
              <mat-option value="LOW">Low</mat-option>
              <mat-option value="MEDIUM">Medium</mat-option>
              <mat-option value="HIGH">High</mat-option>
              <mat-option value="CRITICAL">Critical</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Due Date</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="dueDate">
            <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
            <mat-datepicker #picker></mat-datepicker>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Tags (comma-separated)</mat-label>
          <input matInput formControlName="tagsInput" placeholder="e.g. frontend, urgent, bug">
        </mat-form-field>

        <div *ngIf="error" class="error-message">
          <span>{{ error }}</span>
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="saving">Cancel</button>
      <button mat-raised-button color="primary" (click)="save()" [disabled]="form.invalid || saving">
        <mat-spinner diameter="18" *ngIf="saving"></mat-spinner>
        <span *ngIf="!saving">{{ data?.task ? 'Update' : 'Create' }}</span>
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .task-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 480px;
      padding-top: 8px;
    }

    .full-width {
      width: 100%;
    }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .error-message {
      color: #c62828;
      font-size: 13px;
      padding: 8px 0;
    }

    mat-spinner {
      display: inline-block;
      margin-right: 8px;
    }
  `]
})
export class TaskFormDialogComponent {
  private fb = inject(FormBuilder);
  private taskService = inject(TaskService);
  private dialogRef = inject(MatDialogRef<TaskFormDialogComponent>);

  saving = false;
  error = '';

  form: FormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: TaskFormDialogData) {
    this.form = this.fb.group({
      title: [data?.task?.title || '', [Validators.required, Validators.minLength(3)]],
      description: [data?.task?.description || ''],
      priority: [data?.task?.priority || 'MEDIUM' as TaskPriority],
      dueDate: [data?.task?.dueDate ? new Date(data.task.dueDate) : null],
      tagsInput: [data?.task?.tags?.join(', ') || ''],
    });
  }

  save(): void {
    if (this.form.invalid) return;

    this.saving = true;
    this.error = '';

    const values = this.form.value;
    const tags = values.tagsInput
      ? values.tagsInput.split(',').map((t: string) => t.trim()).filter((t: string) => t)
      : [];

    const input = {
      title: values.title,
      description: values.description || undefined,
      priority: values.priority,
      dueDate: values.dueDate ? (values.dueDate as Date).toISOString().split('T')[0] : undefined,
      tags: tags.length > 0 ? tags : undefined,
    };

    this.taskService.createTask(input).subscribe({
      next: (task) => {
        this.saving = false;
        this.dialogRef.close(task);
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.message || 'Failed to create task. Please try again.';
      }
    });
  }
}
