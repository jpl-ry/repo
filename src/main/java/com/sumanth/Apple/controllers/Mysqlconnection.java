package com.sumanth.Apple.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Mysqlconnection {

    // Change these as per your DB setup
    private static final String DB_URL = "jdbc:mysql://localhost:3306/users";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Root@123";

    public static void main(String[] args) {
        // Sample data
        String firstName = "sumanth2134r";
        String lastName = "Vishvanathula";
        String email = "sumath1.sm2aitd2h@example.com";
        String hireDate = "2024-06-01";
        double salary = 23332.00;

        // SQL Insert statement
        String sql = "INSERT INTO employees (first_name, last_name, email, hire_date, salary) VALUES (?, ?, ?, ?, ?)";

        try (
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, hireDate);
            stmt.setDouble(5, salary);

            int rowsInserted = stmt.executeUpdate();
            System.out.println(rowsInserted + " row(s) inserted.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
