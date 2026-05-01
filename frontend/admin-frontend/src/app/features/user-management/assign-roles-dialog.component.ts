import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatListModule } from '@angular/material/list';
import { UserAdminService } from '../../core/services/user-admin.service';
import { AdminUser, Role } from '../../core/models/admin.model';

@Component({
  selector: 'app-assign-roles-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatListModule,
  ],
  template: `
    <h2 mat-dialog-title>Assign Roles to {{ data.user.username }}</h2>

    <mat-dialog-content>
      <div *ngIf="loading" class="loading-container">
        <mat-spinner diameter="32"></mat-spinner>
      </div>

      <div *ngIf="!loading">
        <p class="instructions">Select roles to assign to this user:</p>

        <mat-selection-list [(ngModel)]="selectedRoles" class="role-list">
          <mat-list-option *ngFor="let role of availableRoles" [value]="role.name" checkboxPosition="before">
            <div matListItemTitle>{{ role.name }}</div>
            <div matListItemLine>{{ role.description }}</div>
          </mat-list-option>
        </mat-selection-list>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" (click)="save()" [disabled]="loading">
        Save Roles
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .instructions {
      font-size: 14px;
      color: rgba(0,0,0,0.6);
      margin-bottom: 8px;
    }

    .role-list {
      min-width: 400px;
    }
  `]
})
export class AssignRolesDialogComponent implements OnInit {
  private userAdminService = inject(UserAdminService);
  private dialogRef = inject(MatDialogRef<AssignRolesDialogComponent>);

  loading = false;
  availableRoles: Role[] = [];
  selectedRoles: string[] = [];

  constructor(@Inject(MAT_DIALOG_DATA) public data: { user: AdminUser }) {
    this.selectedRoles = [...(data.user.roles || [])];
  }

  ngOnInit(): void {
    this.loading = true;
    this.userAdminService.getRoles().subscribe({
      next: (roles) => {
        this.availableRoles = roles;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        // Fallback with common roles
        this.availableRoles = [
          { id: '1', name: 'TENANT_ADMIN', description: 'Can manage tenant users and settings', permissions: [] },
          { id: '2', name: 'TASK_MANAGER', description: 'Can manage all tasks in the tenant', permissions: [] },
          { id: '3', name: 'USER', description: 'Basic user access', permissions: [] },
        ];
      }
    });
  }

  save(): void {
    this.dialogRef.close(this.selectedRoles);
  }
}
