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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private Long testOrganizerId;

    @BeforeEach
    void setUp() {
        testEventId = 1L;
        testOrganizerId = 100L;

        testEvent = Event.builder()
                .id(testEventId)
                .title("Test Conference")
                .description("A test conference event")
                .eventDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(7).plusHours(4))
                .locationId(1L)
                .locationName("Conference Hall A")
                .organizerId(testOrganizerId)
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
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findAll()).thenReturn(events);

        List<Event> result = eventService.getAllEvents();

        assertNotNull(result);
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
        assertEquals("Test Conference", result.getTitle());
        verify(eventRepository).findById(testEventId);
    }

    @Test
    void testGetEventById_NotFound() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.getEventById(testEventId));
        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository).findById(testEventId);
    }

    @Test
    void testCreateEvent() {
        Event newEvent = Event.builder()
                .title("New Event")
                .description("New event description")
                .eventDate(LocalDateTime.now().plusDays(5))
                .organizerId(testOrganizerId)
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
        Event updatedDetails = Event.builder()
                .title("Updated Conference")
                .description("Updated description")
                .eventDate(LocalDateTime.now().plusDays(10))
                .endDate(LocalDateTime.now().plusDays(10).plusHours(5))
                .locationId(2L)
                .locationName("Conference Hall B")
                .maxAttendees(150)
                .eventType(Event.EventType.PRIVATE)
                .status(Event.EventStatus.PUBLISHED)
                .build();

        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.updateEvent(testEventId, updatedDetails);

        assertNotNull(result);
        verify(eventRepository).findById(testEventId);
        verify(eventRepository).save(any(Event.class));
        verify(kafkaProducer).sendMessage(eq("event-updated"), contains("Event updated"));
    }

    @Test
    void testDeleteEvent() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        doNothing().when(eventRepository).delete(testEvent);

        eventService.deleteEvent(testEventId);

        verify(eventRepository).findById(testEventId);
        verify(eventRepository).delete(testEvent);
        verify(kafkaProducer).sendMessage(eq("event-deleted"), contains("eventId"));
    }

    @Test
    void testGetEventsByOrganizer() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByOrganizerId(testOrganizerId)).thenReturn(events);

        List<Event> result = eventService.getEventsByOrganizer(testOrganizerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testOrganizerId, result.get(0).getOrganizerId());
        verify(eventRepository).findByOrganizerId(testOrganizerId);
    }

    @Test
    void testGetEventsByStatus() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByStatus(Event.EventStatus.DRAFT)).thenReturn(events);

        List<Event> result = eventService.getEventsByStatus(Event.EventStatus.DRAFT);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Event.EventStatus.DRAFT, result.get(0).getStatus());
        verify(eventRepository).findByStatus(Event.EventStatus.DRAFT);
    }

    @Test
    void testGetPublicEvents() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByEventTypeOrderByEventDateAsc(Event.EventType.PUBLIC)).thenReturn(events);

        List<Event> result = eventService.getPublicEvents();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(Event.EventType.PUBLIC, result.get(0).getEventType());
        verify(eventRepository).findByEventTypeOrderByEventDateAsc(Event.EventType.PUBLIC);
    }

    @Test
    void testGetUpcomingEvents() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findUpcomingEvents(any(LocalDateTime.class))).thenReturn(events);

        List<Event> result = eventService.getUpcomingEvents();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findUpcomingEvents(any(LocalDateTime.class));
    }

    @Test
    void testGetPastEvents() {
        Event pastEvent = Event.builder()
                .id(2L)
                .title("Past Event")
                .eventDate(LocalDateTime.now().minusDays(7))
                .organizerId(testOrganizerId)
                .build();
        List<Event> events = Arrays.asList(pastEvent);
        when(eventRepository.findPastEvents(any(LocalDateTime.class))).thenReturn(events);

        List<Event> result = eventService.getPastEvents();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findPastEvents(any(LocalDateTime.class));
    }

    @Test
    void testPublishEvent() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.publishEvent(testEventId);

        assertNotNull(result);
        verify(eventRepository).findById(testEventId);
        verify(eventRepository).save(any(Event.class));
        verify(kafkaProducer).sendMessage(eq("event-published"), contains("Event published"));
    }

    @Test
    void testCancelEvent() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.cancelEvent(testEventId);

        assertNotNull(result);
        verify(eventRepository).findById(testEventId);
        verify(eventRepository).save(any(Event.class));
        verify(kafkaProducer).sendMessage(eq("event-cancelled"), contains("eventId"));
    }

    @Test
    void testCompleteEvent() {
        when(eventRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        Event result = eventService.completeEvent(testEventId);

        assertNotNull(result);
        verify(eventRepository).findById(testEventId);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testGetEventsByDateRange() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(10);
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByEventDateBetween(start, end)).thenReturn(events);

        List<Event> result = eventService.getEventsByDateRange(start, end);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findByEventDateBetween(start, end);
    }

    @Test
    void testGetEventsByLocation() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByLocationId(1L)).thenReturn(events);

        List<Event> result = eventService.getEventsByLocation(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findByLocationId(1L);
    }
}
