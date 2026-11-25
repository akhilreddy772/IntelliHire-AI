package com.intellihire.repository;

import com.intellihire.model.Candidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.*;

/** CRUD operations for the candidates table. */
@Repository
public class CandidateRepository {

    @Autowired private JdbcTemplate jdbc;

    private final RowMapper<Candidate> ROW_MAPPER = (rs, row) -> {
        Candidate c = new Candidate();
        c.setId(rs.getString("id"));
        c.setName(rs.getString("name"));
        c.setEmail(rs.getString("email"));
        c.setFilename(rs.getString("filename"));
        c.setScore(rs.getInt("score"));
        c.setYears(rs.getInt("years"));
        c.setTopJob(rs.getString("top_job"));
        c.setTopMatch(rs.getInt("top_match"));
        c.setData(rs.getString("data"));
        c.setCreated(rs.getString("created"));
        return c;
    };

    public void save(Candidate c) {
        jdbc.update(
            "INSERT INTO candidates (id,name,email,filename,score,years,top_job,top_match,data,created) VALUES (?,?,?,?,?,?,?,?,?,?)",
            c.getId(), c.getName(), c.getEmail(), c.getFilename(),
            c.getScore(), c.getYears(), c.getTopJob(), c.getTopMatch(),
            c.getData(), c.getCreated()
        );
    }

    public Optional<Candidate> findById(String id) {
        List<Candidate> list = jdbc.query(
            "SELECT * FROM candidates WHERE id=?", ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Candidate> findAll() {
        return jdbc.query("SELECT * FROM candidates ORDER BY created DESC", ROW_MAPPER);
    }

    public void deleteById(String id) {
        jdbc.update("DELETE FROM candidates WHERE id=?", id);
    }

    public int count() {
        Integer cnt = jdbc.queryForObject("SELECT COUNT(*) FROM candidates", Integer.class);
        return cnt != null ? cnt : 0;
    }

    public double averageScore() {
        Double avg = jdbc.queryForObject("SELECT AVG(score) FROM candidates", Double.class);
        return avg != null ? avg : 0.0;
    }
}
