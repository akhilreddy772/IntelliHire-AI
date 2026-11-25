package com.intellihire.repository;

import com.intellihire.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** CRUD operations for the users table. */
@Repository
public class UserRepository {

    @Autowired private JdbcTemplate jdbc;

    private final RowMapper<User> ROW_MAPPER = (rs, row) -> {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setCreated(rs.getString("created"));
        return u;
    };

    public void save(User u) {
        jdbc.update(
            "INSERT INTO users (id,username,password_hash,role,created) VALUES (?,?,?,?,?)",
            u.getId(), u.getUsername(), u.getPasswordHash(), u.getRole(), u.getCreated()
        );
    }

    public Optional<User> findByUsername(String username) {
        List<User> list = jdbc.query(
            "SELECT * FROM users WHERE username=?", ROW_MAPPER, username);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<User> findById(String id) {
        List<User> list = jdbc.query(
            "SELECT * FROM users WHERE id=?", ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public boolean existsByUsername(String username) {
        Integer cnt = jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username=?", Integer.class, username);
        return cnt != null && cnt > 0;
    }
}
