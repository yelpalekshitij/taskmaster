import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { UserAdminService } from '../../core/services/user-admin.service';
import { AdminUser, Role } from '../../core/models/admin.model';
import { AssignRolesDialogComponent } from './assign-roles-dialog.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatMenuModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatChipsModule,
    MatSelectModule,
    MatDialogModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1>User Management</h1>
          <p class="subtitle">{{ tenantId ? 'Users in this tenant' : 'All users in your tenant' }}</p>
        </div>
      </div>

      <!-- Search -->
      <div class="filters-bar">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search users</mat-label>
          <input matInput [(ngModel)]="searchTerm" (ngModelChange)="onSearch()" placeholder="Name, email or username...">
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <!-- Error -->
      <div *ngIf="error && !loading" class="error-container">
        <mat-icon>error</mat-icon>
        Failed to load users.
        <button mat-button (click)="loadUsers()">Retry</button>
      </div>

      <!-- Table -->
      <div class="table-container mat-elevation-z2" *ngIf="!loading && !error">
        <table mat-table [dataSource]="dataSource" matSort>

          <!-- Name Column -->
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>User</th>
            <td mat-cell *matCellDef="let user">
              <div class="user-cell">
                <div class="user-avatar">{{ getInitials(user) }}</div>
                <div>
                  <div class="user-name">{{ user.firstName }} {{ user.lastName }}</div>
                  <div class="user-username">{{ user.username }}</div>
                </div>
              </div>
            </td>
          </ng-container>

          <!-- Email Column -->
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Email</th>
            <td mat-cell *matCellDef="let user">{{ user.email }}</td>
          </ng-container>

          <!-- Status Column -->
          <ng-container matColumnDef="active">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let user">
              <span [class]="user.active ? 'active-badge' : 'inactive-badge'">
                {{ user.active ? 'Active' : 'Inactive' }}
              </span>
            </td>
          </ng-container>

          <!-- Roles Column -->
          <ng-container matColumnDef="roles">
            <th mat-header-cell *matHeaderCellDef>Roles</th>
            <td mat-cell *matCellDef="let user">
              <div class="roles-cell">
                <mat-chip *ngFor="let role of user.roles" [class]="getRoleChipClass(role)">
                  {{ role }}
                </mat-chip>
                <span *ngIf="!user.roles?.length" class="no-roles">No roles</span>
              </div>
            </td>
          </ng-container>

          <!-- Last Login Column -->
          <ng-container matColumnDef="lastLogin">
            <th mat-header-cell *matHeaderCellDef>Last Login</th>
            <td mat-cell *matCellDef="let user">
              <span *ngIf="user.lastLogin">{{ user.lastLogin | date:'MMM d, y' }}</span>
              <span *ngIf="!user.lastLogin" class="never-login">Never</span>
            </td>
          </ng-container>

          <!-- Actions Column -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let user">
              <button mat-icon-button (click)="openAssignRolesDialog(user)" matTooltip="Assign Roles">
                <mat-icon>manage_accounts</mat-icon>
              </button>
              <button mat-icon-button [matMenuTriggerFor]="actionsMenu">
                <mat-icon>more_vert</mat-icon>
              </button>
              <mat-menu #actionsMenu="matMenu">
                <button mat-menu-item (click)="toggleActive(user)">
                  <mat-icon>{{ user.active ? 'block' : 'check_circle' }}</mat-icon>
                  {{ user.active ? 'Deactivate' : 'Activate' }}
                </button>
              </mat-menu>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        <div *ngIf="dataSource.data.length === 0" class="empty-table">
          <mat-icon>people_outline</mat-icon>
          <p>No users found</p>
        </div>

        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="[10, 20, 50]"
          (page)="onPage($event)"
          showFirstLastButtons>
        </mat-paginator>
      </div>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      margin-bottom: 16px;

      h1 { margin: 0; }
      .subtitle { margin: 4px 0 0; color: rgba(0,0,0,0.6); font-size: 14px; }
    }

    .filters-bar {
      margin-bottom: 16px;
      .search-field { max-width: 400px; }
    }

    .user-cell {
      display: flex;
      align-items: center;
      gap: 12px;

      .user-avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background-color: #7b1fa2;
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 13px;
        font-weight: 600;
        flex-shrink: 0;
      }

      .user-name { font-weight: 500; font-size: 14px; }
      .user-username { font-size: 12px; color: rgba(0,0,0,0.5); }
    }

    .roles-cell {
      display: flex;
      gap: 4px;
      flex-wrap: wrap;
    }

    .no-roles, .never-login {
      color: rgba(0,0,0,0.4);
      font-style: italic;
      font-size: 13px;
    }

    table { width: 100%; }

    .empty-table {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: rgba(0,0,0,0.4);

      mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 8px; }
    }
  `]
})
export class UserListComponent implements OnInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private userAdminService = inject(UserAdminService);
  private oidcService = inject(OidcSecurityService);
  private route = inject(ActivatedRoute);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  displayedColumns = ['name', 'email', 'active', 'roles', 'lastLogin', 'actions'];
  dataSource = new MatTableDataSource<AdminUser>([]);

  loading = false;
  error = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  searchTerm = '';
  tenantId = '';
  currentTenantId = '';

  ngOnInit(): void {
    this.tenantId = this.route.snapshot.params['id'] || '';

    this.oidcService.getUserData().subscribe(userData => {
      this.currentTenantId = userData?.tenant_id || this.tenantId || 'default';
      this.loadUsers();
    });
  }

  loadUsers(): void {
    this.loading = true;
    this.error = false;

    const tid = this.tenantId || this.currentTenantId;
    this.userAdminService.getUsers(tid, this.currentPage, this.pageSize, this.searchTerm).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    this.currentPage = 0;
    this.loadUsers();
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  openAssignRolesDialog(user: AdminUser): void {
    const dialogRef = this.dialog.open(AssignRolesDialogComponent, {
      data: { user },
      width: '500px',
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.userAdminService.assignRoles(user.id, result).subscribe({
          next: (updatedUser) => {
            const idx = this.dataSource.data.findIndex(u => u.id === user.id);
            if (idx >= 0) this.dataSource.data[idx].roles = updatedUser.roles;
            this.dataSource.data = [...this.dataSource.data];
            this.snackBar.open('Roles updated', 'Close', { duration: 3000 });
          }
        });
      }
    });
  }

  toggleActive(user: AdminUser): void {
    const action = user.active
      ? this.userAdminService.deactivateUser(user.id)
      : this.userAdminService.activateUser(user.id);

    action.subscribe({
      next: () => {
        user.active = !user.active;
        this.snackBar.open(`User ${user.active ? 'activated' : 'deactivated'}`, 'Close', { duration: 3000 });
      }
    });
  }

  getInitials(user: AdminUser): string {
    if (user.firstName && user.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    return user.username?.substring(0, 2).toUpperCase() || 'U';
  }

  getRoleChipClass(role: string): string {
    if (role === 'MASTER_ADMIN') return 'role-chip-master';
    if (role === 'TENANT_ADMIN') return 'role-chip-tenant';
    return 'role-chip-user';
  }
}
