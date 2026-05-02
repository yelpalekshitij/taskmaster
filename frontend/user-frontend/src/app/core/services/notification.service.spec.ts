import { TestBed } from '@angular/core/testing';
import { ApolloTestingModule, ApolloTestingController } from 'apollo-angular/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';
import { Notification, NotificationPage } from '../models/notification.model';

const mockNotification: Notification = {
  id: 'notif-1',
  type: 'TASK_ASSIGNED',
  title: 'Task Assigned',
  message: 'You have been assigned a task.',
  read: false,
  createdAt: '2024-01-01T10:00:00Z',
};

const mockPage: NotificationPage = {
  content: [mockNotification],
  totalElements: 1,
  totalPages: 1,
  pageNumber: 0,
  pageSize: 20,
};

describe('NotificationService', () => {
  let service: NotificationService;
  let controller: ApolloTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        ApolloTestingModule.withClients(['notifications']),
        HttpClientTestingModule,
      ],
      providers: [NotificationService],
    });

    service = TestBed.inject(NotificationService);
    controller = TestBed.inject(ApolloTestingController);
  });

  afterEach(() => controller.verify());

  describe('getNotifications', () => {
    it('emits notification page from server response', () => {
      let result: NotificationPage | undefined;

      service.getNotifications(0, 20).subscribe(page => (result = page));

      const op = controller.expectOne('GetNotifications');
      expect(op.operation.variables['page']).toBe(0);
      expect(op.operation.variables['size']).toBe(20);
      op.flushData({ notifications: mockPage });

      expect(result).toEqual(mockPage);
    });

    it('uses default page and size values', () => {
      service.getNotifications().subscribe();

      const op = controller.expectOne('GetNotifications');
      expect(op.operation.variables['page']).toBe(0);
      expect(op.operation.variables['size']).toBe(20);
      op.flushData({ notifications: mockPage });
    });
  });

  describe('getUnreadCount', () => {
    it('emits the unread count', () => {
      let result: number | undefined;

      service.getUnreadCount().subscribe(count => (result = count));

      const op = controller.expectOne('GetUnreadCount');
      op.flushData({ unreadCount: 5 });

      expect(result).toBe(5);
    });
  });

  describe('markAsRead', () => {
    it('sends notification id to the mutation and returns updated notification', () => {
      let result: Notification | undefined;

      service.markAsRead('notif-1').subscribe(n => (result = n));

      const op = controller.expectOne('MarkNotificationRead');
      expect(op.operation.variables['id']).toBe('notif-1');
      op.flushData({ markNotificationRead: { ...mockNotification, read: true } });

      expect(result?.read).toBe(true);
    });
  });

  describe('markAllAsRead', () => {
    it('calls the markAllNotificationsRead mutation and returns true', () => {
      let result: boolean | undefined;

      service.markAllAsRead().subscribe(r => (result = r));

      const op = controller.expectOne('MarkAllNotificationsRead');
      op.flushData({ markAllNotificationsRead: true });

      expect(result).toBe(true);
    });
  });

  describe('deleteNotification', () => {
    it('sends notification id to the mutation and returns true', () => {
      let result: boolean | undefined;

      service.deleteNotification('notif-1').subscribe(r => (result = r));

      const op = controller.expectOne('DeleteNotification');
      expect(op.operation.variables['id']).toBe('notif-1');
      op.flushData({ deleteNotification: true });

      expect(result).toBe(true);
    });
  });
});
