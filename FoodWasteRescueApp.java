package foodwasterescue;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class FoodWasteRescueApp extends JFrame {
    private final DonationService service = new DonationService();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Donor", "Food", "Quantity (kg)", "Location", "Deadline", "Status", "Claimed By"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextArea alerts = new JTextArea(5, 40);
    private final DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private DeadlineMonitor monitor;

    public FoodWasteRescueApp() {
        super("Food Waste Rescue and Donation System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 680);
        setLocationRelativeTo(null);
        buildGui();
        startDeadlineMonitor();
        refreshTable();
    }

    private void buildGui() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Donations", buildDonationPanel());
        tabs.addTab("Reports & Tools", buildReportsPanel());
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                if (monitor != null) monitor.close();
            }
        });
    }

    private JPanel buildDonationPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("Register Surplus Food"));

        JTextField donorField = new JTextField();
        JTextField foodField = new JTextField();
        JTextField quantityField = new JTextField();
        JTextField locationField = new JTextField();
        JTextField deadlineField = new JTextField(LocalDateTime.now().plusHours(3).format(inputFormat));

        form.add(new JLabel("Donor / canteen name:")); form.add(donorField);
        form.add(new JLabel("Food name:")); form.add(foodField);
        form.add(new JLabel("Quantity (kg):")); form.add(quantityField);
        form.add(new JLabel("Collection location:")); form.add(locationField);
        form.add(new JLabel("Deadline (yyyy-MM-dd HH:mm):")); form.add(deadlineField);

        JButton addButton = new JButton("Add Donation");
        addButton.addActionListener(e -> {
            try {
                double quantity = Double.parseDouble(quantityField.getText().trim());
                LocalDateTime deadline = LocalDateTime.parse(deadlineField.getText().trim(), inputFormat);
                service.addDonation(donorField.getText(), foodField.getText(), quantity,
                        locationField.getText(), deadline);
                FileManager.appendActivity("Added donation: " + foodField.getText().trim());
                JOptionPane.showMessageDialog(this, "Donation added successfully.");
                donorField.setText(""); foodField.setText(""); quantityField.setText(""); locationField.setText("");
                refreshTable();
            } catch (NumberFormatException ex) {
                showError("Quantity must be a number, for example 5 or 2.5.");
            } catch (java.time.format.DateTimeParseException ex) {
                showError("Enter deadline in yyyy-MM-dd HH:mm format.");
            } catch (FoodRescueException | SQLException | IOException ex) {
                showError(ex.getMessage());
            } finally {
                // Demonstrates finally: this block runs after success or failure.
                quantityField.setToolTipText("Enter quantity in kilograms.");
            }
        });

        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.add(form, BorderLayout.CENTER);
        formWrapper.add(addButton, BorderLayout.SOUTH);

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Food Donations (earliest deadline first)"));

        JButton refreshButton = new JButton("Refresh");
        JButton claimButton = new JButton("Claim Selected");
        JButton collectedButton = new JButton("Mark Collected");
        JButton deleteButton = new JButton("Delete Available");
        JButton exportButton = new JButton("Export CSV");
        JButton backupButton = new JButton("Backup File");

        refreshButton.addActionListener(e -> refreshTable());
        claimButton.addActionListener(e -> claimSelected());
        collectedButton.addActionListener(e -> markCollectedSelected());
        deleteButton.addActionListener(e -> deleteSelected());
        exportButton.addActionListener(e -> exportCsv());
        backupButton.addActionListener(e -> backup());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(refreshButton); buttons.add(claimButton); buttons.add(collectedButton);
        buttons.add(deleteButton); buttons.add(exportButton); buttons.add(backupButton);

        alerts.setEditable(false);
        alerts.setLineWrap(true);
        alerts.setWrapStyleWord(true);
        JScrollPane alertScroll = new JScrollPane(alerts);
        alertScroll.setBorder(BorderFactory.createTitledBorder("Deadline Alerts"));

        panel.add(formWrapper, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        panel.add(alertScroll, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildReportsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        JButton summaryButton = new JButton("Show Status Summary (CallableStatement)");
        JButton foodButton = new JButton("Show Sorted Food Names (TreeSet)");
        JButton backupRestoreInfo = new JButton("Show Backup Instructions");
        panel.add(summaryButton);
        panel.add(foodButton);
        panel.add(backupRestoreInfo);

        summaryButton.addActionListener(e -> {
            try {
                String summary = service.getSummaryUsingCallableStatement();
                JOptionPane.showMessageDialog(this, summary.isEmpty() ? "No donations found." : summary,
                        "Donation Status Summary", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                showError("Could not call stored procedure. Check schema.sql and MySQL setup.\n" + ex.getMessage());
            }
        });

        foodButton.addActionListener(e -> {
            try {
                JOptionPane.showMessageDialog(this, service.getFoodNamesSorted().toString(),
                        "Sorted Food Names", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) { showError(ex.getMessage()); }
        });

        backupRestoreInfo.addActionListener(e -> {
            try {
                List<FoodDonation> restored = FileManager.restoreBackup(new File("donations-backup.ser"));
                JOptionPane.showMessageDialog(this, "Backup contains " + restored.size() + " donation(s).");
            } catch (IOException | ClassNotFoundException ex) {
                showError("Create a backup first using the Backup File button.\n" + ex.getMessage());
            }
        });
        return panel;
    }

    private void refreshTable() {
        try {
            List<FoodDonation> donations = service.getAllDonations();
            donations.sort(java.util.Comparator.comparing(FoodDonation::getDeadline));
            tableModel.setRowCount(0);
            for (FoodDonation d : donations) {
                tableModel.addRow(new Object[]{d.getId(), d.getDonorName(), d.getFoodName(),
                        d.getQuantityKg(), d.getLocation(), d.getDeadline().format(inputFormat),
                        d.getStatus(), d.getClaimedBy() == null ? "" : d.getClaimedBy()});
            }
        } catch (SQLException ex) {
            showError("Could not load donations. Check MySQL connection and schema.sql.\n" + ex.getMessage());
        }
    }

    private int selectedDonationId() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Select a donation row first.");
            return -1;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return Integer.parseInt(tableModel.getValueAt(modelRow, 0).toString());
    }

    private void claimSelected() {
        int id = selectedDonationId();
        if (id < 0) return;
        String volunteer = JOptionPane.showInputDialog(this, "Enter volunteer / organization name:");
        if (volunteer == null) return;
        try {
            service.claimDonation(id, volunteer);
            FileManager.appendActivity("Claimed donation ID " + id + " by " + volunteer);
            refreshTable();
        } catch (FoodRescueException | SQLException | IOException ex) { showError(ex.getMessage()); }
    }

    private void markCollectedSelected() {
        int id = selectedDonationId();
        if (id < 0) return;
        try {
            service.markCollected(id);
            FileManager.appendActivity("Marked donation ID " + id + " as collected");
            refreshTable();
        } catch (FoodRescueException | SQLException | IOException ex) { showError(ex.getMessage()); }
    }

    private void deleteSelected() {
        int id = selectedDonationId();
        if (id < 0) return;
        int choice = JOptionPane.showConfirmDialog(this, "Delete available donation ID " + id + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            service.deleteDonation(id);
            FileManager.appendActivity("Deleted donation ID " + id);
            refreshTable();
        } catch (FoodRescueException | SQLException | IOException ex) { showError(ex.getMessage()); }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("donation-report.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            FileManager.exportCsv(service.getAllDonations(), chooser.getSelectedFile());
            JOptionPane.showMessageDialog(this, "CSV report exported successfully.");
        } catch (IOException | SQLException ex) { showError(ex.getMessage()); }
    }

    private void backup() {
        try {
            FileManager.backup(service.getAllDonations(), new File("donations-backup.ser"));
            JOptionPane.showMessageDialog(this, "Backup saved as donations-backup.ser");
        } catch (IOException | SQLException ex) { showError(ex.getMessage()); }
    }

    private void startDeadlineMonitor() {
        monitor = new DeadlineMonitor(service, message -> SwingUtilities.invokeLater(() -> {
            alerts.append(message + System.lineSeparator());
        }));
        monitor.start();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Food Rescue System", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoodWasteRescueApp().setVisible(true));
    }
}
