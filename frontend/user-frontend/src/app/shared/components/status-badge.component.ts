import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskStatus } from '../../core/models/task.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="status-badge" [ngClass]="badgeClass">
      {{ label }}
    </span>
  `,
  styles: [`
    .status-badge {
      display: inline-flex;
      align-items: center;
      padding: 2px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      white-space: nowrap;
    }

    .badge-todo {
      background-color: #e3f2fd;
      color: #1565c0;
    }

    .badge-in-progress {
      background-color: #fff3e0;
      color: #e65100;
    }

    .badge-on-hold {
      background-color: #f3e5f5;
      color: #6a1b9a;
    }

    .badge-done {
      background-color: #e8f5e9;
      color: #2e7d32;
    }
  `]
})
export class StatusBadgeComponent {
  @Input() status: TaskStatus = 'TODO';

  get label(): string {
    const labels: Record<TaskStatus, string> = {
      'TODO': 'To Do',
      'IN_PROGRESS': 'In Progress',
      'ON_HOLD': 'On Hold',
      'DONE': 'Done',
      'SCHEDULED': 'Scheduled',
    };
    return labels[this.status] || this.status;
  }

  get badgeClass(): string {
    const classes: Record<TaskStatus, string> = {
      'TODO': 'badge-todo',
      'IN_PROGRESS': 'badge-in-progress',
      'ON_HOLD': 'badge-on-hold',
      'DONE': 'badge-done',
      'SCHEDULED': 'badge-todo',
    };
    return classes[this.status] || '';
  }
}
