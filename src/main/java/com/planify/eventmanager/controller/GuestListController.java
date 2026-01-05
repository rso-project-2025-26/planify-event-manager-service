package com.planify.eventmanager.controller;

import com.planify.eventmanager.model.GuestList;
import com.planify.eventmanager.service.EventService;
import com.planify.eventmanager.service.GuestListService;
import com.planify.eventmanager.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/guests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Guest List", description = "Guest list management endpoints for event organizers")
@SecurityRequirement(name = "bearer-jwt")
public class GuestListController {

    private final GuestListService guestListService;
    private final EventService eventService;
    private final SecurityService securityService;

    @GetMapping
    @Operation(
        summary = "Get all guests for event",
        description = "Returns complete guest list for an event including RSVP status and attendance information. Organizer view."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved guest list",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = GuestList.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<List<GuestList>> getAllGuestsForEvent(
            @Parameter(required = true)
            @PathVariable UUID eventId) {
        if (!securityService.hasAnyRoleInOrganization(eventService.getEventById(eventId).getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to delete events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(guestListService.getAllGuestsForEvent(eventId));
    }

    @GetMapping("/{userId}")
    @Operation(
        summary = "Get specific guest entry",
        description = "Returns detailed guest information for a specific user in the event, including invitation status and RSVP response."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved guest entry",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = GuestList.class))),
        @ApiResponse(responseCode = "400", description = "Bad request", content = @Content),
    })
    public ResponseEntity<GuestList> getGuestEntry(
            @Parameter(required = true)
            @PathVariable UUID eventId,
            @Parameter(required = true)
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(guestListService.getGuestEntry(eventId, userId));
    }

    @PostMapping("/invite")
    @Operation(
        summary = "Invite guest to event",
        description = "Organizer invites a user to the event. Publishes 'guest-invited' Kafka event which triggers invitation notification via notification service."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Guest successfully invited",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = GuestList.class))),
        @ApiResponse(responseCode = "400", description = "Bad request - Invalid data or user already invited", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<GuestList> inviteGuest(
            @Parameter(required = true)
            @PathVariable UUID eventId,
            @Parameter(required = true)
            @RequestParam UUID userId,
            @Parameter(required = true)
            @RequestParam UUID organizationId
    ) {
        if (!securityService.hasAnyRoleInOrganization(eventService.getEventById(eventId).getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to delete events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(guestListService.inviteGuest(eventId, userId, organizationId));
    }

    @DeleteMapping("/{userId}")
    @Operation(
        summary = "Remove guest from event",
        description = "Organizer removes a guest from the event invitation list. Publishes 'guest-removed' Kafka event for tracking purposes."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Guest successfully removed", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'ORGANISER')")
    public ResponseEntity<Void> removeGuest(
            @Parameter(required = true)
            @PathVariable UUID eventId,
            @Parameter(required = true)
            @PathVariable UUID userId
    ) {
        if (!securityService.hasAnyRoleInOrganization(eventService.getEventById(eventId).getOrganizationId(), List.of("ORG_ADMIN", "ORGANISER"))) {
            log.error("User is not authorized to delete events in this organization.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        guestListService.removeGuest(eventId, userId);
        return ResponseEntity.noContent().build();
    }
}