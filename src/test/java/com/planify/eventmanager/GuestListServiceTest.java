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
    private UUID testEventId;
    private UUID testUserId;
    private UUID testGuestId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        testEventId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testGuestId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        testEvent = Event.builder()
                .id(testEventId)
                .title("Test Conference")
                .description("Conference")
                .eventDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(7).plusHours(2))
                .organizationId(organizationId)
                .organizerId(UUID.randomUUID())
                .maxAttendees(100)
                .currentAttendees(0)
                .status(Event.EventStatus.PUBLISHED)
                .build();

        testGuest = GuestList.builder()
                .id(testGuestId)
                .eventId(testEventId)
                .userId(testUserId)
                .organizationId(organizationId)
                .invitedAt(LocalDateTime.now())
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

        GuestList result = guestListService.inviteGuest(testEventId, testUserId, organizationId);

        assertNotNull(result);
        assertEquals(testEventId, result.getEventId());
        assertEquals(testUserId, result.getUserId());
        assertEquals(organizationId, result.getOrganizationId());

        verify(kafkaProducer).sendMessage(eq("guest-invited"), contains("\"eventId\":"));
    }

    @Test
    void testInviteGuest_EventNotFound() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> guestListService.inviteGuest(testEventId, testUserId, organizationId));
    }

    @Test
    void testInviteGuest_AlreadyInvited() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId)).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> guestListService.inviteGuest(testEventId, testUserId, organizationId));

        verify(guestListRepository, never()).save(any());
    }

    @Test
    void testRemoveGuest() {
        when(guestListRepository.findByEventIdAndUserId(testEventId, testUserId))
                .thenReturn(Optional.of(testGuest));

        guestListService.removeGuest(testEventId, testUserId);

        verify(guestListRepository).delete(testGuest);
        verify(kafkaProducer).sendMessage(eq("guest-removed"), contains("\"eventId\":"));
    }

    @Test
    void testIsUserInvited() {
        when(guestListRepository.existsByEventIdAndUserId(testEventId, testUserId)).thenReturn(true);

        assertTrue(guestListService.isUserInvited(testEventId, testUserId));
    }

    @Test
    void testGetGuestsByOrganization() {
        when(guestListRepository.findByOrganizationId(organizationId))
                .thenReturn(List.of(testGuest));

        List<GuestList> result = guestListService.getGuestsByOrganization(organizationId);

        assertEquals(1, result.size());
        assertEquals(organizationId, result.get(0).getOrganizationId());
        verify(guestListRepository).findByOrganizationId(organizationId);
    }
}