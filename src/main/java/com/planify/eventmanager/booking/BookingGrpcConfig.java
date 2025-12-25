package com.planify.eventmanager.booking;

import com.planify.booking_service.grpc.BookingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookingGrpcConfig {

    @Value("${booking.grpc.host:localhost}")
    private String host;

    @Value("${booking.grpc.port:9095}")
    private int port;

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel bookingManagedChannel() {
        return NettyChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @Bean
    public BookingServiceGrpc.BookingServiceBlockingStub bookingBlockingStub(ManagedChannel bookingManagedChannel) {
        return BookingServiceGrpc.newBlockingStub(bookingManagedChannel);
    }
}
