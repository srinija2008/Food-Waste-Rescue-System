package foodwasterescue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public final class FileManager {
    private FileManager() {}

    // FileWriter / FileReader: human-readable CSV report.
    public static void exportCsv(List<FoodDonation> donations, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            writer.write("ID,Donor,Food,QuantityKg,Location,Deadline,Status,ClaimedBy\n");
            for (FoodDonation d : donations) {
                writer.write(d.getReportLine());
                writer.write(System.lineSeparator());
            }
        }
    }

    public static String readTextFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            char[] buffer = new char[1024];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                content.append(buffer, 0, count);
            }
        }
        return content.toString();
    }

    // FileOutputStream / FileInputStream: binary serialized backup.
    public static void backup(List<FoodDonation> donations, File file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(new java.util.ArrayList<>(donations));
        }
    }

    @SuppressWarnings("unchecked")
    public static List<FoodDonation> restoreBackup(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = in.readObject();
            if (!(obj instanceof List<?>)) throw new IOException("Backup file has an invalid format.");
            return (List<FoodDonation>) obj;
        }
    }

    public static void appendActivity(String message) throws IOException {
        Path path = Paths.get("activity.log");
        Files.write(path, (java.time.LocalDateTime.now() + " - " + message + System.lineSeparator())
                .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
