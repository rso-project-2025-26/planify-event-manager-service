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
@RequestMapping("/api/events/{eventId}/guests")
@RequiredArgsConstructor
@Tag(name = "Guest List", description = "Guest list management for event organizers")
public class GuestListController {

    private final GuestListService guestListService;

    @GetMapping
    @Operation(
            summary = "Get all guests for an event",
            description = "Returns list of all guests invited to the event (organizer view)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guests retrieved successfully")
    })
    public ResponseEntity<List<GuestList>> getAllGuestsForEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.getAllGuestsForEvent(eventId));
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get specific guest entry",
            description = "Returns guest details for specific user in the event"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest found"),
            @ApiResponse(responseCode = "404", description = "Guest not found")
    })
    public ResponseEntity<GuestList> getGuestEntry(
            @PathVariable Long eventId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(guestListService.getGuestEntry(eventId, userId));
    }

    @PostMapping
    @Operation(
            summary = "Invite guest to event",
            description = "Organizer invites a user to the event. Publishes 'guest-invited' Kafka event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Guest invited successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already invited")
    })
    public ResponseEntity<GuestList> inviteGuest(
            @PathVariable Long eventId,
            @RequestParam Long userId,
            @RequestParam Long invitedByUserId,
            @RequestParam(required = false) GuestList.GuestRole role,
            @RequestParam(required = false) String notes
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guestListService.inviteGuest(eventId, userId, invitedByUserId, role, notes));
    }

    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Remove guest from event",
            description = "Organizer removes a guest from the event. Publishes 'guest-removed' Kafka event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Guest removed successfully"),
            @ApiResponse(responseCode = "404", description = "Guest not found")
    })
    public ResponseEntity<Void> removeGuest(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @RequestParam Long removedByUserId
    ) {
        guestListService.removeGuest(eventId, userId, removedByUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/role")
    @Operation(
            summary = "Update guest role",
            description = "Organizer updates guest's role (ATTENDEE, SPEAKER, VIP, STAFF)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "404", description = "Guest not found")
    })
    public ResponseEntity<GuestList> updateGuestRole(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @RequestParam GuestList.GuestRole role
    ) {
        return ResponseEntity.ok(guestListService.updateGuestRole(eventId, userId, role));
    }

    @PutMapping("/{userId}/notes")
    @Operation(
            summary = "Update guest notes",
            description = "Organizer updates notes about a guest"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notes updated successfully"),
            @ApiResponse(responseCode = "404", description = "Guest not found")
    })
    public ResponseEntity<GuestList> updateGuestNotes(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            @RequestBody String notes
    ) {
        return ResponseEntity.ok(guestListService.updateGuestNotes(eventId, userId, notes));
    }

    @GetMapping("/role/{role}")
    @Operation(
            summary = "Get guests by role",
            description = "Returns all guests with specific role"
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

    @GetMapping("/count")
    @Operation(
            summary = "Count total guests",
            description = "Returns total number of invited guests"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    })
    public ResponseEntity<Long> countTotalGuests(@PathVariable Long eventId) {
        return ResponseEntity.ok(guestListService.countTotalGuests(eventId));
    }
}