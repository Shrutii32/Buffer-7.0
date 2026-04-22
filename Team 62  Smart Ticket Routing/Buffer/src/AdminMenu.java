import java.util.*;
import java.sql.*;

public class AdminMenu {

    public static void main(String[] args) {

        System.out.println("\n--- ADMIN PANEL ---");

        // 🔥 STEP 1: Get DB connection
        Connection conn = DBConnection.getConnection();

        // 🔥 STEP 2: Create Admin PROPERLY
        Admin admin = new Admin(conn);

        // 🔥 STEP 3: Run pipeline
        HashMap<Integer, PriorityQueue<Ticket>> pqMap = admin.prioritize();
        admin.allocate(pqMap);

        System.out.println("Allocation completed successfully!");
    }
}