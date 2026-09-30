package org.example.bookingservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookingservice.models.constants.BookingStatus;
import org.example.bookingservice.models.dto.requests.CreateBookingDetailRequest;
import org.example.bookingservice.models.dto.requests.CreateBookingRequest;
import org.example.bookingservice.models.dto.responses.BookingDetailResponse;
import org.example.bookingservice.models.dto.responses.BookingResponse;
import org.example.bookingservice.models.dto.responses.MovieResponse;
import org.example.bookingservice.models.entities.Booking;
import org.example.bookingservice.models.entities.BookingDetail;
import org.example.bookingservice.models.repositories.BookingDetailRepository;
import org.example.bookingservice.models.repositories.BookingRepository;
import org.example.bookingservice.models.services.BookingService;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final MovieGatewayService movieGatewayService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        List<MovieResponse> movieResponses = new ArrayList<>();
        double totalAmount = 0.0;

        for (CreateBookingDetailRequest item : request.items()) {
            MovieResponse movie = movieGatewayService.getMovieById(item.movieId());
            movieResponses.add(movie);
            double subtotal = movie.ticketPrice() * item.quantity();
            totalAmount += subtotal;
        }

        Booking booking = Booking.builder()
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .total(totalAmount)
                .status(BookingStatus.PENDING)
                .build();
        Booking savedBooking = bookingRepository.save(booking);

        List<BookingDetail> detailsToSave = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            CreateBookingDetailRequest item = request.items().get(i);
            MovieResponse movie = movieResponses.get(i);
            BookingDetail detail = BookingDetail.builder()
                    .booking(savedBooking)
                    .movieId(movie.id())
                    .quantity(item.quantity())
                    .unitPrice(movie.ticketPrice())
                    .build();
            detailsToSave.add(detail);
        }

        List<BookingDetail> savedDetails = bookingDetailRepository.saveAll(detailsToSave);

        List<BookingDetailResponse> detailResponses = new ArrayList<>();
        for (int i = 0; i < savedDetails.size(); i++) {
            BookingDetail detail = savedDetails.get(i);
            MovieResponse movie = movieResponses.get(i);
            double subtotal = detail.getUnitPrice() * detail.getQuantity();
            detailResponses.add(new BookingDetailResponse(
                    detail.getId(),
                    detail.getMovieId(),
                    movie.title(),
                    detail.getQuantity(),
                    detail.getUnitPrice(),
                    subtotal
            ));
        }

        kafkaTemplate.send("booking-created", request.customerEmail());

        return new BookingResponse(
                savedBooking.getId(),
                savedBooking.getCustomerName(),
                savedBooking.getCustomerEmail(),
                savedBooking.getTotal(),
                savedBooking.getStatus(),
                detailResponses
        );
    }
}
