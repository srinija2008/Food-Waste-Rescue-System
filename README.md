# Food Waste Rescue and Donation System (Java Swing + JDBC + MySQL)

## Requirements
- JDK 17 or newer recommended
- MySQL Server
- MySQL Connector/J JAR

## Setup
1. Open MySQL Workbench.
2. Run `schema.sql`.
3. Open `src/foodwasterescue/DBConnection.java`.
4. Change `PASSWORD = "your_password"` to your own MySQL password.
5. Add MySQL Connector/J to the project's classpath.
6. Compile and run `foodwasterescue.FoodWasteRescueApp`.

### Example command-line setup (adjust the connector JAR path)
Windows:
```bat
javac -cp ".;mysql-connector-j-9.x.x.jar" -d out src\foodwasterescue\*.java
java -cp "out;mysql-connector-j-9.x.x.jar" foodwasterescue.FoodWasteRescueApp
```

Linux/macOS:
```bash
javac -cp ".:mysql-connector-j-9.x.x.jar" -d out src/foodwasterescue/*.java
java -cp "out:mysql-connector-j-9.x.x.jar" foodwasterescue.FoodWasteRescueApp
```

## GUI workflow
1. Enter donor, food, quantity in kg, location, and future deadline.
2. Click Add Donation.
3. Select a row and use Claim Selected to assign it to a volunteer.
4. After collection, select the row and click Mark Collected.
5. Export CSV or create a serialized backup.
6. The deadline monitor checks every minute for available donations due within one hour.

## Notes
- This is an educational prototype. It does not verify food safety or guarantee actual pickup.
- The deadline monitor is a background thread. It displays deadline alerts in the GUI.
- MySQL is the primary data store; file streams are used for report export, activity logs, and backup.
- The `CallableStatement` report button requires the stored procedure in `schema.sql`.
- For real deployment, add authentication, authorization, input sanitation, food safety checks, and transaction-based booking/claim safeguards.
