package com.planify.eventmanager.service;

import com.planify.eventmanager.event.KafkaProducer;
import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.model.GuestList;
import com.planify.eventmanager.repository.EventRepository;
import com.planify.eventmanager.repository.GuestListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestListServiceTest {

    @Mock
    private GuestListRepository guestListRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private GuestListService guestListService;

    private GuestList testGuest;
    private Event testEvent;
    private Long testEventId;
    private Long testUserId;
    private Long testGuestId;

    @BeforeEach
    void setUp() {
        testEventId = 1L;
        testUserId = 100L;
        testGuestId = 1L;

        testEvent = Event.builder()
                .id(testEventId)
                .title("Test Conference")
                .description("A test conference event")
                .eventDate(LocalDateTime.now().plusDays(7))
                .organizerId(50L)
                .maxAttendees(100)
                .currentAttendees(0)
                .status(Event.EventStatus.PUBLISHED)
                .build();

        testGuest = GuestList.builder()
                .id(testGuestId)
                .eventId(testEventId)
                .userId(testUserId)
                .rsvpStatus(GuestList.RsvpStatus.PENDING)
                .role(GuestList.GuestRole.ATTENDEE)
                .invitedAt(LocalDateTime.now())
                .checkedIn(false)
                .build();
    }

    // CRUD Operations Tests

    @Test
    void testGetAllGuestsForEvent() {
        // Arrange
        List<GuestList> guests = Arrays.asList(testGuest);
        when(guestListRepository.findByEventId(testEventId)).thenReturn(guests);

        // Act
        List<GuestList> result = guestListService.getAllGuestsForEvent(testEventId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testEventId, result.get(0).getEventId());
        verify(guestListRepository).findByEventId(testEventId);
    }

    @Test
    void testGetAllEventsForUser() {
        // Arrange
        List<GuestList> events = Arrays.asList(testGuest);
        when(guestListRepository.findByUserId(testUserId)).thenReturn(events);

        // Act
        List<GuestList> result = guestListService.getAllEventsForUser(testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserId, result.get(0).getUserId());
        verify(guestListRepository).findByUserId(testUserId);
    }

    @Test
    void testGetGuestEntry_Success() {
        // Arrange
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));

        // Act
        GuestList result = guestListService.getGuestEntry(testEventId, testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testEventId, result.getEventId());
        assertEquals(testUserId, result.getUserId());
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
    }

    @Test
    void testGetGuestEntry_NotFound() {
        // Arrange
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> guestListService.getGuestEntry(testEventId, testUserId));
        assertTrue(exception.getMessage().contains("Guest not found"));
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
    }

    @Test
    void testInviteGuest_Success() {
        // Arrange
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(false);
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);

        // Act
        GuestList result = guestListService.inviteGuest(
                testEventId, testUserId, GuestList.GuestRole.SPEAKER, "Keynote speaker");

        // Assert
        assertNotNull(result);
        assertEquals(testEventId, result.getEventId());
        assertEquals(testUserId, result.getUserId());
        verify(eventRepository).findById(testEventId);
        verify(guestListRepository).existsByEventIdAndUserId(testEventId, testUserId);
        verify(guestListRepository).save(any(GuestList.class));
        verify(kafkaProducer).sendMessage(eq("guest-invited"), contains("User"));
    }

    @Test
    void testInviteGuest_EventNotFound() {
        // Arrange
        when(eventRepository.findById(testEventId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> guestListService.inviteGuest(testEventId, testUserId, null, null));
        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository).findById(testEventId);
        verify(guestListRepository, never()).save(any());
    }

    @Test
    void testInviteGuest_AlreadyInvited() {
        // Arrange
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> guestListService.inviteGuest(testEventId, testUserId, null, null));
        assertTrue(exception.getMessage().contains("already invited"));
        verify(guestListRepository, never()).save(any());
    }

    @Test
    void testRemoveGuest() {
        // Arrange
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        doNothing().when(guestListRepository).delete(testGuest);

        // Act
        guestListService.removeGuest(testEventId, testUserId);

        // Assert
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
        verify(guestListRepository).delete(testGuest);
        verify(kafkaProducer).sendMessage(eq("guest-removed"), contains("removed"));
    }

    // RSVP Management Tests

    @Test
    void testUpdateRsvp_Accept() {
        // Arrange
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.countByEventIdAndRsvpStatus(
                eq(testEventId), eq(GuestList.RsvpStatus.ACCEPTED))).thenReturn(1L);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        GuestList result = guestListService.updateRsvp(
                testEventId, testUserId, GuestList.RsvpStatus.ACCEPTED);

        // Assert
        assertNotNull(result);
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
        verify(guestListRepository).save(any(GuestList.class));
        verify(kafkaProducer).sendMessage(eq("rsvp-updated"), contains("RSVP"));
        verify(eventRepository).findById(testEventId);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testAcceptInvitation() {
        // Arrange
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.countByEventIdAndRsvpStatus(
                eq(testEventId), eq(GuestList.RsvpStatus.ACCEPTED))).thenReturn(1L);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        GuestList result = guestListService.acceptInvitation(testEventId, testUserId);

        // Assert
        assertNotNull(result);
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
        verify(kafkaProducer).sendMessage(eq("rsvp-updated"), contains("ACCEPTED"));
    }

    @Test
    void testDeclineInvitation() {
        // Arrange
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.countByEventIdAndRsvpStatus(
                eq(testEventId), eq(GuestList.RsvpStatus.ACCEPTED))).thenReturn(0L);
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        // Act
        GuestList result = guestListService.declineInvitation(testEventId, testUserId);

        // Assert
        assertNotNull(result);
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
        verify(kafkaProducer).sendMessage(eq("rsvp-updated"), contains("DECLINED"));
    }

    // Check-in Management Tests

    @Test
    void testCheckInGuest_Success() {
        // Arrange
        testGuest.setRsvpStatus(GuestList.RsvpStatus.ACCEPTED);
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);

        // Act
        GuestList result = guestListService.checkInGuest(testEventId, testUserId);

        // Assert
        assertNotNull(result);
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
        verify(guestListRepository).save(any(GuestList.class));
        verify(kafkaProducer).sendMessage(eq("guest-checked-in"), contains("checked in"));
    }

    @Test
    void testCheckInGuest_NotAccepted() {
        // Arrange
        testGuest.setRsvpStatus(GuestList.RsvpStatus.PENDING);
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> guestListService.checkInGuest(testEventId, testUserId));
        assertTrue(exception.getMessage().contains("not accepted"));
        verify(guestListRepository, never()).save(any());
        verify(kafkaProducer, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void testGetCheckedInGuests() {
        // Arrange
        testGuest.setCheckedIn(true);
        List<GuestList> guests = Arrays.asList(testGuest);
        when(guestListRepository.findByEventIdAndCheckedIn_True(testEventId)).thenReturn(guests);

        // Act
        List<GuestList> result = guestListService.getCheckedInGuests(testEventId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getCheckedIn());
        verify(guestListRepository).findByEventIdAndCheckedIn_True(testEventId);
    }

    @Test
    void testCountCheckedInGuests() {
        // Arrange
        Long expectedCount = 25L;
        when(guestListRepository.countByEventIdAndCheckedIn_True(testEventId))
                .thenReturn(expectedCount);

        // Act
        Long result = guestListService.countCheckedInGuests(testEventId);

        // Assert
        assertEquals(expectedCount, result);
        verify(guestListRepository).countByEventIdAndCheckedIn_True(testEventId);
    }

    // Query Operations Tests

    @Test
    void testGetGuestsByStatus() {
        // Arrange
        List<GuestList> guests = Arrays.asList(testGuest);
        when(guestListRepository.findByEventIdAndRsvpStatus(
                testEventId, GuestList.RsvpStatus.PENDING)).thenReturn(guests);

        // Act
        List<GuestList> result = guestListService.getGuestsByStatus(
                testEventId, GuestList.RsvpStatus.PENDING);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(GuestList.RsvpStatus.PENDING, result.get(0).getRsvpStatus());
        verify(guestListRepository).findByEventIdAndRsvpStatus(
                testEventId, GuestList.RsvpStatus.PENDING);
    }

    @Test
    void testGetGuestsByRole() {
        // Arrange
        testGuest.setRole(GuestList.GuestRole.SPEAKER);
        List<GuestList> guests = Arrays.asList(testGuest);
        when(guestListRepository.findByEventIdAndRole(
                testEventId, GuestList.GuestRole.SPEAKER)).thenReturn(guests);

        // Act
        List<GuestList> result = guestListService.getGuestsByRole(
                testEventId, GuestList.GuestRole.SPEAKER);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(GuestList.GuestRole.SPEAKER, result.get(0).getRole());
        verify(guestListRepository).findByEventIdAndRole(
                testEventId, GuestList.GuestRole.SPEAKER);
    }
}