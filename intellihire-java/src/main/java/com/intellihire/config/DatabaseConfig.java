package com.intellihire.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Database configuration — priority order:
 *  1. PostgreSQL  — set DATABASE_URL env var  (Render free PostgreSQL)
 *  2. MySQL       — set MYSQL_URL env var
 *  3. SQLite      — default fallback for local dev
 */
@Configuration
public class DatabaseConfig {

    // PostgreSQL (Render injects DATABASE_URL automatically for linked DBs)
    @Value("${app.datasource.database-url:}")
    private String databaseUrl;

    // MySQL
    @Value("${app.datasource.mysql-url:}")
    private String mysqlUrl;

    @Value("${app.datasource.mysql-user:root}")
    private String mysqlUser;

    @Value("${app.datasource.mysql-password:}")
    private String mysqlPassword;

    // SQLite fallback
    @Value("${app.datasource.sqlite-path:data/intellihire.db}")
    private String sqlitePath;

    // Which DB type are we using? (set after dataSource() runs)
    private String dbType = "sqlite";

    @Bean
    public DataSource dataSource() {
        // 1. PostgreSQL — DATABASE_URL takes priority (Render standard)
        if (databaseUrl != null && !databaseUrl.isBlank()) {
            System.out.println("[DB] Connecting to PostgreSQL (DATABASE_URL)");
            dbType = "postgres";
            // Render uses postgres:// scheme — convert to jdbc:postgresql://
            String jdbcUrl = databaseUrl.replace("postgres://", "jdbc:postgresql://");
            return DataSourceBuilder.create()
                .url(jdbcUrl)
                .driverClassName("org.postgresql.Driver")
                .build();
        }

        // 2. MySQL — MYSQL_URL
        if (mysqlUrl != null && !mysqlUrl.isBlank()) {
            System.out.println("[DB] Connecting to MySQL: " + mysqlUrl);
            dbType = "mysql";
            return DataSourceBuilder.create()
                .url(mysqlUrl)
                .username(mysqlUser)
                .password(mysqlPassword)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
        }

        // 3. SQLite fallback — local dev
        File dbFile = new File(sqlitePath);
        if (dbFile.getParentFile() != null) dbFile.getParentFile().mkdirs();
        System.out.println("[DB] Using SQLite at: " + dbFile.getAbsolutePath());
        dbType = "sqlite";
        return DataSourceBuilder.create()
            .url("jdbc:sqlite:" + sqlitePath)
            .driverClassName("org.sqlite.JDBC")
            .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Initialize schema + seed demo users on first run. */
    @Bean
    public String initDatabase(DataSource ds, BCryptPasswordEncoder encoder) {
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {

            // Auto-detect actual driver if dbType was not set (bean ordering edge case)
            String driverName = conn.getMetaData().getDriverName().toLowerCase();
            boolean isPg     = driverName.contains("postgresql");
            boolean isMysql  = driverName.contains("mysql");

            // users table
            if (isPg) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id VARCHAR(32) PRIMARY KEY,
                        username VARCHAR(100) UNIQUE NOT NULL,
                        password_hash VARCHAR(255) NOT NULL,
                        role VARCHAR(50) NOT NULL,
                        created VARCHAR(50) NOT NULL
                    )""");
            } else {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY,
                        username TEXT UNIQUE NOT NULL,
                        password_hash TEXT NOT NULL,
                        role TEXT NOT NULL,
                        created TEXT NOT NULL
                    )""");
            }

            // candidates table
            if (isPg) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS candidates (
                        id VARCHAR(32) PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        email VARCHAR(255),
                        filename VARCHAR(255),
                        score INTEGER,
                        years INTEGER,
                        top_job VARCHAR(255),
                        top_match INTEGER,
                        data TEXT NOT NULL,
                        created VARCHAR(50) NOT NULL
                    )""");
            } else {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS candidates (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        email TEXT,
                        filename TEXT,
                        score INTEGER,
                        years INTEGER,
                        top_job TEXT,
                        top_match INTEGER,
                        data TEXT NOT NULL,
                        created TEXT NOT NULL
                    )""");
            }

            // custom_jobs table
            if (isPg) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS custom_jobs (
                        id VARCHAR(32) PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        company VARCHAR(255) NOT NULL,
                        location VARCHAR(255) NOT NULL,
                        type VARCHAR(100) NOT NULL,
                        salary VARCHAR(100) NOT NULL,
                        description TEXT NOT NULL,
                        skills TEXT NOT NULL,
                        created VARCHAR(50) NOT NULL
                    )""");
            } else {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS custom_jobs (
                        id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        company TEXT NOT NULL,
                        location TEXT NOT NULL,
                        type TEXT NOT NULL,
                        salary TEXT NOT NULL,
                        description TEXT NOT NULL,
                        skills TEXT NOT NULL,
                        created TEXT NOT NULL
                    )""");
            }

            // Seed demo accounts if users table is empty
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String[][] accounts = {
                    {"recruiter", "recruiter123", "recruiter"},
                    {"candidate", "candidate123", "candidate"}
                };
                for (String[] acc : accounts) {
                    String id = UUID.randomUUID().toString().replace("-","").substring(0, 8);
                    String hash = encoder.encode(acc[1]);
                    stmt.execute(String.format(
                        "INSERT INTO users (id,username,password_hash,role,created) VALUES ('%s','%s','%s','%s','%s')",
                        id, acc[0], hash, acc[2], now));
                }
                System.out.println("[DB] Demo accounts seeded: recruiter/recruiter123, candidate/candidate123");
            }

            System.out.println("[DB] Schema ready. Driver: " + conn.getMetaData().getDriverName());

        } catch (Exception e) {
            System.err.println("[DB] Init error: " + e.getMessage());
            e.printStackTrace();
        }
        return "db-initialized";
    }
}
