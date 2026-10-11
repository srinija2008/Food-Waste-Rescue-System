package foodwasterescue;

import java.io.Serializable;
import java.time.LocalDateTime;

public class FoodDonation implements Serializable, Reportable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String donorName;
    private String foodName;
    private double quantityKg;
    private String location;
    private LocalDateTime deadline;
    private String status;
    private String claimedBy;

    public FoodDonation(int id, String donorName, String foodName, double quantityKg,
                        String location, LocalDateTime deadline, String status, String claimedBy) {
        this.id = id;
        this.donorName = donorName;
        this.foodName = foodName;
        this.quantityKg = quantityKg;
        this.location = location;
        this.deadline = deadline;
        this.status = status;
        this.claimedBy = claimedBy;
    }

    public int getId() { return id; }
    public String getDonorName() { return donorName; }
    public String getFoodName() { return foodName; }
    public double getQuantityKg() { return quantityKg; }
    public String getLocation() { return location; }
    public LocalDateTime getDeadline() { return deadline; }
    public String getStatus() { return status; }
    public String getClaimedBy() { return claimedBy; }

    public void setStatus(String status) { this.status = status; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }

    @Override
    public String getReportLine() {
        return id + "," + safe(donorName) + "," + safe(foodName) + "," + quantityKg + ","
                + safe(location) + "," + deadline + "," + status + "," + safe(claimedBy);
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace(",", " ").replace("\n", " ");
    }

    @Override
    public String toString() {
        return foodName + " (" + quantityKg + " kg)";
    }
}
