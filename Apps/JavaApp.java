// Java AWT & Swing classes
import java.awt.*;
import java.awt.event.*;
import java.io.File;

// Java Database connection classes
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * SQLDB: Database connection manager for SQLite
 */
class SQLDB {
    public static Connection conn = null;
    public static Statement stmt = null;
    public static ResultSet rset = null;

    public static void connect(String dbpath) {
        try {
            // Loading SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            // Establishing connection with Database
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbpath);
            // Creating statement for execution of queries
            stmt = conn.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void execute(String query) {
        try {
            // Executing query through statement object
            rset = stmt.executeQuery(query);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int executeUpdate(String query) {
        try {
            return stmt.executeUpdate(query);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}

/**
 * IntelliHireFrame: The candidate management window opened after successful login
 * Implements Frames, Event handling, and Database connectivity as required by the lab.
 */
class IntelliHireFrame extends WindowAdapter implements ActionListener {
    Frame f = new Frame("IntelliHire AI — Recruiter Dashboard");
    Label lblTitle = new Label("IntelliHire AI Talent Management", Label.CENTER);
    Label lblStatus = new Label("Logged in to javaapp.db | Candidate Records", Label.CENTER);

    TextArea txtCandidates = new TextArea(10, 50);

    Panel pnlInputs = new Panel();
    Label lblName = new Label("Name:");
    TextField txtName = new TextField(12);
    Label lblScore = new Label("Score:");
    TextField txtScore = new TextField(4);
    Label lblSkills = new Label("Key Skills:");
    TextField txtSkills = new TextField(16);
    Label lblJob = new Label("Role Match:");
    TextField txtJob = new TextField(12);

    Panel pnlButtons = new Panel();
    Button btnLoad = new Button("Load Candidates");
    Button btnAdd = new Button("Add Candidate");
    Button btnClear = new Button("Clear Form");

    IntelliHireFrame() {
        f.setTitle("IntelliHire AI — Candidate Evaluation System");
        f.setSize(620, 520);
        f.setLayout(new BorderLayout(10, 10));

        // Header Panel
        Panel pnlHeader = new Panel(new GridLayout(2, 1));
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        lblStatus.setForeground(Color.DARK_GRAY);
        pnlHeader.add(lblTitle);
        pnlHeader.add(lblStatus);
        f.add(pnlHeader, BorderLayout.NORTH);

        // Center: Candidate text area
        txtCandidates.setEditable(false);
        txtCandidates.setFont(new Font("Monospaced", Font.PLAIN, 12));
        f.add(txtCandidates, BorderLayout.CENTER);

        // South: Input controls and Action buttons
        Panel pnlSouth = new Panel(new GridLayout(2, 1, 6, 6));

        pnlInputs.setLayout(new FlowLayout());
        pnlInputs.add(lblName);
        pnlInputs.add(txtName);
        pnlInputs.add(lblScore);
        pnlInputs.add(txtScore);
        pnlInputs.add(lblSkills);
        pnlInputs.add(txtSkills);
        pnlInputs.add(lblJob);
        pnlInputs.add(txtJob);

        pnlButtons.setLayout(new FlowLayout());
        btnLoad.addActionListener(this);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        pnlButtons.add(btnLoad);
        pnlButtons.add(btnAdd);
        pnlButtons.add(btnClear);

        pnlSouth.add(pnlInputs);
        pnlSouth.add(pnlButtons);
        f.add(pnlSouth, BorderLayout.SOUTH);

        f.addWindowListener(this);
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        // Automatically load candidates from database
        loadCandidateData();
    }

    private void loadCandidateData() {
        try {
            txtCandidates.setText("========================================================================\n");
            txtCandidates.append("ID   | CANDIDATE NAME      | SCORE | ROLE MATCH          | SKILLS\n");
            txtCandidates.append("========================================================================\n");

            SQLDB.execute("SELECT * FROM candidates");
            while (SQLDB.rset != null && SQLDB.rset.next()) {
                String id = SQLDB.rset.getString("id");
                String name = SQLDB.rset.getString("name");
                int score = SQLDB.rset.getInt("score");
                String job = SQLDB.rset.getString("top_job");
                String skills = SQLDB.rset.getString("skills");

                String line = String.format("%-4s | %-19s | %-5d | %-19s | %s\n", id, name, score, job, skills);
                txtCandidates.append(line);
            }
            txtCandidates.append("========================================================================\n");
        } catch (Exception e) {
            txtCandidates.setText("Error reading candidates: " + e.getMessage());
        }
    }

    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals("Load Candidates")) {
            loadCandidateData();
        } else if (cmd.equals("Add Candidate")) {
            String name = txtName.getText().trim();
            String scoreStr = txtScore.getText().trim();
            String skills = txtSkills.getText().trim();
            String job = txtJob.getText().trim();

            if (name.isEmpty() || scoreStr.isEmpty()) {
                lblStatus.setText("Please enter candidate Name and Score.");
                return;
            }

            try {
                int score = Integer.parseInt(scoreStr);
                String newId = String.valueOf(System.currentTimeMillis() % 10000);
                String q = "INSERT INTO candidates VALUES ('" + newId + "', '" + name + "', " + score + ", '" + skills + "', '" + job + "', " + score + ");";
                SQLDB.executeUpdate(q);
                lblStatus.setText("Added candidate: " + name);
                txtName.setText("");
                txtScore.setText("");
                txtSkills.setText("");
                txtJob.setText("");
                loadCandidateData();
            } catch (Exception ex) {
                lblStatus.setText("Error adding candidate: " + ex.getMessage());
            }
        } else if (cmd.equals("Clear Form")) {
            txtName.setText("");
            txtScore.setText("");
            txtSkills.setText("");
            txtJob.setText("");
            lblStatus.setText("Form cleared.");
        }
    }

    public void windowClosing(WindowEvent we) {
        f.dispose();
    }
}

/**
 * Login form class as required in lab specifications
 */
class Login extends WindowAdapter implements ActionListener {
    // Creating frame
    Frame f = new Frame("Login");

    // Creating frame components
    Label l1 = new Label("User Name");
    Label l2 = new Label("Password");
    TextField t1 = new TextField();
    TextField t2 = new TextField();
    Button b1 = new Button("Login");
    Button b2 = new Button("Reset");
    Dialog d = new Dialog(f, "Login", true);

    // Dynamic resolution of javaapp.db location
    private String resolveDbPath() {
        // 1. Check current directory
        File local = new File("javaapp.db");
        if (local.exists()) return local.getAbsolutePath();
        // 2. Check Apps folder relative to current working directory
        File apps = new File("Apps/javaapp.db");
        if (apps.exists()) return apps.getAbsolutePath();
        // 3. Fallback to default specified in lab prompt
        return "C:\\Users\\SRMAP\\Desktop\\Apps\\javaapp.db";
    }

    // Constructor to configure window frame
    Login() {
        // Configuring Window frame
        f.setTitle("Login"); // Setting window title
        f.setSize(300, 300); // Setting window size
        f.setLayout(new GridLayout(3, 2, 8, 8)); // Setting Grid layout

        t2.setEchoChar('*'); // Hide password characters

        // Adding components to frame
        f.add(l1);
        f.add(t1);
        f.add(l2);
        f.add(t2);
        f.add(b1);
        f.add(b2);

        // Adding action listener to buttons
        b1.addActionListener(this);
        b2.addActionListener(this);

        // Adding window listener to frame
        f.addWindowListener(this);
        f.setLocationRelativeTo(null);
        f.setVisible(true); // Displaying window frame
    }

    // Handling Button events
    public void actionPerformed(ActionEvent ae) {
        // Handling Login button event
        System.out.println(ae.getActionCommand());
        if (ae.getActionCommand().equals("Login")) {
            // Connect to Database
            String dbPath = resolveDbPath();
            System.out.println("Connecting to database at: " + dbPath);
            SQLDB.connect(dbPath);

            // Execute SQL query
            String q = "select * from users";
            q = q + " where uname='" + t1.getText() + "' and pwd='" + t2.getText() + "';";
            SQLDB.execute(q);

            // Fetching results of query
            String msg = "";
            boolean success = false;
            try {
                if (SQLDB.rset != null && SQLDB.rset.next()) {
                    msg = "Login success...";
                    success = true;
                } else {
                    msg = "Invalid user name or password";
                }
            } catch (Exception e1) {
                e1.printStackTrace();
                msg = "Database Error: " + e1.getMessage();
            }

            // Displaying Dialog box
            d.removeAll();
            d.setTitle("Login");
            d.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    d.dispose();
                }
            });
            d.setLayout(new BorderLayout());
            d.add(new Label(msg, Label.CENTER), BorderLayout.CENTER);

            Button btnOk = new Button("OK");
            boolean finalSuccess = success;
            btnOk.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    d.dispose();
                    if (finalSuccess) {
                        f.dispose(); // Close login window
                        new IntelliHireFrame(); // Open application window
                    }
                }
            });
            d.add(btnOk, BorderLayout.SOUTH);
            d.setLocationRelativeTo(f);
            d.setSize(300, 120);
            d.setVisible(true);
        }
        // Handling Reset button event
        else if (ae.getActionCommand().equals("Reset")) {
            t1.setText("");
            t2.setText("");
        }
    }

    // Handling window closing events
    public void windowClosing(WindowEvent we) {
        f.dispose();
        System.exit(0);
    }
}

// Driver Class
public class JavaApp {
    // main function
    public static void main(String[] args) {
        // Creating Login class object
        new Login();
    }
}
