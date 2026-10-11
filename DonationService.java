package foodwasterescue;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class DonationService {
    // Collections are used for in-memory processing and demonstration.
    private final ArrayList<FoodDonation> donationList = new ArrayList<>();
    private final LinkedList<Integer> notificationQueue = new LinkedList<>();
    private final HashSet<Integer> knownIds = new HashSet<>();
    private final HashMap<Integer, FoodDonation> donationCache = new HashMap<>();
    private final TreeSet<String> foodNamesSorted = new TreeSet<>();
    private final TreeMap<String, Integer> statusCounts = new TreeMap<>();

    public synchronized List<FoodDonation> getAllDonations() throws SQLException {
        ArrayList<FoodDonation> rows = new ArrayList<>();
        String sql = "SELECT id, donor_name, food_name, quantity_kg, location, deadline, status, claimed_by "
                   + "FROM donations ORDER BY deadline ASC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                FoodDonation d = mapRow(rs);
                rows.add(d);
                donationCache.put(d.getId(), d);
                knownIds.add(d.getId());
                foodNamesSorted.add(d.getFoodName());
            }
        }
        donationList.clear();
        donationList.addAll(rows);
        rebuildStatusCounts(rows);
        return rows;
    }

    public synchronized void addDonation(String donor, String food, double quantity,
                                         String location, LocalDateTime deadline)
            throws FoodRescueException, SQLException {
        if (donor == null || donor.trim().isEmpty()) throw new FoodRescueException("Donor name is required.");
        if (food == null || food.trim().isEmpty()) throw new FoodRescueException("Food name is required.");
        if (quantity <= 0) throw new FoodRescueException("Quantity must be greater than zero.");
        if (location == null || location.trim().isEmpty()) throw new FoodRescueException("Collection location is required.");
        if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
            throw new FoodRescueException("Deadline must be in the future.");
        }

        String sql = "INSERT INTO donations(donor_name, food_name, quantity_kg, location, deadline, status, claimed_by) "
                   + "VALUES(?, ?, ?, ?, ?, 'AVAILABLE', NULL)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, donor.trim());
            ps.setString(2, food.trim());
            ps.setDouble(3, quantity);
            ps.setString(4, location.trim());
            ps.setTimestamp(5, Timestamp.valueOf(deadline));
            ps.executeUpdate();
        }
        getAllDonations();
    }

    public synchronized void claimDonation(int id, String volunteer)
            throws FoodRescueException, SQLException {
        if (volunteer == null || volunteer.trim().isEmpty()) {
            throw new FoodRescueException("Volunteer name is required.");
        }
        String sql = "UPDATE donations SET status='CLAIMED', claimed_by=? WHERE id=? AND status='AVAILABLE'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, volunteer.trim());
            ps.setInt(2, id);
            int changed = ps.executeUpdate();
            if (changed == 0) {
                throw new FoodRescueException("Donation is not available or the ID does not exist.");
            }
        }
        notificationQueue.add(id);
        getAllDonations();
    }

    public synchronized void markCollected(int id) throws FoodRescueException, SQLException {
        String sql = "UPDATE donations SET status='COLLECTED' WHERE id=? AND status='CLAIMED'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int changed = ps.executeUpdate();
            if (changed == 0) {
                throw new FoodRescueException("Only a claimed donation can be marked collected.");
            }
        }
        getAllDonations();
    }

    public synchronized void deleteDonation(int id) throws FoodRescueException, SQLException {
        String sql = "DELETE FROM donations WHERE id=? AND status='AVAILABLE'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            int changed = ps.executeUpdate();
            if (changed == 0) {
                throw new FoodRescueException("Only available donations can be deleted.");
            }
        }
        getAllDonations();
    }

    public synchronized List<FoodDonation> getAvailableByDeadline() throws SQLException {
        ArrayList<FoodDonation> available = new ArrayList<>();
        for (FoodDonation d : getAllDonations()) {
            if (d.isAvailable()) available.add(d);
        }
        // Earliest collection deadline appears first.
        available.sort(Comparator.comparing(FoodDonation::getDeadline));
        return available;
    }

    public synchronized String getSummaryUsingCallableStatement() throws SQLException {
        // Requires the stored procedure included in schema.sql.
        StringBuilder result = new StringBuilder();
        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall("{call donation_status_summary()}");
             ResultSet rs = cs.executeQuery()) {
            while (rs.next()) {
                result.append(rs.getString("status")).append(": ")
                      .append(rs.getInt("total")).append(System.lineSeparator());
            }
        }
        return result.toString();
    }

    public synchronized Map<String, Integer> getStatusCounts() throws SQLException {
        getAllDonations();
        return new TreeMap<>(statusCounts);
    }

    public synchronized LinkedList<Integer> getNotificationQueueCopy() {
        return new LinkedList<>(notificationQueue);
    }

    public synchronized Set<String> getFoodNamesSorted() throws SQLException {
        getAllDonations();
        return new TreeSet<>(foodNamesSorted);
    }

    public synchronized Map<Integer, FoodDonation> getCacheCopy() {
        return new HashMap<>(donationCache);
    }

    private FoodDonation mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("deadline");
        return new FoodDonation(
                rs.getInt("id"),
                rs.getString("donor_name"),
                rs.getString("food_name"),
                rs.getDouble("quantity_kg"),
                rs.getString("location"),
                ts.toLocalDateTime(),
                rs.getString("status"),
                rs.getString("claimed_by")
        );
    }

    private void rebuildStatusCounts(List<FoodDonation> rows) {
        statusCounts.clear();
        for (FoodDonation d : rows) {
            statusCounts.put(d.getStatus(), statusCounts.getOrDefault(d.getStatus(), 0) + 1);
        }
    }
}
