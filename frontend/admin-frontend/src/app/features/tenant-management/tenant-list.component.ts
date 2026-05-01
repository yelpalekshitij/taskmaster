import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { TenantService } from '../../core/services/tenant.service';
import { Tenant } from '../../core/models/admin.model';
import { CreateTenantDialogComponent } from './create-tenant-dialog.component';

@Component({
  selector: 'app-tenant-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatMenuModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatChipsModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1>Tenant Management</h1>
          <p class="subtitle">Manage all organizations in the system</p>
        </div>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add_business</mat-icon>
          New Tenant
        </button>
      </div>

      <!-- Filters -->
      <div class="filters-bar">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search tenants</mat-label>
          <input matInput [(ngModel)]="searchTerm" (ngModelChange)="onSearch($event)" placeholder="Name or slug...">
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline" class="status-filter">
          <mat-label>Status</mat-label>
          <mat-select [(ngModel)]="statusFilter" (ngModelChange)="onStatusFilter()">
            <mat-option value="">All</mat-option>
            <mat-option [value]="true">Active</mat-option>
            <mat-option [value]="false">Inactive</mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <!-- Error -->
      <div *ngIf="error && !loading" class="error-container">
        <mat-icon>error</mat-icon>
        Failed to load tenants.
        <button mat-button (click)="loadTenants()">Retry</button>
      </div>

      <!-- Table -->
      <div class="table-container mat-elevation-z2" *ngIf="!loading && !error">
        <table mat-table [dataSource]="dataSource" matSort>

          <!-- Name Column -->
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Organization</th>
            <td mat-cell *matCellDef="let tenant">
              <div class="tenant-name-cell">
                <div class="tenant-avatar">{{ tenant.name.charAt(0).toUpperCase() }}</div>
                <div>
                  <div class="tenant-name">{{ tenant.name }}</div>
                  <div class="tenant-slug">{{ tenant.slug }}</div>
                </div>
              </div>
            </td>
          </ng-container>

          <!-- Status Column -->
          <ng-container matColumnDef="active">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let tenant">
              <span [class]="tenant.active ? 'active-badge' : 'inactive-badge'">
                {{ tenant.active ? 'Active' : 'Inactive' }}
              </span>
            </td>
          </ng-container>

          <!-- Users Column -->
          <ng-container matColumnDef="userCount">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Users</th>
            <td mat-cell *matCellDef="let tenant">
              <div class="users-cell">
                <mat-icon>people</mat-icon>
                {{ tenant.userCount || 0 }}
              </div>
            </td>
          </ng-container>

          <!-- Tasks Column -->
          <ng-container matColumnDef="taskCount">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Tasks</th>
            <td mat-cell *matCellDef="let tenant">
              <div class="tasks-cell">
                <mat-icon>task_alt</mat-icon>
                {{ tenant.taskCount || 0 }}
              </div>
            </td>
          </ng-container>

          <!-- Created Column -->
          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Created</th>
            <td mat-cell *matCellDef="let tenant">{{ tenant.createdAt | date:'MMM d, y' }}</td>
          </ng-container>

          <!-- Actions Column -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let tenant">
              <a mat-icon-button [routerLink]="['/tenants', tenant.id, 'users']" matTooltip="Manage Users">
                <mat-icon>people</mat-icon>
              </a>
              <button mat-icon-button [matMenuTriggerFor]="actionsMenu">
                <mat-icon>more_vert</mat-icon>
              </button>
              <mat-menu #actionsMenu="matMenu">
                <button mat-menu-item (click)="toggleActive(tenant)">
                  <mat-icon>{{ tenant.active ? 'block' : 'check_circle' }}</mat-icon>
                  {{ tenant.active ? 'Deactivate' : 'Activate' }}
                </button>
              </mat-menu>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        <div *ngIf="dataSource.data.length === 0" class="empty-table">
          <mat-icon>business_center</mat-icon>
          <p>No tenants found</p>
          <button mat-raised-button color="primary" (click)="openCreateDialog()">Create First Tenant</button>
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
      display: flex;
      gap: 16px;
      margin-bottom: 16px;

      .search-field { flex: 1; max-width: 400px; }
      .status-filter { min-width: 150px; }
    }

    .tenant-name-cell {
      display: flex;
      align-items: center;
      gap: 12px;

      .tenant-avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background-color: #311b92;
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        font-weight: 600;
        flex-shrink: 0;
      }

      .tenant-name { font-weight: 500; font-size: 14px; }
      .tenant-slug { font-size: 12px; color: rgba(0,0,0,0.5); font-family: monospace; }
    }

    .users-cell, .tasks-cell {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 14px;

      mat-icon { font-size: 16px; width: 16px; height: 16px; color: rgba(0,0,0,0.4); }
    }

    .empty-table {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: rgba(0,0,0,0.4);

      mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 8px; }
    }

    table { width: 100%; }
  `]
})
export class TenantListComponent implements OnInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private tenantService = inject(TenantService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  displayedColumns = ['name', 'active', 'userCount', 'taskCount', 'createdAt', 'actions'];
  dataSource = new MatTableDataSource<Tenant>([]);

  loading = false;
  error = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  searchTerm = '';
  statusFilter: boolean | string = '';

  ngOnInit(): void {
    this.loadTenants();
  }

  loadTenants(): void {
    this.loading = true;
    this.error = false;

    const active = this.statusFilter === '' ? undefined : this.statusFilter as boolean;

    this.tenantService.getTenants(this.currentPage, this.pageSize, this.searchTerm, active).subscribe({
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

  onSearch(term: string): void {
    this.currentPage = 0;
    this.loadTenants();
  }

  onStatusFilter(): void {
    this.currentPage = 0;
    this.loadTenants();
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadTenants();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(CreateTenantDialogComponent, {
      width: '560px',
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.snackBar.open(`Tenant "${result.name}" created`, 'Close', { duration: 3000 });
        this.loadTenants();
      }
    });
  }

  toggleActive(tenant: Tenant): void {
    const action = tenant.active
      ? this.tenantService.deactivateTenant(tenant.id)
      : this.tenantService.activateTenant(tenant.id);

    action.subscribe({
      next: () => {
        tenant.active = !tenant.active;
        this.snackBar.open(`Tenant ${tenant.active ? 'activated' : 'deactivated'}`, 'Close', { duration: 3000 });
      },
      error: () => {
        this.snackBar.open('Action failed', 'Close', { duration: 3000 });
      }
    });
  }
}
