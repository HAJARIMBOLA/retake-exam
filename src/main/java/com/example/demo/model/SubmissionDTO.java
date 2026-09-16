package com.example.demo.model;

import java.time.Instant;
import java.util.UUID;

public record SubmissionDTO(UUID id, String email, String thumbnailKey, Instant createdAt) {}
