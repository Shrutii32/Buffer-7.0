import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

class DBConnection {

    private static Connection conn = null;

    private static final String URL = "jdbc:mysql://localhost:3306/buffer";
    private static final String USER = "root";
    private static final String PASS = "root";

    private DBConnection() {
    }

    // GLOBAL ACCESS POINT
    public static Connection getConnection() {

        try {
            if (conn == null || conn.isClosed()) {

                Class.forName("com.mysql.cj.jdbc.Driver");

                conn = DriverManager.getConnection(URL, USER, PASS);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return conn;
    }
}

class Agent {

    int agentId;
    int deptId;
    int workload;
    boolean available;
    Integer supervisorId;

    Connection conn;

    Map<Integer, List<Agent>> deptAgentMap = new HashMap<>();
    Map<Integer, Agent> supervisorMap = new HashMap<>();

    public Agent() {

    }

    public Agent(Connection conn) {
        this.conn = conn;
    }

    // ---------------- LOAD AGENTS ----------------
    public void loadAgents() {

        try {
            String query = "SELECT * FROM agent_table";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Agent a = new Agent();
                a.agentId = rs.getInt("agentID");
                a.deptId = rs.getInt("deptID");
                a.workload = rs.getInt("workload");
                a.available = rs.getBoolean("availability");

                int sup = rs.getInt("supervisor_id");
                a.supervisorId = rs.wasNull() ? null : sup;

                if (!deptAgentMap.containsKey(a.deptId)) {
                    deptAgentMap.put(a.deptId, new ArrayList<>());
                }

                deptAgentMap.get(a.deptId).add(a);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- LOAD SUPERVISORS ----------------
    public void loadSupervisors() {

        for (Map.Entry<Integer, List<Agent>> entry : deptAgentMap.entrySet()) {

            int deptId = entry.getKey();

            for (Agent a : entry.getValue()) {

                if (a.supervisorId == null) {
                    supervisorMap.put(deptId, a);
                    break;
                }
            }
        }
    }

    // ---------------- FIND BEST AGENT ----------------
    public Agent findBestAgent(int deptId) {

        List<Agent> agents = deptAgentMap.get(deptId);

        if (agents == null)
            return null;

        Agent best = null;
        int minWorkload = Integer.MAX_VALUE;

        for (Agent a : agents) {

            if (a.available && a.workload < minWorkload) {
                minWorkload = a.workload;
                best = a;
            }
        }

        return best;
    }

    // ---------------- FIND SUPERVISOR ----------------
    public Agent findSupervisor(int deptId) {

        return supervisorMap.get(deptId);
    }
}

class Admin {

    Connection conn;
    Agent agentService;

    Admin() {
    }

    public Admin(Connection conn) {
        this.conn = conn;
        this.agentService = new Agent(conn);

        // 🔥 LOAD DATA HERE
        agentService.loadAgents();
        agentService.loadSupervisors();
    }

    double calculatePriority(Ticket t) {
        double urgency;

        if (t.timeWindow == 0) {
            urgency = 0; // no deadline → no urgency boost
        } else {
            urgency = 10.0 / (t.timeWindow + 1); // time already in minutes
        }
        double ageScore = t.age;
        double customerScore = t.modelType * 2.5;

        return (0.5 * urgency) +
                (0.3 * ageScore) +
                (0.2 * customerScore);
    }

    HashMap<Integer, PriorityQueue<Ticket>> prioritize() {
        HashMap<Integer, ArrayList<Ticket>> categoryMap = Allocation.buildBuffer();
        HashMap<Integer, PriorityQueue<Ticket>> pqMap = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<Ticket>> entry : categoryMap.entrySet()) {

            int deptId = entry.getKey();
            List<Ticket> tickets = entry.getValue();

            PriorityQueue<Ticket> pq = new PriorityQueue<>(
                    (a, b) -> Double.compare(b.priorityScore, a.priorityScore));

            for (Ticket t : tickets) {

                t.priorityScore = calculatePriority(t);
                pq.add(t);
            }

            pqMap.put(deptId, pq);
        }
        return pqMap;

    }

    public void allocate(Map<Integer, PriorityQueue<Ticket>> pqMap) {

        for (Map.Entry<Integer, PriorityQueue<Ticket>> entry : pqMap.entrySet()) {

            int deptId = entry.getKey();
            PriorityQueue<Ticket> pq = entry.getValue();

            while (!pq.isEmpty()) {

                Ticket t = pq.poll();
                boolean isAllocated = false;

                Agent bestAgent = agentService.findBestAgent(deptId);

                try {

                    // ---------------- ALLOCATION ----------------
                    if (bestAgent != null) {

                        conn.setAutoCommit(false);

                        bestAgent.workload++;

                        String insert = "INSERT INTO Allocated(ticket_Id, cust_userName, agentid, status) VALUES (?, ?, ?, ?)";
                        PreparedStatement ps1 = conn.prepareStatement(insert);
                        ps1.setInt(1, t.ticketId);
                        ps1.setString(2, t.username);
                        ps1.setInt(3, bestAgent.agentId);
                        ps1.setBoolean(4, true);
                        ps1.executeUpdate();

                        String delete = "DELETE FROM not_allocated WHERE ticket_id = ?";
                        PreparedStatement ps2 = conn.prepareStatement(delete);
                        ps2.setInt(1, t.ticketId);
                        ps2.executeUpdate();

                        conn.commit();

                        t.status = "ALLOCATED";
                        isAllocated = true;
                    }

                    // ---------------- SUPERVISOR ----------------
                    else if (t.priorityScore >= 7.0) {

                        Agent supervisor = agentService.findSupervisor(deptId);

                        if (supervisor != null) {

                            conn.setAutoCommit(false);

                            supervisor.workload++;

                            String insert = "INSERT INTO Allocated(ticket_Id, cust_userName, agentid, status) VALUES (?, ?, ?, ?)";
                            PreparedStatement ps1 = conn.prepareStatement(insert);
                            ps1.setInt(1, t.ticketId);
                            ps1.setString(2, t.username);
                            ps1.setInt(3, supervisor.agentId);
                            ps1.setBoolean(4, true);
                            ps1.executeUpdate();

                            String delete = "DELETE FROM ticket WHERE ticket_id = ?";
                            PreparedStatement ps2 = conn.prepareStatement(delete);
                            ps2.setInt(1, t.ticketId);
                            ps2.executeUpdate();

                            conn.commit();

                            t.status = "ALLOCATED";
                            isAllocated = true;
                        }
                    }

                    // ---------------- PENDING ----------------
                    if (!isAllocated) {

                        String update = "UPDATE ticket SET age = age + 1, status = 'PENDING' WHERE ticket_id = ?";
                        PreparedStatement ps = conn.prepareStatement(update);
                        ps.setInt(1, t.ticketId);
                        ps.executeUpdate();

                        t.age++;
                        t.status = "PENDING";
                    }

                } catch (Exception e) {
                    try {
                        conn.rollback();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                }
            }
        }
    }

}

public class Allocation {

    // EDIT DISTANCE (DP)
    public static int editDistance(String s1, String s2) {
        int n = s1.length(), m = s2.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++)
            dp[i][0] = i;
        for (int j = 0; j <= m; j++)
            dp[0][j] = j;

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(
                            dp[i - 1][j],
                            Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
                }
            }
        }
        return dp[n][m];
    }

    // DEPARTMENT DETECTION (WITH WEIGHTS + PRIORITY)
    public static int getDept(String desc) {
        desc = desc.toLowerCase();
        String[] words = desc.split(" ");

        HashMap<Integer, String[]> deptKeywords = new HashMap<>();

        deptKeywords.put(1, new String[] { "payment", "transaction", "gateway", "upi", "failed" }); // fintech
        deptKeywords.put(2, new String[] { "refund", "billing", "charged", "invoice", "deducted" }); // finance
        deptKeywords.put(3, new String[] { "error", "bug", "crash", "login", "server", "notworking" }); // tech
        deptKeywords.put(4, new String[] { "delivery", "late", "shipping", "courier", "delayed" }); // delivery
        deptKeywords.put(5, new String[] { "order", "cancel", "update", "status", "change" }); // operations

        HashMap<Integer, HashSet<String>> strongWords = new HashMap<>();

        strongWords.put(1, new HashSet<>(Arrays.asList("failed", "upi")));
        strongWords.put(2, new HashSet<>(Arrays.asList("refund", "charged", "deducted")));
        strongWords.put(3, new HashSet<>(Arrays.asList("crash", "error")));
        strongWords.put(4, new HashSet<>(Arrays.asList("late", "delayed")));
        strongWords.put(5, new HashSet<>(Arrays.asList("cancel")));

        HashMap<Integer, Integer> score = new HashMap<>();
        for (int dept : deptKeywords.keySet()) {
            score.put(dept, 0);
        }

        boolean techFlag = false;
        boolean financeStrongFlag = false;

        for (String word : words) {
            for (int dept : deptKeywords.keySet()) {
                for (String key : deptKeywords.get(dept)) {

                    if (Math.abs(word.length() - key.length()) <= 2 &&
                            editDistance(word, key) <= 2 ||
                            word.startsWith(key)) {

                        if (strongWords.get(dept).contains(key)) {
                            score.put(dept, score.get(dept) + 3);
                        } else {
                            score.put(dept, score.get(dept) + 1);
                        }

                        if (dept == 3)
                            techFlag = true;
                        if (dept == 2 && strongWords.get(2).contains(key)) {
                            financeStrongFlag = true;
                        }
                    }
                }
            }
        }

        // PRIORITY RULES
        if (techFlag && score.get(3) >= 2)
            return 3;
        if (financeStrongFlag)
            return 2;

        int bestDept = 5;
        int maxScore = 0;

        for (int dept : score.keySet()) {
            if (score.get(dept) > maxScore) {
                maxScore = score.get(dept);
                bestDept = dept;
            }
        }

        return bestDept;
    }

    // build map (MAIN FUNCTION)
    public static HashMap<Integer, ArrayList<Ticket>> buildBuffer() {

        HashMap<Integer, ArrayList<Ticket>> bufferMap = new HashMap<>();

        // initialize lists for 5 departments
        for (int i = 1; i <= 5; i++) {
            bufferMap.put(i, new ArrayList<>());
        }

        String url = "jdbc:mysql://localhost:3306/buffer";
        String user = "root";
        String password = "root";

        try {
            Connection con = DriverManager.getConnection(url, user, password);

            String query = "SELECT * FROM not_allocated";
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {

                int id = rs.getInt("ticket_id");
                String username = rs.getString("cust_username");
                String desc = rs.getString("description");

                int dept = getDept(desc);

                // create Ticket object
                Ticket t = new Ticket(id, username, "", desc, 0, 0);

                bufferMap.get(dept).add(t);
            }

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bufferMap;
    }

    // (TESTING)
    public static void main(String[] args) {

        // HashMap<Integer, ArrayList<Ticket>> buffer = buildBuffer();

        // for (int dept : buffer.keySet()) {
        // System.out.println("Dept " + dept + " ->");

        // for (Ticket t : buffer.get(dept)) {
        // t.display();
        // }
        // }
    }
}