import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { TenantService } from '../../core/services/tenant.service';

@Component({
  selector: 'app-create-tenant-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>Create New Tenant</h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="tenant-form">
        <!-- Tenant Info -->
        <div class="section-header">
          <mat-icon>business</mat-icon>
          Tenant Information
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Tenant Name *</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Acme Corp">
          <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Slug *</mat-label>
          <input matInput formControlName="slug" placeholder="e.g. acme-corp">
          <mat-hint>Unique identifier for the tenant (lowercase, hyphens allowed)</mat-hint>
          <mat-error *ngIf="form.get('slug')?.hasError('required')">Slug is required</mat-error>
          <mat-error *ngIf="form.get('slug')?.hasError('pattern')">Only lowercase letters, numbers, hyphens</mat-error>
        </mat-form-field>

        <mat-divider></mat-divider>

        <!-- Admin User -->
        <div class="section-header">
          <mat-icon>person_add</mat-icon>
          First Admin User
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>First Name *</mat-label>
            <input matInput formControlName="adminFirstName">
            <mat-error *ngIf="form.get('adminFirstName')?.hasError('required')">Required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Last Name *</mat-label>
            <input matInput formControlName="adminLastName">
            <mat-error *ngIf="form.get('adminLastName')?.hasError('required')">Required</mat-error>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Admin Email *</mat-label>
          <input matInput formControlName="adminEmail" type="email" placeholder="admin@company.com">
          <mat-error *ngIf="form.get('adminEmail')?.hasError('required')">Email is required</mat-error>
          <mat-error *ngIf="form.get('adminEmail')?.hasError('email')">Enter a valid email</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Initial Password *</mat-label>
          <input matInput formControlName="adminPassword" type="password">
          <mat-error *ngIf="form.get('adminPassword')?.hasError('required')">Password is required</mat-error>
          <mat-error *ngIf="form.get('adminPassword')?.hasError('minlength')">Minimum 8 characters</mat-error>
        </mat-form-field>

        <div *ngIf="error" class="error-message">{{ error }}</div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="saving">Cancel</button>
      <button mat-raised-button color="primary" (click)="save()" [disabled]="form.invalid || saving">
        <mat-spinner diameter="18" *ngIf="saving"></mat-spinner>
        <span *ngIf="!saving">Create Tenant</span>
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .tenant-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 500px;
      padding-top: 8px;
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 600;
      color: #311b92;
      margin: 8px 0 4px;
    }

    .full-width { width: 100%; }

    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    mat-divider { margin: 8px 0; }

    .error-message {
      color: #c62828;
      font-size: 13px;
    }

    mat-spinner { display: inline-block; margin-right: 8px; }
  `]
})
export class CreateTenantDialogComponent {
  private fb = inject(FormBuilder);
  private tenantService = inject(TenantService);
  private dialogRef = inject(MatDialogRef<CreateTenantDialogComponent>);

  saving = false;
  error = '';

  form: FormGroup = this.fb.group({
    name: ['', Validators.required],
    slug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
    adminFirstName: ['', Validators.required],
    adminLastName: ['', Validators.required],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  save(): void {
    if (this.form.invalid) return;

    this.saving = true;
    this.error = '';

    this.tenantService.createTenant(this.form.value).subscribe({
      next: (tenant) => {
        this.saving = false;
        this.dialogRef.close(tenant);
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to create tenant';
      }
    });
  }
}
