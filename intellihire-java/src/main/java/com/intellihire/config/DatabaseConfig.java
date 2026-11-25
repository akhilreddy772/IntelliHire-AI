package com.intellihire.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Database configuration — SQLite default, MySQL if MYSQL_URL is set.
 * Creates tables and seeds demo accounts on first run.
 */
@Configuration
public class DatabaseConfig {

    @Value("${app.datasource.sqlite-path:data/intellihire.db}")
    private String sqlitePath;

    @Value("${app.datasource.mysql-url:}")
    private String mysqlUrl;

    @Value("${app.datasource.mysql-user:root}")
    private String mysqlUser;

    @Value("${app.datasource.mysql-password:}")
    private String mysqlPassword;

    @Bean
    public DataSource dataSource() {
        // Try MySQL first if configured
        if (mysqlUrl != null && !mysqlUrl.isBlank()) {
            System.out.println("[DB] Connecting to MySQL: " + mysqlUrl);
            return DataSourceBuilder.create()
                .url(mysqlUrl)
                .username(mysqlUser)
                .password(mysqlPassword)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
        }

        // SQLite fallback — ensure parent directory exists
        File dbFile = new File(sqlitePath);
        dbFile.getParentFile().mkdirs();

        System.out.println("[DB] Using SQLite at: " + dbFile.getAbsolutePath());
        return DataSourceBuilder.create()
            .url("jdbc:sqlite:" + sqlitePath)
            .driverClassName("org.sqlite.JDBC")
            .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Initialize schema + seed users after the DataSource is ready. */
    @Bean
    public String initDatabase(DataSource ds, BCryptPasswordEncoder encoder) {
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {

            // users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL,
                    created TEXT NOT NULL
                )""");

            // candidates table
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

            // custom_jobs table
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

            // Seed demo accounts
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                String[][] accounts = {
                    {"recruiter", "recruiter123", "recruiter"},
                    {"candidate", "candidate123", "candidate"}
                };
                for (String[] acc : accounts) {
                    String id = UUID.randomUUID().toString().replace("-","").substring(0,8);
                    String hash = encoder.encode(acc[1]);
                    stmt.execute(String.format(
                        "INSERT INTO users VALUES ('%s','%s','%s','%s','%s')",
                        id, acc[0], hash, acc[2], now));
                }
                System.out.println("[DB] Demo accounts seeded: recruiter/recruiter123, candidate/candidate123");
            }

        } catch (Exception e) {
            System.err.println("[DB] Init error: " + e.getMessage());
        }
        return "db-initialized";
    }
}
