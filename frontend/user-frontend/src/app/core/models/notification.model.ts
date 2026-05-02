export type NotificationType = 'TASK_ASSIGNED' | 'TASK_UPDATED' | 'TASK_COMMENTED' | 'TASK_DUE_SOON' | 'TASK_OVERDUE' | 'MENTION';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  referenceId?: string;
}

export interface NotificationPage {
  content: Notification[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export interface NotificationPreferences {
  emailEnabled: boolean;
  pushEnabled: boolean;
  taskAssigned: boolean;
  taskUpdated: boolean;
  taskCommented: boolean;
  taskDueSoon: boolean;
  taskOverdue: boolean;
}
