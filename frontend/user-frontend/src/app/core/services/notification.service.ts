import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Apollo, ApolloBase, gql } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Notification, NotificationPage, NotificationPreferences } from '../models/notification.model';

const GET_NOTIFICATIONS = gql`
  query GetNotifications($page: Int, $size: Int) {
    notifications(page: $page, size: $size) {
      content {
        id
        type
        title
        message
        read
        createdAt
        referenceId
      }
      totalElements
      totalPages
      pageNumber
      pageSize
    }
  }
`;

const GET_UNREAD_COUNT = gql`
  query GetUnreadCount {
    unreadCount
  }
`;

const MARK_NOTIFICATION_READ = gql`
  mutation MarkNotificationRead($id: ID!) {
    markNotificationRead(id: $id) {
      id
      read
    }
  }
`;

const MARK_ALL_NOTIFICATIONS_READ = gql`
  mutation MarkAllNotificationsRead {
    markAllNotificationsRead
  }
`;

const DELETE_NOTIFICATION = gql`
  mutation DeleteNotification($id: ID!) {
    deleteNotification(id: $id)
  }
`;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private apollo = inject(Apollo);
  private http = inject(HttpClient);
  private prefsUrl = `${environment.apiUrl}/api/v1/notifications/preferences`;

  private get notificationApollo(): ApolloBase {
    return this.apollo.use('notifications');
  }

  getNotifications(page = 0, size = 20): Observable<NotificationPage> {
    return this.notificationApollo
      .watchQuery<{ notifications: NotificationPage }>({
        query: GET_NOTIFICATIONS,
        variables: { page, size },
        fetchPolicy: 'network-only',
      })
      .valueChanges.pipe(map(result => result.data.notifications));
  }

  getUnreadCount(): Observable<number> {
    return this.notificationApollo
      .watchQuery<{ unreadCount: number }>({
        query: GET_UNREAD_COUNT,
        fetchPolicy: 'network-only',
      })
      .valueChanges.pipe(map(result => result.data.unreadCount));
  }

  markAsRead(id: string): Observable<Notification> {
    return this.notificationApollo
      .mutate<{ markNotificationRead: Notification }>({
        mutation: MARK_NOTIFICATION_READ,
        variables: { id },
      })
      .pipe(map(result => result.data!.markNotificationRead));
  }

  markAllAsRead(): Observable<boolean> {
    return this.notificationApollo
      .mutate<{ markAllNotificationsRead: boolean }>({
        mutation: MARK_ALL_NOTIFICATIONS_READ,
      })
      .pipe(map(result => result.data!.markAllNotificationsRead));
  }

  deleteNotification(id: string): Observable<boolean> {
    return this.notificationApollo
      .mutate<{ deleteNotification: boolean }>({
        mutation: DELETE_NOTIFICATION,
        variables: { id },
      })
      .pipe(map(result => result.data!.deleteNotification));
  }

  getPreferences(): Observable<NotificationPreferences> {
    return this.http.get<NotificationPreferences>(this.prefsUrl);
  }

  updatePreferences(preferences: Partial<NotificationPreferences>): Observable<NotificationPreferences> {
    return this.http.put<NotificationPreferences>(this.prefsUrl, preferences);
  }
}
