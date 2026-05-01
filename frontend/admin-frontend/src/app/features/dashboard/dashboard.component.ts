import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { ReportsService } from '../../core/services/reports.service';
import { GlobalStats, TenantStats } from '../../core/models/admin.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTableModule,
    MatChipsModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1>Admin Dashboard</h1>
          <p class="subtitle" *ngIf="isMasterAdmin">Viewing all tenants and system-wide statistics</p>
          <p class="subtitle" *ngIf="!isMasterAdmin">Viewing your tenant statistics</p>
        </div>
        <div class="role-badge" [class.master]="isMasterAdmin">
          <mat-icon>{{ isMasterAdmin ? 'admin_panel_settings' : 'manage_accounts' }}</mat-icon>
          {{ isMasterAdmin ? 'Master Admin' : 'Tenant Admin' }}
        </div>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <!-- Master Admin View -->
      <ng-container *ngIf="!loading && isMasterAdmin && globalStats">
        <!-- Global Stats -->
        <div class="stats-grid">
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-content">
                <div>
                  <p class="stat-label">Total Tenants</p>
                  <p class="stat-value">{{ globalStats.totalTenants }}</p>
                  <p class="stat-sub">{{ globalStats.activeTenants }} active</p>
                </div>
                <mat-icon class="stat-icon">business</mat-icon>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-content">
                <div>
                  <p class="stat-label">Total Users</p>
                  <p class="stat-value">{{ globalStats.totalUsers }}</p>
                  <p class="stat-sub">{{ globalStats.activeUsers }} active</p>
                </div>
                <mat-icon class="stat-icon">people</mat-icon>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-content">
                <div>
                  <p class="stat-label">Total Tasks</p>
                  <p class="stat-value">{{ globalStats.totalTasks }}</p>
                  <p class="stat-sub">{{ globalStats.completedTasks }} completed</p>
                </div>
                <mat-icon class="stat-icon">task_alt</mat-icon>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-content">
                <div>
                  <p class="stat-label">Completion Rate</p>
                  <p class="stat-value">{{ getCompletionRate(globalStats) | number:'1.0-0' }}%</p>
                  <mat-progress-bar
                    mode="determinate"
                    [value]="getCompletionRate(globalStats)"
                    color="primary">
                  </mat-progress-bar>
                </div>
                <mat-icon class="stat-icon">trending_up</mat-icon>
              </div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Tenant Overview Table -->
        <mat-card class="table-card" *ngIf="tenantStats.length > 0">
          <mat-card-header>
            <mat-card-title>Tenant Overview</mat-card-title>
            <span class="spacer"></span>
            <a mat-button color="primary" routerLink="/tenants">Manage Tenants</a>
          </mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="tenantStats" class="full-width">
              <ng-container matColumnDef="name">
                <th mat-header-cell *matHeaderCellDef>Tenant</th>
                <td mat-cell *matCellDef="let t">{{ t.tenantName }}</td>
              </ng-container>
              <ng-container matColumnDef="users">
                <th mat-header-cell *matHeaderCellDef>Users</th>
                <td mat-cell *matCellDef="let t">{{ t.activeUsers }}/{{ t.totalUsers }}</td>
              </ng-container>
              <ng-container matColumnDef="tasks">
                <th mat-header-cell *matHeaderCellDef>Tasks</th>
                <td mat-cell *matCellDef="let t">{{ t.totalTasks }}</td>
              </ng-container>
              <ng-container matColumnDef="completion">
                <th mat-header-cell *matHeaderCellDef>Completion</th>
                <td mat-cell *matCellDef="let t">
                  <div class="completion-cell">
                    <mat-progress-bar mode="determinate" [value]="t.taskCompletionRate" color="primary"></mat-progress-bar>
                    <span>{{ t.taskCompletionRate | number:'1.0-0' }}%</span>
                  </div>
                </td>
              </ng-container>
              <ng-container matColumnDef="overdue">
                <th mat-header-cell *matHeaderCellDef>Overdue</th>
                <td mat-cell *matCellDef="let t">
                  <span [class.overdue-count]="t.overdueTasks > 0">{{ t.overdueTasks }}</span>
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="['name','users','tasks','completion','overdue']"></tr>
              <tr mat-row *matRowDef="let r; columns: ['name','users','tasks','completion','overdue']"></tr>
            </table>
          </mat-card-content>
        </mat-card>
      </ng-container>

      <!-- Tenant Admin View -->
      <ng-container *ngIf="!loading && !isMasterAdmin && tenantStats.length > 0">
        <div class="stats-grid">
          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-content">
                <div>
                  <p class="stat-label">Total Users</p>
                  <p class="stat-value">{{ tenantStats[0].totalUsers }}</p>
                  <p class="stat-sub">{{ tenantStats[0].activeUsers }} active</p>
                </div>
                <mat-icon class="stat-icon">people</mat-icon>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-content">
                <div>
                  <p class="stat-label">Total Tasks</p>
                  <p class="stat-value">{{ tenantStats[0].totalTasks }}</p>
                </div>
                <mat-icon class="stat-icon">task_alt</mat-icon>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-content">
                <div>
                  <p class="stat-label">In Progress</p>
                  <p class="stat-value stat-orange">{{ tenantStats[0].inProgressTasks }}</p>
                </div>
                <mat-icon class="stat-icon icon-orange">autorenew</mat-icon>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card class="stat-card">
            <mat-card-content>
              <div class="stat-content">
                <div>
                  <p class="stat-label">Overdue</p>
                  <p class="stat-value stat-red">{{ tenantStats[0].overdueTasks }}</p>
                </div>
                <mat-icon class="stat-icon icon-red">warning</mat-icon>
              </div>
            </mat-card-content>
          </mat-card>
        </div>
      </ng-container>
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

    .role-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      background-color: #e8eaf6;
      color: #311b92;
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;

      &.master {
        background-color: #311b92;
        color: white;
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .stat-card {
      .stat-content {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      .stat-label {
        margin: 0;
        font-size: 13px;
        color: rgba(0,0,0,0.6);
      }

      .stat-value {
        margin: 4px 0;
        font-size: 32px;
        font-weight: 600;
      }

      .stat-sub {
        margin: 0;
        font-size: 12px;
        color: rgba(0,0,0,0.5);
      }

      .stat-orange { color: #e65100; }
      .stat-red { color: #c62828; }

      .stat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        opacity: 0.15;
        color: #311b92;
      }

      .icon-orange { color: #e65100; }
      .icon-red { color: #c62828; }
    }

    .table-card {
      margin-bottom: 24px;

      mat-card-header {
        display: flex;
        align-items: center;
        margin-bottom: 16px;
      }
    }

    .full-width { width: 100%; }

    .completion-cell {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 120px;

      mat-progress-bar { flex: 1; }
      span { font-size: 12px; white-space: nowrap; }
    }

    .overdue-count { color: #c62828; font-weight: 600; }

    .spacer { flex: 1 1 auto; }
  `]
})
export class DashboardComponent implements OnInit {
  private oidcService = inject(OidcSecurityService);
  private reportsService = inject(ReportsService);

  loading = false;
  isMasterAdmin = false;
  globalStats: GlobalStats | null = null;
  tenantStats: TenantStats[] = [];

  ngOnInit(): void {
    this.oidcService.getUserData().subscribe(userData => {
      const roles: string[] = userData?.realm_access?.roles || [];
      this.isMasterAdmin = roles.includes('MASTER_ADMIN');
      this.loadStats();
    });
  }

  loadStats(): void {
    this.loading = true;

    if (this.isMasterAdmin) {
      this.reportsService.getGlobalStats().subscribe({
        next: (stats) => {
          this.globalStats = stats;
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
      this.reportsService.getAllTenantStats().subscribe({
        next: (stats) => this.tenantStats = stats,
        error: () => {}
      });
    } else {
      this.reportsService.getAllTenantStats().subscribe({
        next: (stats) => {
          this.tenantStats = stats;
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
    }
  }

  getCompletionRate(stats: GlobalStats): number {
    if (!stats.totalTasks) return 0;
    return (stats.completedTasks / stats.totalTasks) * 100;
  }
}
