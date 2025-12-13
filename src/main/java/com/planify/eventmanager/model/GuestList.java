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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false)
    private Long eventId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private GuestRole role = GuestRole.ATTENDEE;
    
    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt;
    
    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;
    
    @Column(length = 1000)
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        invitedAt = LocalDateTime.now();
        if (role == null) role = GuestRole.ATTENDEE;
    }
    
    public enum GuestRole {
        ATTENDEE, SPEAKER, VIP, STAFF
    }
}