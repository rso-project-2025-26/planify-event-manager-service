package com.planify.eventmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    // Location reference
    @Column(name = "location_id")
    private UUID locationId;
    
    @Column(name = "location_name", length = 500)
    private String locationName;

    // Booking linkage (booking-service)
    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "booking_status", length = 50)
    private String bookingStatus;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;
    
    @Column(name = "max_attendees")
    private Integer maxAttendees;
    
    @Column(name = "current_attendees")
    @Builder.Default
    private Integer currentAttendees = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    @Builder.Default
    private EventType eventType = EventType.PRIVATE;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = EventStatus.DRAFT;
        if (eventType == null) eventType = EventType.PRIVATE;
        if (currentAttendees == null) currentAttendees = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum EventStatus {
        DRAFT, PUBLISHED, CANCELLED, COMPLETED
    }
    
    public enum EventType {
        PRIVATE,
        PUBLIC
    }
}