import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TaskService } from '../../core/services/task.service';
import { Task, TaskStatus, TaskPriority } from '../../core/models/task.model';
import { StatusBadgeComponent } from '../../shared/components/status-badge.component';
import { TaskFormDialogComponent } from './task-form-dialog.component';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatMenuModule,
    MatTooltipModule,
    StatusBadgeComponent,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Tasks</h1>
        <div class="header-actions">
          <a mat-stroked-button routerLink="/tasks/board">
            <mat-icon>view_kanban</mat-icon>
            Board View
          </a>
          <button mat-raised-button color="primary" (click)="openCreateDialog()">
            <mat-icon>add</mat-icon>
            New Task
          </button>
        </div>
      </div>

      <!-- Filters -->
      <div class="filters-bar">
        <div class="status-chips">
          <span class="filter-label">Status:</span>
          <mat-chip-listbox [(ngModel)]="selectedStatus" (ngModelChange)="onStatusFilter($event)">
            <mat-chip-option value="">All</mat-chip-option>
            <mat-chip-option value="TODO">To Do</mat-chip-option>
            <mat-chip-option value="IN_PROGRESS">In Progress</mat-chip-option>
            <mat-chip-option value="ON_HOLD">On Hold</mat-chip-option>
            <mat-chip-option value="DONE">Done</mat-chip-option>
          </mat-chip-listbox>
        </div>

        <mat-form-field appearance="outline" class="priority-filter">
          <mat-label>Priority</mat-label>
          <mat-select [(ngModel)]="selectedPriority" (ngModelChange)="onPriorityFilter($event)">
            <mat-option value="">All</mat-option>
            <mat-option value="LOW">Low</mat-option>
            <mat-option value="MEDIUM">Medium</mat-option>
            <mat-option value="HIGH">High</mat-option>
            <mat-option value="CRITICAL">Critical</mat-option>
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
        Failed to load tasks.
        <button mat-button (click)="loadTasks()">Retry</button>
      </div>

      <!-- Table -->
      <div class="table-container mat-elevation-z2" *ngIf="!loading && !error">
        <table mat-table [dataSource]="dataSource" matSort>

          <!-- Title Column -->
          <ng-container matColumnDef="title">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Title</th>
            <td mat-cell *matCellDef="let task">
              <div class="task-title-cell">
                <span class="task-title">{{ task.title }}</span>
                <span class="task-description" *ngIf="task.description">{{ task.description | slice:0:60 }}{{ task.description.length > 60 ? '...' : '' }}</span>
              </div>
            </td>
          </ng-container>

          <!-- Status Column -->
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
            <td mat-cell *matCellDef="let task">
              <app-status-badge [status]="task.status"></app-status-badge>
            </td>
          </ng-container>

          <!-- Priority Column -->
          <ng-container matColumnDef="priority">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Priority</th>
            <td mat-cell *matCellDef="let task">
              <span [class]="'priority-' + task.priority.toLowerCase()">
                <mat-icon *ngIf="task.priority === 'HIGH' || task.priority === 'CRITICAL'">flag</mat-icon>
                {{ task.priority }}
              </span>
            </td>
          </ng-container>

          <!-- Assignee Column -->
          <ng-container matColumnDef="assignee">
            <th mat-header-cell *matHeaderCellDef>Assignee</th>
            <td mat-cell *matCellDef="let task">
              <span *ngIf="task.assignedTo">{{ task.assignedTo }}</span>
              <span *ngIf="!task.assignedTo" class="unassigned">Unassigned</span>
            </td>
          </ng-container>

          <!-- Due Date Column -->
          <ng-container matColumnDef="dueDate">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Due Date</th>
            <td mat-cell *matCellDef="let task">
              <span *ngIf="task.dueDate" [class.overdue-text]="isOverdue(task)">
                {{ task.dueDate | date:'MMM d, y' }}
              </span>
              <span *ngIf="!task.dueDate" class="no-date">—</span>
            </td>
          </ng-container>

          <!-- Actions Column -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let task">
              <button mat-icon-button [matMenuTriggerFor]="actionsMenu" matTooltip="Actions">
                <mat-icon>more_vert</mat-icon>
              </button>
              <mat-menu #actionsMenu="matMenu">
                <button mat-menu-item (click)="updateStatus(task, 'IN_PROGRESS')" *ngIf="task.status === 'TODO'">
                  <mat-icon>play_arrow</mat-icon> Start
                </button>
                <button mat-menu-item (click)="updateStatus(task, 'DONE')" *ngIf="task.status !== 'DONE'">
                  <mat-icon>check</mat-icon> Mark Done
                </button>
                <button mat-menu-item (click)="updateStatus(task, 'ON_HOLD')" *ngIf="task.status === 'IN_PROGRESS'">
                  <mat-icon>pause</mat-icon> Put On Hold
                </button>
                <button mat-menu-item (click)="updateStatus(task, 'TODO')" *ngIf="task.status !== 'TODO'">
                  <mat-icon>undo</mat-icon> Reset to Todo
                </button>
              </mat-menu>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;" class="task-row"></tr>
        </table>

        <div *ngIf="dataSource.data.length === 0" class="empty-table">
          <mat-icon>inbox</mat-icon>
          <p>No tasks found</p>
          <button mat-raised-button color="primary" (click)="openCreateDialog()">Create Task</button>
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
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      h1 { margin: 0; }
      .header-actions { display: flex; gap: 12px; }
    }

    .filters-bar {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;
      flex-wrap: wrap;

      .filter-label {
        font-size: 14px;
        color: rgba(0,0,0,0.6);
        margin-right: 8px;
      }

      .status-chips {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .priority-filter {
        min-width: 150px;
      }
    }

    .table-container {
      border-radius: 8px;
      overflow: hidden;
    }

    table {
      width: 100%;
    }

    .task-title-cell {
      display: flex;
      flex-direction: column;

      .task-title {
        font-weight: 500;
        font-size: 14px;
      }

      .task-description {
        font-size: 12px;
        color: rgba(0,0,0,0.5);
      }
    }

    .task-row:hover {
      background-color: rgba(0,0,0,0.02);
    }

    .priority-low { color: #388e3c; }
    .priority-medium { color: #f57c00; }
    .priority-high { color: #d32f2f; }
    .priority-critical { color: #b71c1c; font-weight: 700; }

    .priority-high, .priority-critical {
      display: flex;
      align-items: center;
      gap: 2px;
      mat-icon { font-size: 16px; width: 16px; height: 16px; }
    }

    .unassigned { color: rgba(0,0,0,0.4); font-style: italic; }
    .no-date { color: rgba(0,0,0,0.4); }
    .overdue-text { color: #c62828; font-weight: 500; }

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
export class TaskListComponent implements OnInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  private taskService = inject(TaskService);
  private dialog = inject(MatDialog);

  displayedColumns = ['title', 'status', 'priority', 'assignee', 'dueDate', 'actions'];
  dataSource = new MatTableDataSource<Task>([]);

  loading = false;
  error = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  selectedStatus: string = '';
  selectedPriority: string = '';

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.error = false;

    this.taskService.getTasks({
      status: this.selectedStatus as TaskStatus || undefined,
      priority: this.selectedPriority as TaskPriority || undefined,
      page: this.currentPage,
      size: this.pageSize,
    }).subscribe({
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

  onStatusFilter(status: string): void {
    this.selectedStatus = status;
    this.currentPage = 0;
    this.loadTasks();
  }

  onPriorityFilter(priority: string): void {
    this.selectedPriority = priority;
    this.currentPage = 0;
    this.loadTasks();
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadTasks();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(TaskFormDialogComponent, {
      data: {},
      width: '560px',
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.loadTasks();
      }
    });
  }

  updateStatus(task: Task, status: TaskStatus): void {
    this.taskService.updateTaskStatus({ taskId: task.id, status }).subscribe({
      next: () => this.loadTasks(),
      error: () => {}
    });
  }

  isOverdue(task: Task): boolean {
    if (!task.dueDate || task.status === 'DONE') return false;
    return new Date(task.dueDate) < new Date();
  }
}
