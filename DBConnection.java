package foodwasterescue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/food_rescue_db";
    private static final String USER = "root";
    // Change this to your own MySQL password.
    private static final String PASSWORD = "your_password";

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
