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
import java.util.UUID;

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
    public ResponseEntity<List<GuestList>> getAllGuestsForEvent(@PathVariable UUID eventId) {
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
            @PathVariable UUID eventId,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(guestListService.getGuestEntry(eventId, userId));
    }

    @PostMapping("/invite")
    @Operation(
            summary = "Invite guest to event",
            description = "Organizer invites a user to the event. Publishes 'guest-invited' Kafka event."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Guest invited successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already invited")
    })
    public ResponseEntity<GuestList> inviteGuest(
            @PathVariable UUID eventId,
            @RequestParam UUID userId,
            @RequestParam UUID organizationId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guestListService.inviteGuest(eventId, userId, organizationId));
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
            @PathVariable UUID eventId,
            @PathVariable UUID userId
    ) {
        guestListService.removeGuest(eventId, userId);
        return ResponseEntity.noContent().build();
    }
}