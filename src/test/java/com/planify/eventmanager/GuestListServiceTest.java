package com.planify.eventmanager;

import com.planify.eventmanager.event.KafkaProducer;
import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.model.GuestList;
import com.planify.eventmanager.repository.EventRepository;
import com.planify.eventmanager.repository.GuestListRepository;
import com.planify.eventmanager.service.GuestListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

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
    private UUID testUserId;
    private Long testGuestId;
    private UUID testInvitedByUserId;

    @BeforeEach
    void setUp() {
        testEventId = 1L;
        testUserId = UUID.randomUUID();
        testGuestId = 10L;
        testInvitedByUserId = UUID.randomUUID();

        testEvent = Event.builder()
                .id(testEventId)
                .title("Test Conference")
                .description("Conference")
                .eventDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(7).plusHours(2))
                .organizationId(UUID.randomUUID())
                .organizerId(UUID.randomUUID())
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
        when(guestListRepository.findByEventId(testEventId))
                .thenReturn(List.of(testGuest));

        List<GuestList> result = guestListService.getAllGuestsForEvent(testEventId);

        assertEquals(1, result.size());
        assertEquals(testEventId, result.get(0).getEventId());
        verify(guestListRepository).findByEventId(testEventId);
    }

    @Test
    void testGetAllEventsForUser() {
        when(guestListRepository.findByUserId(testUserId))
                .thenReturn(List.of(testGuest));

        List<GuestList> result = guestListService.getAllEventsForUser(testUserId);

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
    }

    @Test
    void testGetGuestEntry_NotFound() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> guestListService.getGuestEntry(testEventId, testUserId));
    }

    @Test
    void testInviteGuest_Success() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId)).thenReturn(false);
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);

        GuestList result = guestListService.inviteGuest(
                testEventId, testUserId, testInvitedByUserId,
                GuestList.GuestRole.SPEAKER, "Keynote");

        assertNotNull(result);
        assertEquals(testEventId, result.getEventId());
        assertEquals(testUserId, result.getUserId());

        verify(kafkaProducer).sendMessage(eq("guest-invited"), contains("\"eventId\":"));
    }

    @Test
    void testInviteGuest_EventNotFound() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> guestListService.inviteGuest(testEventId, testUserId, testInvitedByUserId, null, null));
    }

    @Test
    void testInviteGuest_AlreadyInvited() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId)).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> guestListService.inviteGuest(testEventId, testUserId, testInvitedByUserId, null, null));

        verify(guestListRepository, never()).save(any());
    }

    @Test
    void testRemoveGuest() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));

        guestListService.removeGuest(testEventId, testUserId, testInvitedByUserId);

        verify(guestListRepository).delete(testGuest);
        verify(kafkaProducer).sendMessage(eq("guest-removed"), contains("\"removedBy\""));
    }

    @Test
    void testUpdateGuestRole() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);

        GuestList result = guestListService.updateGuestRole(testEventId, testUserId, GuestList.GuestRole.VIP);

        assertEquals(GuestList.GuestRole.VIP, result.getRole());
    }

    @Test
    void testUpdateGuestNotes() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));
        when(guestListRepository.save(any(GuestList.class))).thenReturn(testGuest);

        GuestList result = guestListService.updateGuestNotes(testEventId, testUserId, "Updated note");

        assertEquals("Updated note", result.getNotes());
    }

    @Test
    void testIsUserInvited() {
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId)).thenReturn(true);

        assertTrue(guestListService.isUserInvited(testEventId, testUserId));
    }

    @Test
    void testGetGuestsByRole() {
        testGuest.setRole(GuestList.GuestRole.SPEAKER);

        when(guestListRepository.findByEventIdAndRole(testEventId, GuestList.GuestRole.SPEAKER))
                .thenReturn(List.of(testGuest));

        List<GuestList> result = guestListService.getGuestsByRole(testEventId, GuestList.GuestRole.SPEAKER);

        assertEquals(1, result.size());
        assertEquals(GuestList.GuestRole.SPEAKER, result.get(0).getRole());
    }
}
