package com.planify.eventmanager.controller;

import com.planify.eventmanager.model.GuestList;
import com.planify.eventmanager.service.GuestListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Get all guests for an event",
            description = "Returns a list of all guest entries associated with the specified event ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guests retrieved successfully")
    })
    public ResponseEntity<List<GuestList>> getAllGuestsForEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.getAllGuestsForEvent(eventId));
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get all events for a user",
            description = "Returns all guest entries indicating which events the given user is participating in or invited to."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest entries retrieved successfully")
    })
    public ResponseEntity<List<GuestList>> getAllEventsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(guestListService.getAllEventsForUser(userId));
    }

    @GetMapping("/event/{eventId}/user/{userId}")
    @Operation(
            summary = "Get specific guest entry",
            description = "Returns the guest entry for a specific user and event combination."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest entry retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Guest entry not found")
    })
    public ResponseEntity<GuestList> getGuestEntry(
            @PathVariable Long eventId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(guestListService.getGuestEntry(eventId, userId));
    }

    @PostMapping("/invite")
    @Operation(
            summary = "Invite guest to event",
            description = "Creates a guest entry for the specified user and event. Role and notes are optional."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Guest invited successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<GuestList> inviteGuest(
            @RequestParam Long eventId,
            @RequestParam Long userId,
            @RequestParam(required = false) GuestList.GuestRole role,
            @RequestParam(required = false) String notes
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guestListService.inviteGuest(eventId, userId, role, notes));
    }

    @DeleteMapping("/event/{eventId}/user/{userId}")
    @Operation(
            summary = "Remove guest from event",
            description = "Deletes the guest entry for the specified event and user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Guest removed successfully"),
            @ApiResponse(responseCode = "404", description = "Guest entry not found")
    })
    public ResponseEntity<Void> removeGuest(
            @PathVariable Long eventId,
            @PathVariable Long userId
    ) {
        guestListService.removeGuest(eventId, userId);
        return ResponseEntity.noContent().build();
    }

    // RSVP Management
    @PutMapping("/event/{eventId}/user/{userId}/rsvp")
    @Operation(
            summary = "Update RSVP status",
            description = "Updates the RSVP status (ACCEPTED, DECLINED, PENDING, etc.) for the given guest entry."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "RSVP updated successfully"),
            @ApiResponse(responseCode = "404", description = "Guest entry not found")
    })
    public ResponseEntity<GuestList> updateRsvp(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @RequestParam GuestList.RsvpStatus status
    ) {
        return ResponseEntity.ok(guestListService.updateRsvp(eventId, userId, status));
    }

    @PutMapping("/event/{eventId}/user/{userId}/accept")
    @Operation(
            summary = "Accept invitation",
            description = "Sets the guest's RSVP status to ACCEPTED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation accepted"),
            @ApiResponse(responseCode = "404", description = "Guest entry not found")
    })
    public ResponseEntity<GuestList> acceptInvitation(
            @PathVariable Long eventId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(guestListService.acceptInvitation(eventId, userId));
    }

    @PutMapping("/event/{eventId}/user/{userId}/decline")
    @Operation(
            summary = "Decline invitation",
            description = "Sets the guest's RSVP status to DECLINED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitation declined"),
            @ApiResponse(responseCode = "404", description = "Guest entry not found")
    })
    public ResponseEntity<GuestList> declineInvitation(
            @PathVariable Long eventId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(guestListService.declineInvitation(eventId, userId));
    }

    // Check-in Management
    @PutMapping("/event/{eventId}/user/{userId}/check-in")
    @Operation(
            summary = "Check in guest",
            description = "Marks the guest as checked in for the event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest checked in successfully"),
            @ApiResponse(responseCode = "404", description = "Guest entry not found")
    })
    public ResponseEntity<GuestList> checkInGuest(
            @PathVariable Long eventId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(guestListService.checkInGuest(eventId, userId));
    }

    @GetMapping("/event/{eventId}/checked-in")
    @Operation(
            summary = "Get checked-in guests",
            description = "Returns a list of guests who have checked in for the event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checked-in guests retrieved")
    })
    public ResponseEntity<List<GuestList>> getCheckedInGuests(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.getCheckedInGuests(eventId));
    }

    @GetMapping("/event/{eventId}/checked-in/count")
    @Operation(
            summary = "Count checked-in guests",
            description = "Returns the number of guests who have checked in for the event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checked-in guest count retrieved")
    })
    public ResponseEntity<Long> countCheckedInGuests(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.countCheckedInGuests(eventId));
    }

    // Query Operations
    @GetMapping("/event/{eventId}/status/{status}")
    @Operation(
            summary = "Get guests by RSVP status",
            description = "Returns all guests for an event that match the provided RSVP status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guests retrieved successfully")
    })
    public ResponseEntity<List<GuestList>> getGuestsByStatus(
            @PathVariable Long eventId,
            @PathVariable GuestList.RsvpStatus status
    ) {
        return ResponseEntity.ok(guestListService.getGuestsByStatus(eventId, status));
    }

    @GetMapping("/event/{eventId}/role/{role}")
    @Operation(
            summary = "Get guests by role",
            description = "Returns all guests for an event with the specified role (e.g., SPEAKER, VIP)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guests retrieved successfully")
    })
    public ResponseEntity<List<GuestList>> getGuestsByRole(
            @PathVariable Long eventId,
            @PathVariable GuestList.GuestRole role
    ) {
        return ResponseEntity.ok(guestListService.getGuestsByRole(eventId, role));
    }
}