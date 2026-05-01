import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { TaskService } from '../../core/services/task.service';
import { Task } from '../../core/models/task.model';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { Observable, combineLatest, of } from 'rxjs';
import { map, catchError, startWith } from 'rxjs/operators';

interface DashboardStats {
  total: number;
  inProgress: number;
  doneToday: number;
  overdue: number;
}

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
    MatChipsModule,
    MatDividerModule,
    StatusBadgeComponent,
  ],
  template: `
    <div class="page-container">
      <div class="dashboard-header">
        <h1>Dashboard</h1>
        <span class="welcome-text" *ngIf="userName">Welcome back, {{ userName }}!</span>
      </div>

      <!-- Stats Cards -->
      <div class="stats-grid">
        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-label">Total Tasks</p>
                <p class="stat-value">{{ stats.total }}</p>
              </div>
              <mat-icon class="stat-icon icon-blue">assignment</mat-icon>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-label">In Progress</p>
                <p class="stat-value stat-orange">{{ stats.inProgress }}</p>
              </div>
              <mat-icon class="stat-icon icon-orange">autorenew</mat-icon>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-label">Done Today</p>
                <p class="stat-value stat-green">{{ stats.doneToday }}</p>
              </div>
              <mat-icon class="stat-icon icon-green">check_circle</mat-icon>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card">
          <mat-card-content>
            <div class="stat-content">
              <div class="stat-info">
                <p class="stat-label">Overdue</p>
                <p class="stat-value stat-red">{{ stats.overdue }}</p>
              </div>
              <mat-icon class="stat-icon icon-red">warning</mat-icon>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <!-- Recent Tasks -->
      <mat-card class="recent-tasks-card">
        <mat-card-header>
          <mat-card-title>My Recent Tasks</mat-card-title>
          <span class="spacer"></span>
          <a mat-button color="primary" routerLink="/tasks">View All</a>
        </mat-card-header>
        <mat-card-content>
          <div *ngIf="loading" class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
          </div>

          <div *ngIf="error" class="error-container">
            <mat-icon>error</mat-icon>
            Failed to load tasks. <button mat-button (click)="loadTasks()">Retry</button>
          </div>

          <div *ngIf="!loading && !error">
            <div *ngIf="recentTasks.length === 0" class="empty-state">
              <mat-icon>inbox</mat-icon>
              <p>No tasks yet. <a routerLink="/tasks">Create your first task</a></p>
            </div>

            <div class="task-list" *ngIf="recentTasks.length > 0">
              <div class="task-item" *ngFor="let task of recentTasks">
                <div class="task-main">
                  <span class="task-title">{{ task.title }}</span>
                  <app-status-badge [status]="task.status"></app-status-badge>
                </div>
                <div class="task-meta">
                  <span class="priority-indicator" [class]="'priority-' + task.priority.toLowerCase()">
                    <mat-icon>flag</mat-icon>
                    {{ task.priority }}
                  </span>
                  <span *ngIf="task.dueDate" class="due-date" [class.overdue]="isOverdue(task.dueDate)">
                    <mat-icon>schedule</mat-icon>
                    {{ task.dueDate | date:'MMM d' }}
                  </span>
                </div>
                <mat-divider></mat-divider>
              </div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Quick Actions -->
      <mat-card class="quick-actions-card">
        <mat-card-header>
          <mat-card-title>Quick Actions</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="quick-actions">
            <a mat-raised-button color="primary" routerLink="/tasks">
              <mat-icon>add</mat-icon>
              New Task
            </a>
            <a mat-stroked-button routerLink="/tasks/board">
              <mat-icon>view_kanban</mat-icon>
              Kanban Board
            </a>
            <a mat-stroked-button routerLink="/notifications">
              <mat-icon>notifications</mat-icon>
              Notifications
            </a>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .dashboard-header {
      display: flex;
      align-items: baseline;
      gap: 16px;
      margin-bottom: 24px;

      h1 { margin: 0; }
      .welcome-text { color: rgba(0,0,0,0.6); }
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
        margin: 4px 0 0;
        font-size: 32px;
        font-weight: 600;
      }

      .stat-orange { color: #e65100; }
      .stat-green { color: #2e7d32; }
      .stat-red { color: #c62828; }

      .stat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        opacity: 0.2;
      }

      .icon-blue { color: #1565c0; }
      .icon-orange { color: #e65100; }
      .icon-green { color: #2e7d32; }
      .icon-red { color: #c62828; }
    }

    .recent-tasks-card, .quick-actions-card {
      margin-bottom: 24px;

      mat-card-header {
        display: flex;
        align-items: center;
        margin-bottom: 16px;
      }
    }

    .task-item {
      padding: 8px 0;

      .task-main {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 4px;
      }

      .task-title {
        font-weight: 500;
        font-size: 14px;
      }

      .task-meta {
        display: flex;
        gap: 16px;
        font-size: 12px;
        color: rgba(0,0,0,0.6);
        margin-bottom: 8px;

        span {
          display: flex;
          align-items: center;
          gap: 2px;

          mat-icon { font-size: 14px; width: 14px; height: 14px; }
        }
      }

      .priority-low { color: #388e3c; }
      .priority-medium { color: #f57c00; }
      .priority-high { color: #d32f2f; }
      .priority-critical { color: #b71c1c; font-weight: 700; }

      .overdue { color: #c62828; }
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 32px;
      color: rgba(0,0,0,0.4);

      mat-icon { font-size: 48px; width: 48px; height: 48px; }
    }

    .quick-actions {
      display: flex;
      gap: 12px;
      flex-wrap: wrap;
    }

    .spacer { flex: 1 1 auto; }
  `]
})
export class DashboardComponent implements OnInit {
  private taskService = inject(TaskService);
  private oidcService = inject(OidcSecurityService);

  recentTasks: Task[] = [];
  loading = false;
  error = false;
  userName = '';

  stats: DashboardStats = {
    total: 0,
    inProgress: 0,
    doneToday: 0,
    overdue: 0,
  };

  ngOnInit(): void {
    this.oidcService.getUserData().subscribe(userData => {
      this.userName = userData?.given_name || userData?.preferred_username || '';
    });
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.error = false;

    this.taskService.getMyTasks(undefined, 0, 10).subscribe({
      next: (page) => {
        this.recentTasks = page.content.slice(0, 5);
        this.computeStats(page.content);
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  private computeStats(tasks: Task[]): void {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    this.stats = {
      total: tasks.length,
      inProgress: tasks.filter(t => t.status === 'IN_PROGRESS').length,
      doneToday: tasks.filter(t => {
        if (t.status !== 'DONE') return false;
        const updated = new Date(t.updatedAt);
        return updated >= today && updated < tomorrow;
      }).length,
      overdue: tasks.filter(t => {
        if (!t.dueDate || t.status === 'DONE') return false;
        return new Date(t.dueDate) < today;
      }).length,
    };
  }

  isOverdue(dueDate: string): boolean {
    return new Date(dueDate) < new Date();
  }
}
