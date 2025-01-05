package com.mctg.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:postgresql://localhost:5432/mctg_db";  // Connect to mctg_db
    private static final String USER = "postgres";  // Default user
    private static final String PASSWORD = "dobitedi54";  // Replace with your pgAdmin/Postgres password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
