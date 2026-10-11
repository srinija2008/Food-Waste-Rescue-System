package foodwasterescue;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

public class DeadlineMonitor implements AutoCloseable {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DonationService service;
    private final java.util.function.Consumer<String> alertConsumer;

    public DeadlineMonitor(DonationService service, java.util.function.Consumer<String> alertConsumer) {
        this.service = service;
        this.alertConsumer = alertConsumer;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<FoodDonation> donations = service.getAvailableByDeadline();
                LocalDateTime now = LocalDateTime.now();
                for (FoodDonation d : donations) {
                    long minutes = Duration.between(now, d.getDeadline()).toMinutes();
                    if (minutes >= 0 && minutes <= 60) {
                        alertConsumer.accept("Urgent: " + d.getFoodName() + " (ID " + d.getId()
                                + ") has " + minutes + " minute(s) left.");
                    }
                }
            } catch (SQLException ex) {
                alertConsumer.accept("Background deadline check failed: " + ex.getMessage());
            } catch (Exception ex) {
                alertConsumer.accept("Deadline monitor error: " + ex.getMessage());
            }
        }, 5, 60, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
