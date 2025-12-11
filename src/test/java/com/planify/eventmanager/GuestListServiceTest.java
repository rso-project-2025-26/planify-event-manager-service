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
    private Long testInvitedByUserId;

    @BeforeEach
    void setUp() {
        testEventId = 1L;
        testUserId = 100L;
        testGuestId = 1L;
        testInvitedByUserId = 50L;

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
                .role(GuestList.GuestRole.ATTENDEE)
                .invitedAt(LocalDateTime.now())
                .invitedByUserId(testInvitedByUserId)
                .notes("Initial note")
                .build();
    }

    @Test
    void testGetAllGuestsForEvent() {
        List<GuestList> guests = Arrays.asList(testGuest);
        when(guestListRepository.findByEventId(testEventId)).thenReturn(guests);

        List<GuestList> result = guestListService.getAllGuestsForEvent(testEventId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testEventId, result.get(0).getEventId());
        verify(guestListRepository).findByEventId(testEventId);
    }

    @Test
    void testGetAllEventsForUser() {
        List<GuestList> events = Arrays.asList(testGuest);
        when(guestListRepository.findByUserId(testUserId)).thenReturn(events);

        List<GuestList> result = guestListService.getAllEventsForUser(testUserId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUserId, result.get(0).getUserId());
        verify(guestListRepository).findByUserId(testUserId);
    }

    @Test
    void testGetGuestEntry_Success() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));

        GuestList result = guestListService.getGuestEntry(testEventId, testUserId);

        assertNotNull(result);
        assertEquals(testEventId, result.getEventId());
        assertEquals(testUserId, result.getUserId());
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
    }

    @Test
    void testGetGuestEntry_NotFound() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> guestListService.getGuestEntry(testEventId, testUserId));
        assertTrue(exception.getMessage().contains("Guest not found"));
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
    }

    @Test
    void testInviteGuest_Success() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId)).thenReturn(false);
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);

        GuestList result = guestListService.inviteGuest(
                testEventId, testUserId, testInvitedByUserId, GuestList.GuestRole.SPEAKER, "Keynote speaker");

        assertNotNull(result);
        assertEquals(testEventId, result.getEventId());
        assertEquals(testUserId, result.getUserId());
        verify(eventRepository).findById(testEventId);
        verify(guestListRepository).existsByEventIdAndUserId(testEventId, testUserId);
        verify(guestListRepository).save(any(GuestList.class));
        verify(kafkaProducer).sendMessage(eq("guest-invited"), contains("\"eventId\":"));
    }

    @Test
    void testInviteGuest_EventNotFound() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> guestListService.inviteGuest(testEventId, testUserId, testInvitedByUserId, null, null));
        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository).findById(testEventId);
        verify(guestListRepository, never()).save(any());
    }

    @Test
    void testInviteGuest_AlreadyInvited() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> guestListService.inviteGuest(testEventId, testUserId, testInvitedByUserId, null, null));
        assertTrue(exception.getMessage().contains("already invited"));
        verify(guestListRepository, never()).save(any());
    }

    @Test
    void testRemoveGuest() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        doNothing().when(guestListRepository).delete(testGuest);

        guestListService.removeGuest(testEventId, testUserId, testInvitedByUserId);

        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
        verify(guestListRepository).delete(testGuest);
        verify(kafkaProducer).sendMessage(eq("guest-removed"), contains("\"removedBy\""));
    }

    @Test
    void testUpdateGuestRole() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);

        GuestList result = guestListService.updateGuestRole(testEventId, testUserId, GuestList.GuestRole.VIP);

        assertNotNull(result);
        assertEquals(GuestList.GuestRole.VIP, result.getRole());
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
        verify(guestListRepository).save(any(GuestList.class));
    }

    @Test
    void testUpdateGuestNotes() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);

        GuestList result = guestListService.updateGuestNotes(testEventId, testUserId, "Updated note");

        assertNotNull(result);
        assertEquals("Updated note", result.getNotes());
        verify(guestListRepository).findByEventIdAndUserId(testEventId, testUserId);
        verify(guestListRepository).save(any(GuestList.class));
    }

    @Test
    void testCountTotalGuests() {
        Long expected = 42L;
        when(guestListRepository.countByEventId(testEventId)).thenReturn(expected);

        Long result = guestListService.countTotalGuests(testEventId);

        assertEquals(expected, result);
        verify(guestListRepository).countByEventId(testEventId);
    }

    @Test
    void testIsUserInvited() {
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId)).thenReturn(true);

        boolean invited = guestListService.isUserInvited(testEventId, testUserId);

        assertTrue(invited);
        verify(guestListRepository).existsByEventIdAndUserId(testEventId, testUserId);
    }

    @Test
    void testGetGuestsByRole() {
        testGuest.setRole(GuestList.GuestRole.SPEAKER);
        List<GuestList> guests = Arrays.asList(testGuest);
        when(guestListRepository.findByEventIdAndRole(testEventId, GuestList.GuestRole.SPEAKER)).thenReturn(guests);

        List<GuestList> result = guestListService.getGuestsByRole(testEventId, GuestList.GuestRole.SPEAKER);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(GuestList.GuestRole.SPEAKER, result.get(0).getRole());
        verify(guestListRepository).findByEventIdAndRole(testEventId, GuestList.GuestRole.SPEAKER);
    }
}
