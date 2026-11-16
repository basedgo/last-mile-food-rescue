package com.app.notifications;

public record NotificationEvent(String eventType, String subject, String message) {
}
