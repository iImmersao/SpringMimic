package com.iimmersao.springmimic.database;

public class DriverCheck {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver"); // Force driver loading
        System.out.println("Driver loaded successfully!");
    }
}
