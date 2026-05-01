import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { UserAdminService } from '../../core/services/user-admin.service';
import { Role } from '../../core/models/admin.model';

@Component({
  selector: 'app-role-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatListModule,
    MatDividerModule,
    MatSnackBarModule,
    MatExpansionModule,
    MatTooltipModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1>Role Management</h1>
          <p class="subtitle">Define and manage user roles and permissions</p>
        </div>
        <button mat-raised-button color="primary" (click)="showCreateForm = !showCreateForm">
          <mat-icon>{{ showCreateForm ? 'close' : 'add' }}</mat-icon>
          {{ showCreateForm ? 'Cancel' : 'New Role' }}
        </button>
      </div>

      <!-- Create Role Form -->
      <mat-card *ngIf="showCreateForm" class="create-form-card">
        <mat-card-header>
          <mat-card-title>Create New Role</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="createForm" class="create-role-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Role Name</mat-label>
              <input matInput formControlName="name" placeholder="e.g. PROJECT_MANAGER">
              <mat-hint>Uppercase, underscores allowed</mat-hint>
              <mat-error *ngIf="createForm.get('name')?.hasError('required')">Required</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Description</mat-label>
              <input matInput formControlName="description" placeholder="What can this role do?">
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Permissions (comma-separated)</mat-label>
              <input matInput formControlName="permissionsInput" placeholder="e.g. READ_TASKS, WRITE_TASKS, MANAGE_USERS">
            </mat-form-field>

            <div class="form-actions">
              <button mat-raised-button color="primary" (click)="createRole()" [disabled]="createForm.invalid || creating">
                <mat-spinner diameter="18" *ngIf="creating"></mat-spinner>
                <span *ngIf="!creating">Create Role</span>
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      <!-- Loading -->
      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <!-- Roles List -->
      <div *ngIf="!loading" class="roles-grid">
        <mat-card *ngFor="let role of roles" class="role-card">
          <mat-card-header>
            <div mat-card-avatar class="role-icon">
              <mat-icon>security</mat-icon>
            </div>
            <mat-card-title>{{ role.name }}</mat-card-title>
            <mat-card-subtitle>{{ role.description }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="permissions-section">
              <p class="permissions-label">Permissions:</p>
              <div class="permissions-chips" *ngIf="(role.permissions?.length ?? 0) > 0">
                <mat-chip *ngFor="let perm of role.permissions" class="permission-chip">
                  {{ perm }}
                </mat-chip>
              </div>
              <p *ngIf="!role.permissions?.length" class="no-permissions">No specific permissions defined</p>
            </div>
          </mat-card-content>
          <mat-card-actions align="end">
            <button mat-button color="warn" (click)="deleteRole(role)"
                    [disabled]="isSystemRole(role.name)"
                    [matTooltip]="isSystemRole(role.name) ? 'System role cannot be deleted' : 'Delete role'">
              <mat-icon>delete</mat-icon>
              Delete
            </button>
          </mat-card-actions>
        </mat-card>

        <div *ngIf="roles.length === 0" class="empty-state">
          <mat-icon>security</mat-icon>
          <p>No custom roles defined yet</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      margin-bottom: 24px;

      h1 { margin: 0; }
      .subtitle { margin: 4px 0 0; color: rgba(0,0,0,0.6); font-size: 14px; }
    }

    .create-form-card {
      margin-bottom: 24px;

      .create-role-form {
        display: flex;
        flex-direction: column;
        gap: 8px;
        max-width: 600px;
        padding-top: 8px;
      }

      .full-width { width: 100%; }
      .form-actions { display: flex; justify-content: flex-end; padding-top: 8px; }
    }

    .roles-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
    }

    .role-card {
      .role-icon {
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: #ede7f6;
        border-radius: 50%;

        mat-icon { color: #6a1b9a; font-size: 24px; width: 24px; height: 24px; }
      }
    }

    .permissions-section {
      margin-top: 8px;

      .permissions-label {
        font-size: 12px;
        font-weight: 600;
        color: rgba(0,0,0,0.6);
        text-transform: uppercase;
        letter-spacing: 0.5px;
        margin: 0 0 8px;
      }

      .permissions-chips {
        display: flex;
        gap: 4px;
        flex-wrap: wrap;
      }

      .permission-chip {
        background-color: #ede7f6 !important;
        color: #6a1b9a !important;
        font-size: 11px !important;
        height: 22px !important;
      }

      .no-permissions {
        font-size: 13px;
        color: rgba(0,0,0,0.4);
        font-style: italic;
        margin: 0;
      }
    }

    .empty-state {
      grid-column: 1 / -1;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: rgba(0,0,0,0.4);

      mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 8px; }
    }

    mat-spinner { display: inline-block; margin-right: 8px; }
  `]
})
export class RoleListComponent implements OnInit {
  private readonly userAdminService = inject(UserAdminService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  roles: Role[] = [];
  loading = false;
  creating = false;
  showCreateForm = false;

  createForm: FormGroup = this.fb.group({
    name: ['', [Validators.required, Validators.pattern(/^[A-Z_]+$/)]],
    description: [''],
    permissionsInput: [''],
  });

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.loading = true;
    this.userAdminService.getRoles().subscribe({
      next: (roles) => {
        this.roles = roles;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        // Provide default system roles for display
        this.roles = [
          { id: 'sys-1', name: 'MASTER_ADMIN', description: 'Full system access across all tenants', permissions: ['*'] },
          { id: 'sys-2', name: 'TENANT_ADMIN', description: 'Full access within assigned tenant', permissions: ['READ_*', 'WRITE_*', 'MANAGE_USERS'] },
          { id: 'sys-3', name: 'USER', description: 'Standard user access', permissions: ['READ_TASKS', 'WRITE_OWN_TASKS'] },
        ];
      }
    });
  }

  createRole(): void {
    if (this.createForm.invalid) return;

    this.creating = true;
    const values = this.createForm.value;
    const permissions = values.permissionsInput
      ? values.permissionsInput.split(',').map((p: string) => p.trim()).filter(Boolean)
      : [];

    const role: Partial<Role> = {
      name: values.name,
      description: values.description,
      permissions,
    };

    this.userAdminService.createRole(role).subscribe({
      next: (newRole) => {
        this.roles = [...this.roles, newRole];
        this.creating = false;
        this.showCreateForm = false;
        this.createForm.reset();
        this.snackBar.open(`Role "${newRole.name}" created`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.creating = false;
        this.snackBar.open('Failed to create role', 'Close', { duration: 3000 });
      }
    });
  }

  deleteRole(role: Role): void {
    if (this.isSystemRole(role.name)) return;

    this.userAdminService.deleteRole(role.id).subscribe({
      next: () => {
        this.roles = this.roles.filter(r => r.id !== role.id);
        this.snackBar.open(`Role "${role.name}" deleted`, 'Close', { duration: 3000 });
      }
    });
  }

  isSystemRole(name: string): boolean {
    return ['MASTER_ADMIN', 'TENANT_ADMIN', 'USER'].includes(name);
  }
}
