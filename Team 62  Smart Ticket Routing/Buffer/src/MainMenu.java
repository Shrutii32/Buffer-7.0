import java.util.*;

public class MainMenu {

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        int choice;

        do {
            System.out.println("\n===== MAIN MENU =====");
            System.out.println("1. Admin");
            System.out.println("2. Customer");
            System.out.println("3. Agent");
            System.out.println("4. Exit");
            System.out.print("Enter choice: ");

            choice = sc.nextInt();
            sc.nextLine(); // clear buffer

            switch (choice) {

                case 1:
                    // Admin : run allocation
                    AdminMenu.main(null);
                    break;

                case 2:
                    // Customer: open customer menu
                    CustomerMenu.main(null);
                    break;

                case 3:

                    AgentMenu.main(null);
                    break;

                case 4:
                    System.out.println("Exiting system...");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 4);
    }
}