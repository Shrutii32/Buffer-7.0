class Ticket {
    int ticketId;
    String username;
    String category;
    String description;

    int modelType; // 2 = Gold, 1 = Silver, 0 = Normal

    int timeWindow;
    int age;

    double priorityScore;
    String status;

    public Ticket(int ticketId, String username, String category,
            String description, int timeWindow, int age) {

        this.ticketId = ticketId;
        this.username = username;
        this.category = category;
        this.description = description;
        this.timeWindow = timeWindow;
        this.age = age;

        this.status = "NOT ASSIGNED";
    }
}