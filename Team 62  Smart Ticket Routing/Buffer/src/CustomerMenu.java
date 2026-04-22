import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

class CustomerMenu {

    static Scanner sc = new Scanner(System.in);
    static List<Ticket> tickets = new ArrayList<>();
    static int ticketCounter = 100;

    public static void main(String[] args) {

        System.out.print("Enter Username: ");
        String username = sc.nextLine();

        int choice;

        do {
            System.out.println("\n--- CUSTOMER MENU ---");
            System.out.println("1. Raise New Ticket");
            System.out.println("2. View My Tickets");
            System.out.println("3. Logout");
            System.out.print("Enter choice: ");

            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    raiseTicket(username);
                    break;

                case 2:
                    viewTickets(username);
                    break;

                case 3:
                    System.out.println("Logging out...");
                    break;

                default:
                    System.out.println("Invalid choice!");
            }

        } while (choice != 3);
    }

    // Raise Ticket
    public static void raiseTicket(String username) {
        String url = "jdbc:mysql://localhost:3306/buffer";
        String user = "root";
        String password = "root";

        try {
            Connection con = DriverManager.getConnection(url, user, password);

            Scanner sc = new Scanner(System.in);

            System.out.print("Enter Description: ");
            String desc = sc.nextLine();

            String query = "INSERT INTO not_allocated (cust_username, description) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setString(1, username);
            ps.setString(2, desc);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Ticket created successfully!");
            } else {
                System.out.println("Failed to create ticket.");
            }

            con.close();

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
        // sc.close();
    }

    // View Tickets
    public static void viewTickets(String username) {
        String url = "jdbc:mysql://localhost:3306/buffer";
        String user = "root";
        String password = "root";

        try {
            Connection con = DriverManager.getConnection(url, user, password);

            System.out.println("\n===== YOUR TICKETS =====");

            // 1. NOT ALLOCATED (Pending)
            String q1 = "SELECT * FROM not_allocated WHERE cust_username = ?";
            PreparedStatement ps1 = con.prepareStatement(q1);
            ps1.setString(1, username);

            ResultSet rs1 = ps1.executeQuery();

            System.out.println("\n--- Pending Tickets (Not Assigned) ---");

            boolean foundPending = false;

            while (rs1.next()) {
                System.out.println("Ticket ID: " + rs1.getInt("ticket_id"));
                // System.out.println("Category: " + rs1.getString("category"));
                System.out.println("Status: NOT ASSIGNED");
                System.out.println("Description: " + rs1.getString("description"));
                System.out.println("-----------------------------");

                foundPending = true;
            }

            // 2. ALLOCATED BUT NOT RESOLVED
            String q2 = "SELECT * FROM allocated WHERE cust_username = ? AND status != 'CLOSED'";
            PreparedStatement ps2 = con.prepareStatement(q2);
            ps2.setString(1, username);

            ResultSet rs2 = ps2.executeQuery();

            System.out.println("\n--- Pending Tickets (Assigned) ---");

            while (rs2.next()) {
                System.out.println("Ticket ID: " + rs2.getInt("ticket_id"));
                System.out.println("Category: " + rs2.getString("category"));
                System.out.println("Status: " + rs2.getString("status")); // ASSIGNED / IN PROGRESS
                System.out.println("Agent ID: " + rs2.getString("agent_id"));
                System.out.println("-----------------------------");

                foundPending = true;
            }

            if (!foundPending) {
                System.out.println("No pending tickets.");
            }

            // 3. RESOLVED
            String q3 = "SELECT * FROM ex_ticket WHERE cust_username = ?";
            PreparedStatement ps3 = con.prepareStatement(q3);
            ps3.setString(1, username);

            ResultSet rs3 = ps3.executeQuery();

            System.out.println("\n--- Resolved Tickets ---");

            boolean foundResolved = false;

            while (rs3.next()) {
                System.out.println("Ticket ID: " + rs3.getInt("ticket_id"));
                // System.out.println("Category: " + rs3.getString("category"));
                System.out.println("Status: CLOSED");
                System.out.println("Agent ID: " + rs3.getString("agent_id"));
                System.out.println("-----------------------------");

                foundResolved = true;
            }

            if (!foundResolved) {
                System.out.println("No resolved tickets.");
            }

            con.close();

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }
}
