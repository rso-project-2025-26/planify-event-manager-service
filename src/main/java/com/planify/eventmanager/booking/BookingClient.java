package com.planify.eventmanager.booking;

import com.planify.booking_service.grpc.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingClient {

    private final BookingServiceGrpc.BookingServiceBlockingStub bookingStub;

    public CheckAvailabilityResponse checkAvailability(UUID locationId, long startEpochMillis, long endEpochMillis) {
        CheckAvailabilityRequest req = CheckAvailabilityRequest.newBuilder()
                .setLocationId(String.valueOf(locationId))
                .setStartEpochMillis(startEpochMillis)
                .setEndEpochMillis(endEpochMillis)
                .build();
        return bookingStub.checkAvailability(req);
    }

    public CreateBookingResponse createBooking(UUID locationId,
                                               UUID eventId,
                                               String organizationId,
                                               long startEpochMillis,
                                               long endEpochMillis,
                                               String currency,
                                               Map<Long, Integer> addonQuantities) {
        CreateBookingRequest.Builder builder = CreateBookingRequest.newBuilder()
                .setLocationId(String.valueOf(locationId))
                .setEventId(String.valueOf(eventId))
                .setOrganizationId(organizationId)
                .setStartEpochMillis(startEpochMillis)
                .setEndEpochMillis(endEpochMillis)
                .setCurrency(currency);

        return bookingStub.createBooking(builder.build());
    }

    public CancelBookingResponse cancelBooking(UUID bookingId) {
        CancelBookingRequest req = CancelBookingRequest.newBuilder()
                .setBookingId(String.valueOf(bookingId))
                .build();
        return bookingStub.cancelBooking(req);
    }
}
