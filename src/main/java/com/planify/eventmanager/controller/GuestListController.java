package com.planify.eventmanager.controller;

import com.planify.eventmanager.model.GuestList;
import com.planify.eventmanager.service.GuestListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
@Tag(name = "Guest List", description = "Guest list and RSVP management")
public class GuestListController {
    
    private final GuestListService guestListService;
    
    // Guest Management
    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get all guests for an event")
    public ResponseEntity<List<GuestList>> getAllGuestsForEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.getAllGuestsForEvent(eventId));
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all events for a user")
    public ResponseEntity<List<GuestList>> getAllEventsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(guestListService.getAllEventsForUser(userId));
    }
    
    @GetMapping("/event/{eventId}/user/{userId}")
    @Operation(summary = "Get specific guest entry")
    public ResponseEntity<GuestList> getGuestEntry(
            @PathVariable Long eventId, 
            @PathVariable Long userId) {
        return ResponseEntity.ok(guestListService.getGuestEntry(eventId, userId));
    }
    
    @PostMapping("/invite")
    @Operation(summary = "Invite guest to event")
    public ResponseEntity<GuestList> inviteGuest(
            @RequestParam Long eventId,
            @RequestParam Long userId,
            @RequestParam(required = false) GuestList.GuestRole role,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(guestListService.inviteGuest(eventId, userId, role, notes));
    }
    
    @DeleteMapping("/event/{eventId}/user/{userId}")
    @Operation(summary = "Remove guest from event")
    public ResponseEntity<Void> removeGuest(
            @PathVariable Long eventId, 
            @PathVariable Long userId) {
        guestListService.removeGuest(eventId, userId);
        return ResponseEntity.noContent().build();
    }
    
    // RSVP Management    
    @PutMapping("/event/{eventId}/user/{userId}/rsvp")
    @Operation(summary = "Update RSVP status")
    public ResponseEntity<GuestList> updateRsvp(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @RequestParam GuestList.RsvpStatus status) {
        return ResponseEntity.ok(guestListService.updateRsvp(eventId, userId, status));
    }
    
    @PutMapping("/event/{eventId}/user/{userId}/accept")
    @Operation(summary = "Accept invitation")
    public ResponseEntity<GuestList> acceptInvitation(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(guestListService.acceptInvitation(eventId, userId));
    }
    
    @PutMapping("/event/{eventId}/user/{userId}/decline")
    @Operation(summary = "Decline invitation")
    public ResponseEntity<GuestList> declineInvitation(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(guestListService.declineInvitation(eventId, userId));
    }
    
    // Check-in Management    
    @PutMapping("/event/{eventId}/user/{userId}/check-in")
    @Operation(summary = "Check in guest")
    public ResponseEntity<GuestList> checkInGuest(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(guestListService.checkInGuest(eventId, userId));
    }
    
    @GetMapping("/event/{eventId}/checked-in")
    @Operation(summary = "Get checked-in guests")
    public ResponseEntity<List<GuestList>> getCheckedInGuests(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.getCheckedInGuests(eventId));
    }
    
    @GetMapping("/event/{eventId}/checked-in/count")
    @Operation(summary = "Count checked-in guests")
    public ResponseEntity<Long> countCheckedInGuests(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.countCheckedInGuests(eventId));
    }
    
    // Query Operations    
    @GetMapping("/event/{eventId}/status/{status}")
    @Operation(summary = "Get guests by RSVP status")
    public ResponseEntity<List<GuestList>> getGuestsByStatus(
            @PathVariable Long eventId,
            @PathVariable GuestList.RsvpStatus status) {
        return ResponseEntity.ok(guestListService.getGuestsByStatus(eventId, status));
    }
    
    @GetMapping("/event/{eventId}/role/{role}")
    @Operation(summary = "Get guests by role")
    public ResponseEntity<List<GuestList>> getGuestsByRole(
            @PathVariable Long eventId,
            @PathVariable GuestList.GuestRole role) {
        return ResponseEntity.ok(guestListService.getGuestsByRole(eventId, role));
    }
    
    @GetMapping("/event/{eventId}/user/{userId}/invited")
    @Operation(summary = "Check if user is invited")
    public ResponseEntity<Boolean> isUserInvited(
            @PathVariable Long eventId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(guestListService.isUserInvited(eventId, userId));
    }
    
    // Statistics
    @GetMapping("/event/{eventId}/count")
    @Operation(summary = "Count total guests")
    public ResponseEntity<Long> countTotalGuests(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.countTotalGuests(eventId));
    }
    
    @GetMapping("/event/{eventId}/status/{status}/count")
    @Operation(summary = "Count guests by status")
    public ResponseEntity<Long> countGuestsByStatus(
            @PathVariable Long eventId,
            @PathVariable GuestList.RsvpStatus status) {
        return ResponseEntity.ok(guestListService.countGuestsByStatus(eventId, status));
    }
}