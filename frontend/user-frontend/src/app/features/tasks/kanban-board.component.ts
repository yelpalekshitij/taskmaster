import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CdkDragDrop, DragDropModule, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TaskService } from '../../core/services/task.service';
import { Task, TaskStatus } from '../../core/models/task.model';
import { TaskFormDialogComponent } from './task-form-dialog.component';

interface KanbanColumn {
  id: TaskStatus;
  label: string;
  color: string;
  tasks: Task[];
}

@Component({
  selector: 'app-kanban-board',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    DragDropModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatDialogModule,
    MatTooltipModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Kanban Board</h1>
        <div class="header-actions">
          <a mat-stroked-button routerLink="/tasks">
            <mat-icon>list</mat-icon>
            List View
          </a>
          <button mat-raised-button color="primary" (click)="openCreateDialog()">
            <mat-icon>add</mat-icon>
            New Task
          </button>
        </div>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <div *ngIf="error && !loading" class="error-container">
        <mat-icon>error</mat-icon>
        Failed to load tasks.
        <button mat-button (click)="loadTasks()">Retry</button>
      </div>

      <div class="kanban-board" *ngIf="!loading && !error">
        <div
          class="kanban-column"
          *ngFor="let column of columns"
          [style.border-top]="'3px solid ' + column.color">

          <div class="kanban-column-header" [style.color]="column.color">
            {{ column.label }}
            <span class="task-count">{{ column.tasks.length }}</span>
          </div>

          <div
            class="kanban-drop-list"
            cdkDropList
            [id]="column.id"
            [cdkDropListData]="column.tasks"
            [cdkDropListConnectedTo]="connectedLists"
            (cdkDropListDropped)="onDrop($event)">

            <mat-card
              class="kanban-task-card"
              *ngFor="let task of column.tasks"
              cdkDrag
              [cdkDragData]="task">

              <div class="drag-placeholder" *cdkDragPlaceholder></div>

              <mat-card-content class="task-card-content">
                <div class="task-header">
                  <span class="task-title">{{ task.title }}</span>
                  <span class="priority-dot" [style.background-color]="getPriorityColor(task.priority)"
                        [matTooltip]="task.priority"></span>
                </div>

                <p class="task-description" *ngIf="task.description">
                  {{ task.description | slice:0:80 }}{{ task.description.length > 80 ? '...' : '' }}
                </p>

                <div class="task-footer">
                  <span class="due-date" *ngIf="task.dueDate" [class.overdue]="isOverdue(task)">
                    <mat-icon>event</mat-icon>
                    {{ task.dueDate | date:'MMM d' }}
                  </span>
                  <span class="assignee" *ngIf="task.assignee">
                    <mat-icon>person</mat-icon>
                    {{ task.assignee.firstName || task.assignee.username }}
                  </span>
                  <span class="tag-chip" *ngFor="let tag of (task.tags || []).slice(0, 2)">
                    {{ tag }}
                  </span>
                </div>
              </mat-card-content>
            </mat-card>

            <div class="empty-column" *ngIf="column.tasks.length === 0">
              <mat-icon>inbox</mat-icon>
              <span>Drop tasks here</span>
            </div>
          </div>
        </div>
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

    .kanban-board {
      display: flex;
      gap: 16px;
      overflow-x: auto;
      padding-bottom: 16px;
      min-height: calc(100vh - 200px);
    }

    .kanban-column {
      min-width: 280px;
      flex: 1;
      background-color: #f0f0f0;
      border-radius: 8px;
      padding: 12px;
    }

    .kanban-column-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-weight: 600;
      font-size: 13px;
      padding: 4px 4px 12px;
      text-transform: uppercase;
      letter-spacing: 0.5px;

      .task-count {
        background-color: rgba(0,0,0,0.12);
        border-radius: 12px;
        padding: 0 8px;
        font-size: 12px;
        color: rgba(0,0,0,0.6);
      }
    }

    .kanban-drop-list {
      min-height: 100px;
    }

    .kanban-task-card {
      margin-bottom: 8px;
      cursor: grab;

      &:active { cursor: grabbing; }

      mat-card-content { padding: 12px !important; }
    }

    .task-card-content {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .task-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 8px;

      .task-title {
        font-weight: 500;
        font-size: 14px;
        line-height: 1.3;
      }

      .priority-dot {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        flex-shrink: 0;
        margin-top: 2px;
      }
    }

    .task-description {
      margin: 0;
      font-size: 12px;
      color: rgba(0,0,0,0.6);
      line-height: 1.4;
    }

    .task-footer {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 4px;

      .due-date, .assignee {
        display: flex;
        align-items: center;
        gap: 2px;
        font-size: 11px;
        color: rgba(0,0,0,0.5);

        mat-icon { font-size: 12px; width: 12px; height: 12px; }
      }

      .overdue { color: #c62828; }

      .tag-chip {
        background-color: #e3f2fd;
        color: #1565c0;
        border-radius: 4px;
        padding: 1px 6px;
        font-size: 10px;
      }
    }

    .empty-column {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px;
      color: rgba(0,0,0,0.3);
      font-size: 12px;

      mat-icon { font-size: 32px; width: 32px; height: 32px; }
    }

    .drag-placeholder {
      min-height: 60px;
      background-color: rgba(0,0,0,0.05);
      border: 2px dashed rgba(0,0,0,0.2);
      border-radius: 4px;
    }

    .cdk-drag-preview {
      box-sizing: border-box;
      border-radius: 4px;
      box-shadow: 0 5px 5px -3px rgba(0,0,0,.2),
                  0 8px 10px 1px rgba(0,0,0,.14);
    }

    .cdk-drag-animating {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }

    .cdk-drop-list-dragging .kanban-task-card:not(.cdk-drag-placeholder) {
      transition: transform 250ms cubic-bezier(0, 0, 0.2, 1);
    }
  `]
})
export class KanbanBoardComponent implements OnInit {
  private taskService = inject(TaskService);
  private dialog = inject(MatDialog);

  loading = false;
  error = false;

  columns: KanbanColumn[] = [
    { id: 'TODO', label: 'To Do', color: '#1565c0', tasks: [] },
    { id: 'IN_PROGRESS', label: 'In Progress', color: '#e65100', tasks: [] },
    { id: 'ON_HOLD', label: 'On Hold', color: '#6a1b9a', tasks: [] },
    { id: 'DONE', label: 'Done', color: '#2e7d32', tasks: [] },
  ];

  get connectedLists(): string[] {
    return this.columns.map(c => c.id);
  }

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.error = false;

    this.taskService.getTasks({ size: 100 }).subscribe({
      next: (page) => {
        this.columns.forEach(col => col.tasks = []);
        page.content.forEach(task => {
          const col = this.columns.find(c => c.id === task.status);
          if (col) col.tasks.push(task);
        });
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  onDrop(event: CdkDragDrop<Task[]>): void {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const task = event.previousContainer.data[event.previousIndex];
      const newStatus = event.container.id as TaskStatus;

      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );

      this.taskService.updateTaskStatus({ taskId: task.id, status: newStatus }).subscribe({
        error: () => {
          // Revert on failure
          transferArrayItem(
            event.container.data,
            event.previousContainer.data,
            event.currentIndex,
            event.previousIndex
          );
        }
      });
    }
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

  getPriorityColor(priority: string): string {
    const colors: Record<string, string> = {
      LOW: '#388e3c',
      MEDIUM: '#f57c00',
      HIGH: '#d32f2f',
      CRITICAL: '#b71c1c',
    };
    return colors[priority] || '#9e9e9e';
  }

  isOverdue(task: Task): boolean {
    if (!task.dueDate || task.status === 'DONE') return false;
    return new Date(task.dueDate) < new Date();
  }
}
