import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { NotificationService } from '../../core/services/notification.service';
import { Notification, NotificationType } from '../../core/models/notification.model';

@Component({
  selector: 'app-notification-list',
  standalone: true,
  imports: [
    CommonModule,
    MatListModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatPaginatorModule,
    MatDividerModule,
    MatTooltipModule,
    MatBadgeModule,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>
          Notifications
          <span class="unread-badge" *ngIf="unreadCount > 0">{{ unreadCount }} unread</span>
        </h1>
        <button mat-stroked-button (click)="markAllAsRead()" [disabled]="unreadCount === 0">
          <mat-icon>done_all</mat-icon>
          Mark All Read
        </button>
      </div>

      <div *ngIf="loading" class="loading-container">
        <mat-spinner></mat-spinner>
      </div>

      <div *ngIf="error && !loading" class="error-container">
        <mat-icon>error</mat-icon>
        Failed to load notifications.
        <button mat-button (click)="loadNotifications()">Retry</button>
      </div>

      <mat-card *ngIf="!loading && !error">
        <div *ngIf="notifications.length === 0" class="empty-state">
          <mat-icon>notifications_none</mat-icon>
          <p>No notifications yet</p>
        </div>

        <mat-list *ngIf="notifications.length > 0">
          <mat-list-item
            *ngFor="let notification of notifications; let last = last"
            class="notification-item"
            [class.unread]="!notification.read">

            <div class="notification-content">
              <div class="notification-icon-wrapper">
                <mat-icon [style.color]="getNotificationColor(notification.type)">
                  {{ getNotificationIcon(notification.type) }}
                </mat-icon>
                <span class="unread-dot" *ngIf="!notification.read"></span>
              </div>

              <div class="notification-body">
                <div class="notification-title">{{ notification.title }}</div>
                <div class="notification-message">{{ notification.message }}</div>
                <div class="notification-time">{{ notification.createdAt | date:'MMM d, h:mm a' }}</div>
              </div>

              <div class="notification-actions">
                <button
                  mat-icon-button
                  *ngIf="!notification.read"
                  (click)="markAsRead(notification)"
                  matTooltip="Mark as read">
                  <mat-icon>check</mat-icon>
                </button>
                <button
                  mat-icon-button
                  (click)="deleteNotification(notification)"
                  matTooltip="Delete">
                  <mat-icon>close</mat-icon>
                </button>
              </div>
            </div>

            <mat-divider *ngIf="!last"></mat-divider>
          </mat-list-item>
        </mat-list>

        <mat-paginator
          *ngIf="totalElements > pageSize"
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageSizeOptions]="[20, 50]"
          (page)="onPage($event)"
          showFirstLastButtons>
        </mat-paginator>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;

      h1 {
        margin: 0;
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .unread-badge {
        background-color: #e53935;
        color: white;
        border-radius: 12px;
        padding: 2px 10px;
        font-size: 13px;
        font-weight: 500;
      }
    }

    .notification-item {
      height: auto !important;
      padding: 12px 16px !important;

      &.unread {
        background-color: #f3f8ff;
      }
    }

    .notification-content {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      width: 100%;
    }

    .notification-icon-wrapper {
      position: relative;
      flex-shrink: 0;

      mat-icon { margin-top: 2px; }

      .unread-dot {
        position: absolute;
        top: -2px;
        right: -2px;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background-color: #1565c0;
      }
    }

    .notification-body {
      flex: 1;

      .notification-title {
        font-weight: 500;
        font-size: 14px;
        margin-bottom: 2px;
      }

      .notification-message {
        font-size: 13px;
        color: rgba(0,0,0,0.6);
        margin-bottom: 4px;
      }

      .notification-time {
        font-size: 11px;
        color: rgba(0,0,0,0.4);
      }
    }

    .notification-actions {
      display: flex;
      gap: 4px;
      flex-shrink: 0;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 64px;
      color: rgba(0,0,0,0.4);

      mat-icon { font-size: 64px; width: 64px; height: 64px; }
    }
  `]
})
export class NotificationListComponent implements OnInit {
  private notificationService = inject(NotificationService);

  notifications: Notification[] = [];
  loading = false;
  error = false;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  unreadCount = 0;

  ngOnInit(): void {
    this.loadNotifications();
    this.loadUnreadCount();
  }

  loadNotifications(): void {
    this.loading = true;
    this.error = false;

    this.notificationService.getNotifications(this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.notifications = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  loadUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe({
      next: (count) => this.unreadCount = count,
      error: () => {}
    });
  }

  markAsRead(notification: Notification): void {
    this.notificationService.markAsRead(notification.id).subscribe({
      next: () => {
        notification.read = true;
        this.unreadCount = Math.max(0, this.unreadCount - 1);
      }
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
        this.unreadCount = 0;
      }
    });
  }

  deleteNotification(notification: Notification): void {
    this.notificationService.deleteNotification(notification.id).subscribe({
      next: () => {
        this.notifications = this.notifications.filter(n => n.id !== notification.id);
        if (!notification.read) {
          this.unreadCount = Math.max(0, this.unreadCount - 1);
        }
        this.totalElements--;
      }
    });
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadNotifications();
  }

  getNotificationIcon(type: NotificationType): string {
    const icons: Record<NotificationType, string> = {
      TASK_ASSIGNED: 'assignment_ind',
      TASK_UPDATED: 'update',
      TASK_COMMENTED: 'comment',
      TASK_DUE_SOON: 'alarm',
      TASK_OVERDUE: 'warning',
      MENTION: 'alternate_email',
    };
    return icons[type] || 'notifications';
  }

  getNotificationColor(type: NotificationType): string {
    const colors: Record<NotificationType, string> = {
      TASK_ASSIGNED: '#1565c0',
      TASK_UPDATED: '#0277bd',
      TASK_COMMENTED: '#6a1b9a',
      TASK_DUE_SOON: '#e65100',
      TASK_OVERDUE: '#c62828',
      MENTION: '#2e7d32',
    };
    return colors[type] || '#9e9e9e';
  }
}
