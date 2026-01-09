package com.planify.eventmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "guest_list")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestList {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt;
    
    @PrePersist
    protected void onCreate() {
        invitedAt = LocalDateTime.now();
    }
}