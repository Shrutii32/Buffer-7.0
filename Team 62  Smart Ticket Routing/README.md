 📌 Intelligent Ticket Allocation & Priority Management System       
 // Team members: Shruti Ambekar, Ketaki Bhate, Gayatri Bhujbal, Shreya Dhage
 
 Video Link:https://drive.google.com/file/d/115Y2reyiKUBwzyF7gIVrZI4j2Sl-MwMU/view

## 🚀 Overview
This project is a dsa-based **Ticket Management System** that automatically categorizes, prioritizes, and allocates support tickets to agents using a smart rule-based engine.

It simulates real-world support systems used in **fintech, e-commerce, and IT helpdesk platforms**.

---

## ⚙️ Features

### 📥 1. Automatic Ticket Categorization
- Tickets are assigned to departments based on description analysis
- Uses keyword matching 

---

### ⚡ 2. Priority Calculation Engine
Each ticket gets a priority score based on:

- ⏳ Time urgency (time window)
- 📈 Ticket age
- 👑 Customer type (Gold / Silver / Normal)

**Formula:**
Priority Score = (0.5 × urgency) + (0.3 × age) + (0.2 × customer weight)
---

### 🧑‍💼 3. Smart Agent Allocation
- Assigns ticket to least-loaded available agent
- Department-wise agent grouping
- Dynamic workload tracking
- If no agent available, then the high priority tickets are assigned to supervisor which is hierarchially senior to the agents.

---

### 🔺 4. Supervisor Escalation System
- High-priority tickets (score ≥ threshold) are escalated
- If no agent is available, supervisor handles the ticket
- This ensures that the critical and time sensitive issues get resolved in time.
---

### 📊 5. Priority Queue Processing
- Each department uses a **Max Heap (PriorityQueue)**
- Ensures highest priority tickets are processed first

---

### 🗄️ 6. MySQL Database Integration
Handles persistent storage using:
This allows enhancement of console-based project
---

## 🏗️ System Architecture
Incoming Tickets
↓
Category Detection
↓
Priority Calculation
↓
Department-wise Priority Queue
↓
Agent Allocation Engine
↓
Database Update (Allocated / Pending)

---
