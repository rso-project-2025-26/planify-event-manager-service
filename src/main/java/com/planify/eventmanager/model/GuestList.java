package com.planify.eventmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status", nullable = false)
    @Builder.Default
    private RsvpStatus rsvpStatus = RsvpStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private GuestRole role = GuestRole.ATTENDEE;
    
    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
    
    @Column(name = "checked_in")
    @Builder.Default
    private Boolean checkedIn = false;
    
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;
    
    @Column(length = 1000)
    private String notes;
    
    @PrePersist
    protected void onCreate() {
        invitedAt = LocalDateTime.now();
        if (rsvpStatus == null) rsvpStatus = RsvpStatus.PENDING;
        if (role == null) role = GuestRole.ATTENDEE;
        if (checkedIn == null) checkedIn = false;
    }
    
    public enum RsvpStatus {
        PENDING, ACCEPTED, DECLINED, MAYBE
    }
    
    public enum GuestRole {
        ATTENDEE, SPEAKER, VIP, STAFF
    }
}