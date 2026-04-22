import java.util.*;
import java.sql.*;

public class AgentMenu {

    static Scanner sc = new Scanner(System.in);

    static String URL = "jdbc:mysql://localhost:3306/buffer";
    static String USER = "root";
    static String PASS = "root";

    // ---------------- COMMON DB CONNECTION ----------------
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            System.out.println("DB Error: " + e);
            return null;
        }
    }

    public static void main(String[] args) {

        int agentId = -1;

        while (true) {
            try {
                Connection con = getConnection();

                System.out.print("Enter Agent ID: ");
                int id = sc.nextInt();
                sc.nextLine();

                System.out.print("Enter Password: ");
                String pass = sc.nextLine();

                String query = "SELECT * FROM agent_table WHERE agentID=? AND password=?";
                PreparedStatement ps = con.prepareStatement(query);

                ps.setInt(1, id);
                ps.setString(2, pass);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    System.out.println("Login Successful!");

                    // set availability true
                    String update = "UPDATE agent_table SET availability=true WHERE agentID=?";
                    PreparedStatement ps2 = con.prepareStatement(update);
                    ps2.setInt(1, id);
                    ps2.executeUpdate();

                    agentId = id;
                    con.close();
                    break;
                } else {
                    System.out.println("Invalid ID or Password! Try again.");
                    con.close();
                }

            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }

        int choice;

        do {
            System.out.println("\n--- AGENT MENU ---");
            System.out.println("1. View Assigned Tickets");
            System.out.println("2. Resolve Ticket");
            System.out.println("3. View Today's Resolved Tickets");
            System.out.println("4. Logout");
            System.out.print("Enter choice: ");

            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    viewAssignedTickets(agentId);
                    break;

                case 2:
                    resolveTicket(agentId);
                    break;

                case 3:
                    viewTodayResolved(agentId);
                    break;

                case 4:
                    // Logoout (set availability false)
                    try {
                        Connection con = getConnection();

                        String query = "UPDATE agent_table SET availability=false WHERE agent_id=?";
                        PreparedStatement ps = con.prepareStatement(query);

                        ps.setInt(1, agentId);
                        ps.executeUpdate();

                        con.close();

                    } catch (Exception e) {
                        System.out.println("Error: " + e);
                    }

                    System.out.println("Logged out successfully!");
                    break;

                default:
                    System.out.println("Invalid choice!");
            }

        } while (choice != 4);
    }

    // ---------------- VIEW ASSIGNED TICKETS ----------------
    public static void viewAssignedTickets(int agentId) {

        try {
            Connection con = getConnection();

            String query = "SELECT * FROM allocated WHERE agentID = ? AND status != 0";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setInt(1, agentId);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n--- ASSIGNED TICKETS ---");

            boolean found = false;

            while (rs.next()) {
                System.out.println("Ticket ID: " + rs.getInt("ticket_id"));
                System.out.println("Customer: " + rs.getString("cust_username"));
                System.out.println("Status: " + rs.getString("status"));
                System.out.println("-----------------------------");
                found = true;
            }

            if (!found) {
                System.out.println("No assigned tickets.");
            }

            con.close();

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    // ---------------- RESOLVE TICKET ----------------
    public static void resolveTicket(int agentId) {

        try {
            Connection con = getConnection();

            System.out.print("Enter Ticket ID to resolve: ");
            int ticketId = sc.nextInt();

            String query = "UPDATE allocated SET status=1 WHERE ticket_id=? AND agentid=?";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setInt(1, ticketId);
            ps.setInt(2, agentId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Ticket resolved successfully!");
            } else {
                System.out.println("Ticket not found or not assigned to you.");
            }

            con.close();

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    // ---------------- VIEW TODAY RESOLVED ----------------
    public static void viewTodayResolved(int agentId) {

        try {
            Connection con = getConnection();

            String query = "SELECT * FROM ex_ticket WHERE agent_id=? AND DATE(resolved_date)=CURDATE()";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setInt(1, agentId);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n--- TODAY'S RESOLVED TICKETS ---");

            boolean found = false;

            while (rs.next()) {
                System.out.println("Ticket ID: " + rs.getInt("ticket_id"));
                System.out.println("Customer: " + rs.getString("cust_username"));
                System.out.println("-----------------------------");
                found = true;
            }

            if (!found) {
                System.out.println("No tickets resolved today.");
            }

            con.close();

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }
}