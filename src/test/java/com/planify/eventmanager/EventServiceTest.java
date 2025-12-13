package com.planify.eventmanager.service;

import com.planify.eventmanager.event.KafkaProducer;
import com.planify.eventmanager.model.Event;
import com.planify.eventmanager.repository.EventRepository;
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
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private Long testEventId;
    private UUID organizationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        testEventId = 1L;
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testEvent = Event.builder()
                .id(testEventId)
                .title("Test Conference")
                .description("A test conference event")
                .eventDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(7).plusHours(3))
                .locationId(1L)
                .locationName("Main Hall")
                .organizationId(organizationId)
                .organizerId(userId)
                .maxAttendees(100)
                .currentAttendees(0)
                .eventType(Event.EventType.PUBLIC)
                .status(Event.EventStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetAllEvents() {
        when(eventRepository.findAll()).thenReturn(List.of(testEvent));

        List<Event> result = eventService.getAllEvents();

        assertEquals(1, result.size());
        assertEquals(testEventId, result.get(0).getId());
        verify(eventRepository).findAll();
    }

    @Test
    void testGetEventById_Success() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));

        Event result = eventService.getEventById(testEventId);

        assertNotNull(result);
        assertEquals(testEventId, result.getId());
        verify(eventRepository).findById(testEventId);
    }

    @Test
    void testGetEventById_NotFound() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> eventService.getEventById(testEventId));

        verify(eventRepository).findById(testEventId);
    }

    @Test
    void testCreateEvent() {
        Event newEvent = Event.builder()
                .title("New Event")
                .eventDate(LocalDateTime.now().plusDays(5))
                .organizationId(organizationId)
                .organizerId(userId)
                .build();

        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.createEvent(newEvent);

        assertNotNull(result);
        assertEquals(testEventId, result.getId());
        verify(eventRepository).save(any(Event.class));
        verify(kafkaProducer).sendMessage(eq("event-created"), contains("Event created"));
    }

    @Test
    void testUpdateEvent() {
        Event details = Event.builder()
                .title("Updated Title")
                .description("Updated Description")
                .eventDate(LocalDateTime.now().plusDays(10))
                .eventType(Event.EventType.PRIVATE)
                .status(Event.EventStatus.PUBLISHED)
                .locationId(5L)
                .locationName("Updated Location")
                .maxAttendees(200)
                .build();

        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.updateEvent(testEventId, details);

        assertNotNull(result);
        verify(eventRepository).findById(testEventId);
        verify(eventRepository).save(any(Event.class));
        verify(kafkaProducer).sendMessage(eq("event-updated"), contains("Event updated"));
    }

    @Test
    void testDeleteEvent() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));

        eventService.deleteEvent(testEventId);

        verify(eventRepository).delete(testEvent);
        verify(kafkaProducer).sendMessage(eq("event-deleted"), contains("eventId"));
    }

    @Test
    void testGetEventsByOrganization() {
        when(eventRepository.findByOrganizationId(organizationId))
                .thenReturn(List.of(testEvent));

        List<Event> result = eventService.getEventsByOrganization(organizationId);

        assertEquals(1, result.size());
        assertEquals(organizationId, result.get(0).getOrganizationId());
    }

    @Test
    void testGetEventsByStatus() {
        when(eventRepository.findByStatus(Event.EventStatus.DRAFT))
                .thenReturn(List.of(testEvent));

        List<Event> result = eventService.getEventsByStatus(Event.EventStatus.DRAFT);

        assertEquals(1, result.size());
        assertEquals(Event.EventStatus.DRAFT, result.get(0).getStatus());
    }

    @Test
    void testGetPublicEvents() {
        when(eventRepository.findByEventTypeOrderByEventDateAsc(Event.EventType.PUBLIC))
                .thenReturn(List.of(testEvent));

        List<Event> result = eventService.getPublicEvents();

        assertEquals(1, result.size());
    }

    @Test
    void testGetUpcomingEvents() {
        when(eventRepository.findUpcomingEvents(any(LocalDateTime.class)))
                .thenReturn(List.of(testEvent));

        List<Event> result = eventService.getUpcomingEvents();

        assertEquals(1, result.size());
        verify(eventRepository).findUpcomingEvents(any(LocalDateTime.class));
    }

    @Test
    void testGetPastEvents() {
        when(eventRepository.findPastEvents(any(LocalDateTime.class)))
                .thenReturn(List.of(testEvent));

        List<Event> result = eventService.getPastEvents();

        assertEquals(1, result.size());
        verify(eventRepository).findPastEvents(any(LocalDateTime.class));
    }

    @Test
    void testPublishEvent() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        eventService.publishEvent(testEventId);

        verify(kafkaProducer).sendMessage(eq("event-published"), contains("Event published"));
    }

    @Test
    void testCancelEvent() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        eventService.cancelEvent(testEventId);

        verify(kafkaProducer).sendMessage(eq("event-cancelled"), contains("eventId"));
    }

    @Test
    void testCompleteEvent() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        eventService.completeEvent(testEventId);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testGetEventsByDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);

        when(eventRepository.findByEventDateBetween(start, end))
                .thenReturn(List.of(testEvent));

        List<Event> result = eventService.getEventsByDateRange(start, end);

        assertEquals(1, result.size());
    }

    @Test
    void testGetEventsByLocation() {
        when(eventRepository.findByLocationId(1L)).thenReturn(List.of(testEvent));

        List<Event> result = eventService.getEventsByLocation(1L);

        assertEquals(1, result.size());
    }
}
