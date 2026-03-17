package com.uzima.bootstrap.adapter.http.dto;

public record CreateReminderRequest(String content, String trigger, String scheduledAt) {}