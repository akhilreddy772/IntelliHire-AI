"""IntelliHire AI - SQLite database initializer for the Java App component."""
import sqlite3
import os

db_path = os.path.join(os.path.dirname(__file__), "javaapp.db")
conn = sqlite3.connect(db_path)
cur = conn.cursor()

# Users table (as required by lab specification - srm/ap credentials)
cur.execute("CREATE TABLE IF NOT EXISTS users (uname text, pwd text);")
cur.execute("DELETE FROM users;")
cur.executemany("INSERT INTO users VALUES (?, ?);", [
    ("srm", "ap"),
    ("recruiter", "recruiter123"),
    ("candidate", "candidate123"),
])

# IntelliHire candidates table
cur.execute("""CREATE TABLE IF NOT EXISTS candidates (
    id text primary key,
    name text,
    score int,
    skills text,
    top_job text,
    top_match int
);""")
cur.execute("DELETE FROM candidates;")
cur.executemany("INSERT INTO candidates VALUES (?, ?, ?, ?, ?, ?);", [
    ("1", "Priya Sharma",  88, "Python, SQL, Flask, ML, Docker",           "Python Backend Developer", 92),
    ("2", "Alex Johnson",  82, "React, Node.js, TypeScript, Docker",        "Full Stack Engineer",      85),
    ("3", "Rohan Mehta",   76, "Java, Spring Boot, MySQL, REST API",        "Java Developer",           80),
    ("4", "Sara Ali",      71, "Machine Learning, TensorFlow, NLP, Python", "ML Engineer",              74),
    ("5", "David Kim",     67, "AWS, Docker, Kubernetes, Linux, Terraform", "DevOps Engineer",          70),
])

conn.commit()
conn.close()
print("javaapp.db initialized at:", db_path)
