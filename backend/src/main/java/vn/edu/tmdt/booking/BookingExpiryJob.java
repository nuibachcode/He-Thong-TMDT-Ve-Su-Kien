package vn.edu.tmdt.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingService bookings;

    public BookingExpiryJob(BookingService bookings) {
        this.bookings = bookings;
    }

    /** Moi 60 giay nha ve cua cac don PENDING da het han giu cho. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void releaseExpired() {
        int released = bookings.expireOverdue();
        if (released > 0) {
            log.info("Da nha {} don het han giu cho", released);
        }
    }
}
