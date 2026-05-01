import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { ReportsService } from '../../core/services/reports.service';
import { GlobalStats, TenantStats } from '../../core/models/admin.model';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatDividerModule,
    MatSelectModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Reports & Analytics</h1>
        <button mat-stroked-button (click)="loadData()">
          <mat-icon>refresh</mat-icon>
          Refresh
        </button>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <ng-container *ngIf="!loading">
        <!-- Global Stats (Master Admin only) -->
        <ng-container *ngIf="isMasterAdmin && globalStats">
          <h2 class="section-title">System Overview</h2>

          <div class="stats-row">
            <mat-card class="metric-card">
              <mat-card-content>
                <div class="metric">
                  <mat-icon>business</mat-icon>
                  <div>
                    <div class="metric-value">{{ globalStats.totalTenants }}</div>
                    <div class="metric-label">Total Tenants</div>
                  </div>
                </div>
                <mat-progress-bar
                  mode="determinate"
                  [value]="(globalStats.activeTenants / globalStats.totalTenants) * 100 || 0"
                  color="primary">
                </mat-progress-bar>
                <div class="progress-label">{{ globalStats.activeTenants }} active ({{ getPercent(globalStats.activeTenants, globalStats.totalTenants) }}%)</div>
              </mat-card-content>
            </mat-card>

            <mat-card class="metric-card">
              <mat-card-content>
                <div class="metric">
                  <mat-icon>people</mat-icon>
                  <div>
                    <div class="metric-value">{{ globalStats.totalUsers }}</div>
                    <div class="metric-label">Total Users</div>
                  </div>
                </div>
                <mat-progress-bar
                  mode="determinate"
                  [value]="(globalStats.activeUsers / globalStats.totalUsers) * 100 || 0"
                  color="accent">
                </mat-progress-bar>
                <div class="progress-label">{{ globalStats.activeUsers }} active ({{ getPercent(globalStats.activeUsers, globalStats.totalUsers) }}%)</div>
              </mat-card-content>
            </mat-card>

            <mat-card class="metric-card">
              <mat-card-content>
                <div class="metric">
                  <mat-icon>task_alt</mat-icon>
                  <div>
                    <div class="metric-value">{{ globalStats.totalTasks }}</div>
                    <div class="metric-label">Total Tasks</div>
                  </div>
                </div>
                <mat-progress-bar
                  mode="determinate"
                  [value]="(globalStats.completedTasks / globalStats.totalTasks) * 100 || 0"
                  color="primary">
                </mat-progress-bar>
                <div class="progress-label">{{ globalStats.completedTasks }} completed ({{ getPercent(globalStats.completedTasks, globalStats.totalTasks) }}%)</div>
              </mat-card-content>
            </mat-card>
          </div>
        </ng-container>

        <!-- Tenant Stats Table -->
        <h2 class="section-title">{{ isMasterAdmin ? 'Per-Tenant Breakdown' : 'Your Tenant Statistics' }}</h2>

        <mat-card class="table-card" *ngIf="tenantStats.length > 0">
          <mat-card-content>
            <table mat-table [dataSource]="tenantStats" class="full-width">

              <ng-container matColumnDef="tenant" *ngIf="isMasterAdmin">
                <th mat-header-cell *matHeaderCellDef>Tenant</th>
                <td mat-cell *matCellDef="let t">
                  <strong>{{ t.tenantName }}</strong>
                </td>
              </ng-container>

              <ng-container matColumnDef="users">
                <th mat-header-cell *matHeaderCellDef>Users</th>
                <td mat-cell *matCellDef="let t">
                  <div class="stat-cell">
                    <span class="big-num">{{ t.totalUsers }}</span>
                    <span class="sub-num">{{ t.activeUsers }} active</span>
                  </div>
                </td>
              </ng-container>

              <ng-container matColumnDef="totalTasks">
                <th mat-header-cell *matHeaderCellDef>Total Tasks</th>
                <td mat-cell *matCellDef="let t">{{ t.totalTasks }}</td>
              </ng-container>

              <ng-container matColumnDef="completed">
                <th mat-header-cell *matHeaderCellDef>Completed</th>
                <td mat-cell *matCellDef="let t">
                  <span class="stat-green">{{ t.completedTasks }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="inProgress">
                <th mat-header-cell *matHeaderCellDef>In Progress</th>
                <td mat-cell *matCellDef="let t">
                  <span class="stat-orange">{{ t.inProgressTasks }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="overdue">
                <th mat-header-cell *matHeaderCellDef>Overdue</th>
                <td mat-cell *matCellDef="let t">
                  <span [class.stat-red]="t.overdueTasks > 0">{{ t.overdueTasks }}</span>
                </td>
              </ng-container>

              <ng-container matColumnDef="completion">
                <th mat-header-cell *matHeaderCellDef>Completion Rate</th>
                <td mat-cell *matCellDef="let t">
                  <div class="completion-bar">
                    <mat-progress-bar mode="determinate" [value]="t.taskCompletionRate" color="primary"></mat-progress-bar>
                    <span class="completion-pct">{{ t.taskCompletionRate | number:'1.0-0' }}%</span>
                  </div>
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="getColumns()"></tr>
              <tr mat-row *matRowDef="let row; columns: getColumns()"></tr>
            </table>
          </mat-card-content>
        </mat-card>

        <div *ngIf="tenantStats.length === 0" class="empty-state">
          <mat-icon>bar_chart</mat-icon>
          <p>No statistics available</p>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 24px;

      h1 { margin: 0; }
    }

    .section-title {
      font-size: 16px;
      font-weight: 600;
      margin: 24px 0 16px;
      color: rgba(0,0,0,0.7);
    }

    .stats-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 16px;
      margin-bottom: 16px;
    }

    .metric-card {
      .metric {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 12px;

        mat-icon {
          font-size: 40px;
          width: 40px;
          height: 40px;
          color: #311b92;
          opacity: 0.7;
        }

        .metric-value {
          font-size: 36px;
          font-weight: 600;
          line-height: 1;
        }

        .metric-label {
          font-size: 13px;
          color: rgba(0,0,0,0.6);
          margin-top: 2px;
        }
      }

      .progress-label {
        font-size: 12px;
        color: rgba(0,0,0,0.5);
        margin-top: 4px;
      }
    }

    .table-card {
      margin-bottom: 24px;
    }

    .full-width { width: 100%; }

    .stat-cell {
      display: flex;
      flex-direction: column;

      .big-num { font-weight: 500; }
      .sub-num { font-size: 11px; color: rgba(0,0,0,0.5); }
    }

    .stat-green { color: #2e7d32; font-weight: 500; }
    .stat-orange { color: #e65100; font-weight: 500; }
    .stat-red { color: #c62828; font-weight: 500; }

    .completion-bar {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 140px;

      mat-progress-bar { flex: 1; }
      .completion-pct { font-size: 12px; white-space: nowrap; }
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 64px;
      color: rgba(0,0,0,0.4);

      mat-icon { font-size: 64px; width: 64px; height: 64px; margin-bottom: 8px; }
    }
  `]
})
export class ReportsComponent implements OnInit {
  private reportsService = inject(ReportsService);
  private oidcService = inject(OidcSecurityService);

  loading = false;
  isMasterAdmin = false;
  globalStats: GlobalStats | null = null;
  tenantStats: TenantStats[] = [];

  ngOnInit(): void {
    this.oidcService.getUserData().subscribe(userData => {
      const roles: string[] = userData?.realm_access?.roles || [];
      this.isMasterAdmin = roles.includes('MASTER_ADMIN');
      this.loadData();
    });
  }

  loadData(): void {
    this.loading = true;

    if (this.isMasterAdmin) {
      this.reportsService.getGlobalStats().subscribe({
        next: (stats) => {
          this.globalStats = stats;
        },
        error: () => {}
      });
    }

    this.reportsService.getAllTenantStats().subscribe({
      next: (stats) => {
        this.tenantStats = stats;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  getColumns(): string[] {
    const cols = ['users', 'totalTasks', 'completed', 'inProgress', 'overdue', 'completion'];
    if (this.isMasterAdmin) return ['tenant', ...cols];
    return cols;
  }

  getPercent(part: number, total: number): number {
    if (!total) return 0;
    return Math.round((part / total) * 100);
  }
}
