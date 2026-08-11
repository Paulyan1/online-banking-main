export type NotificationType = 'TRANSFER_SENT' | 'TRANSFER_RECEIVED' | 'ACCOUNT_CREATED' | 'ACCOUNT_FROZEN' | 'BALANCE_LOW' | 'SYSTEM';

export interface NotificationResponse {
  id: string;
  userId: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  metadata?: Record<string, string>;
}

export interface PagedNotificationsResponse {
  notifications: NotificationResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  unreadCount: number;
}

export interface WebSocketNotification {
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  metadata?: Record<string, string>;
}
