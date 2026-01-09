package com.planify.eventmanager.booking;

import com.planify.booking_service.grpc.*;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingClient {

    private final BookingServiceGrpc.BookingServiceBlockingStub bookingStub;

    @Retry(name = "bookingGrpcService")
    @Bulkhead(name = "bookingGrpcService")
    @CircuitBreaker(name = "bookingGrpcService", fallbackMethod = "checkAvailabilityFallback")
    public CheckAvailabilityResponse checkAvailability(UUID locationId, long startEpochMillis, long endEpochMillis) {
        log.info("Checking availability for location: {} from {} to {}", locationId, startEpochMillis, endEpochMillis);
        CheckAvailabilityRequest req = CheckAvailabilityRequest.newBuilder()
                .setLocationId(String.valueOf(locationId))
                .setStartEpochMillis(startEpochMillis)
                .setEndEpochMillis(endEpochMillis)
                .build();
        CheckAvailabilityResponse response = bookingStub.checkAvailability(req);
        log.info("Availability check result: {}", response.getAvailable());
        return response;
    }

    private CheckAvailabilityResponse checkAvailabilityFallback(UUID locationId, long startEpochMillis, long endEpochMillis, Exception ex) {
        log.error("Booking service unavailable. Availability check fallback for location: {}. Error: {}", 
                  locationId, ex.getMessage());
        // Privzeto vrnemo, da ni na voljo
        return CheckAvailabilityResponse.newBuilder()
                .setAvailable(false)
                .build();
    }

    @Retry(name = "bookingGrpcService")
    @Bulkhead(name = "bookingGrpcService")
    @CircuitBreaker(name = "bookingGrpcService", fallbackMethod = "createBookingFallback")
    public CreateBookingResponse createBooking(UUID locationId,
                                               UUID eventId,
                                               String organizationId,
                                               long startEpochMillis,
                                               long endEpochMillis,
                                               String currency,
                                               Map<Long, Integer> addonQuantities) {
        log.info("Creating booking for event: {} at location: {}", eventId, locationId);
        CreateBookingRequest.Builder builder = CreateBookingRequest.newBuilder()
                .setLocationId(String.valueOf(locationId))
                .setEventId(String.valueOf(eventId))
                .setOrganizationId(organizationId)
                .setStartEpochMillis(startEpochMillis)
                .setEndEpochMillis(endEpochMillis)
                .setCurrency(currency);

        CreateBookingResponse response = bookingStub.createBooking(builder.build());
        log.info("Booking created successfully with ID: {}", response.getBookingId());
        return response;
    }

    private CreateBookingResponse createBookingFallback(UUID locationId, UUID eventId, String organizationId,
                                                       long startEpochMillis, long endEpochMillis, 
                                                       String currency, Map<Long, Integer> addonQuantities, Exception ex) {
        log.error("Booking service unavailable. Cannot create booking for event: {}. Error: {}", 
                  eventId, ex.getMessage());
        // Vračamo failure response
        return CreateBookingResponse.newBuilder()
                .setStatus("FAILED")
                .setAvailable(false)
                .build();
    }

    @Retry(name = "bookingGrpcService")
    @Bulkhead(name = "bookingGrpcService")
    @CircuitBreaker(name = "bookingGrpcService", fallbackMethod = "cancelBookingFallback")
    public CancelBookingResponse cancelBooking(UUID bookingId) {
        log.info("Cancelling booking: {}", bookingId);
        CancelBookingRequest req = CancelBookingRequest.newBuilder()
                .setBookingId(String.valueOf(bookingId))
                .build();
        CancelBookingResponse response = bookingStub.cancelBooking(req);
        log.info("Booking cancelled: {}", bookingId);
        return response;
    }

    private CancelBookingResponse cancelBookingFallback(UUID bookingId, Exception ex) {
        log.error("Booking service unavailable. Cannot cancel booking: {}. Error: {}", 
                  bookingId, ex.getMessage());
        return CancelBookingResponse.newBuilder()
                .setStatus("FAILED")
                .build();
    }
}
