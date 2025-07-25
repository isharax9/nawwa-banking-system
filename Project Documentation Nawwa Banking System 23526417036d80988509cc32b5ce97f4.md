# Project Documentation: Nawwa Banking System

**Project Title:** Developing a Banking System with EJB and Time Services
**Author:** Ishara Lakshitha (@isharax9)
**Version:** 1.0
**Date:** July 17, 2025

---

---

# **1. Introduction**

## **1.1. Project Overview**

This document outlines the design, implementation, and architecture of the **EJB Banking System**, a modern core banking platform developed to overhaul a national bank's operations. The system is built on the Jakarta EE 10 platform, with a primary focus on Enterprise JavaBeans (EJB) to ensure robustness, scalability, and security.

The system handles critical banking functions, including customer and account management, secure financial transaction processing, and the automation of time-sensitive operations. A key architectural feature is the deep integration of **EJB Timer Services** to automate tasks like scheduled fund transfers and daily interest calculations, thereby reducing manual workload and enhancing operational efficiency.

## **1.2. Core Objectives**

- To develop a secure and efficient system for managing core banking functions.
- To leverage EJB for transactional integrity and scalability.
- To automate periodic and time-sensitive tasks using EJB Timer Services.
- To create a well-structured, maintainable, and multi-module application.

# **2. System Architecture**

## **2.1. Multi-Module Maven Structure**

The project is organized as a multi-module Maven project to enforce a strong separation of concerns and facilitate parallel development.

- **`core`**: A foundational JAR module containing JPA entities, DTOs, mappers, and custom exceptions. It is a shared dependency for all other modules.
- **`security-module` (EJB):** Handles all aspects of user authentication, registration, password management, and authorization.
- **`banking-services` (EJB):** Manages the core business logic for customer and account entities (CRUD operations).
- **`transaction-services` (EJB):** A specialized module for handling all financial movements, using a Facade pattern (`TransactionManager`) to orchestrate fund transfers and payments.
- **`timer-services` (EJB):** Contains all `@Singleton` EJB timers for automated, background tasks.
- **`web` (WAR):** The presentation layer, built with Servlets and JSPs, which acts as the client for the backend EJB services.
- **`ear`**: The final packaging module that assembles all EJB and WAR modules into a single, deployable Enterprise Archive.

This structure enhances maintainability, enables independent testing of modules, and promotes code reuse through the `core` library.

## **2.2. Technology Stack**

The system is built on a modern, robust, and industry-standard technology stack.

| Category | Technology / Library | Purpose |
| --- | --- | --- |
| **Backend Framework** | Jakarta EE 10 | Core enterprise platform |
| **Business Logic** | Enterprise JavaBeans (EJB) 4.0 (`@Stateless`, `@Singleton`) | Transaction management, concurrency, business components |
| **Persistence** | Jakarta Persistence (JPA) 3.1 | Object-Relational Mapping (ORM) and database interaction |
| **Database** | MySQL 8.0 | Relational data storage |
| **Frontend Framework** | Servlets 6.0 & JavaServer Pages (JSP) 3.1 with JSTL | Server-side rendering of the user interface |
| **Client-Side** | HTML5, CSS3, JavaScript, Bootstrap 5, jQuery, DataTables, Chart.js | Responsive UI, interactivity, and data visualization |
| **Security** | jBCrypt | Strong, adaptive password hashing |
| **Build & Dependencies** | Apache Maven | Project build automation and dependency management |
| **Application Server** | GlassFish 7 | Jakarta EE 10 compatible runtime environment |

## **2.3. Deployment Architecture**

The application is deployed in a standard enterprise environment designed for security and scalability.

- **Application Server:** The final EAR file is deployed to a **GlassFish 7** application server running on a Linux (Ubuntu) Virtual Private Server (VPS).
- **Reverse Proxy:** An **Nginx** server is configured as a reverse proxy in front of GlassFish. It is responsible for handling incoming traffic, SSL/TLS termination (HTTPS), and can be used for load balancing across multiple GlassFish nodes in a clustered setup.
- **CDN & Security Layer:** All traffic is routed through **Cloudflare**. This provides several benefits:
    - **DNS Management:** Manages the application's domain records.
    - **Security:** Provides a Web Application Firewall (WAF) and DDoS mitigation to protect the application from common web attacks.
    - **Performance:** Caches static assets (CSS, JS, images) at edge locations, reducing load on the origin server and speeding up page load times for users.

# **3. Core Features & Implementation**

## **3.1. User Authentication & Security**

Handled by the `security-module`, this feature provides robust authentication using the `AuthenticationService` and comprehensive user lifecycle management via the `UserManagementService`. Passwords are never stored in plaintext; they are securely hashed using **BCrypt**.

## **3.2. Customer & Account Management**

The `banking-services` module provides the business logic for managing customer profiles and their associated bank accounts. This includes creating new accounts, activating/deactivating them, and changing account types.

## **3.3. Transaction Processing**

The `transaction-services` module is the heart of financial operations. It uses a **Facade Pattern** (`TransactionManager`) to provide a simple API for two distinct operations:

- **Fund Transfers:** A two-legged, atomic transaction that debits one account and credits another, ensuring data integrity.
- **Payments & Deposits:** Single-legged transactions that modify the balance of a single account.
- **Transaction Statement downloading capability**

## **3.4. Automated Operations with EJB Timer Services**

The `timer-services` module automates all background tasks:

- **`InterestCalculationService`**: A daily timer that calculates and applies interest to all eligible savings accounts.
- **`ScheduledTransferProcessor`**: A frequent timer (runs every minute) that finds and executes scheduled transfers.
- **`DailyReportGenerator`**: A daily timer that generates summary reports.
- **`MaintenanceTaskService`**: A weekly timer for data hygiene, like archiving old transactions.

# **4. Architectural Deep Dive**

## **4.1. Transaction Demarcation Strategy**

The system exclusively uses **Container-Managed Transactions (CMT)**. By annotating EJB service methods with `@Transactional`, we delegate the complex task of managing transaction boundaries (begin, commit, rollback) to the EJB container. This declarative approach simplifies code and is less error-prone than manual management, ensuring all financial operations are fully atomic (ACID compliant).

## **4.2. Security Design**

Security is multi-layered, combining strong authentication with flexible authorization. The use of **BCrypt** for password hashing is a critical feature. Authorization is handled **programmatically** within the Servlets, which provides the flexibility to implement fine-grained rules, such as ensuring a customer can only view their own transaction history.

## **4.3. Exception Handling Strategy**

A custom exception hierarchy (with a base `BankingException`) is used. All custom exceptions extend `RuntimeException`, which automatically signals the EJB container to **roll back transactions** on business rule violations. In the web layer, a centralized `ServletUtil.getRootErrorMessage()` utility unwraps nested `EJBException`s to provide clear, user-friendly error messages.

## **4.4. Use of Interceptors for Cross-Cutting Concerns**

Interceptors (`AuditInterceptor`, `PerformanceMonitorInterceptor`) are used to manage cross-cutting concerns like logging and performance monitoring. This separates operational logic from business logic, making the services cleaner and adhering to the Single Responsibility Principle.

# **5. Application Flow & User Experience**

## **5.1. New User Registration Flow**

1. A new user navigates to the public home page and clicks "Register".
2. The user fills out the registration form (`register.jsp`).
3. Upon submission, the `RegisterServlet` receives the POST request and validates the input.
4. The servlet calls the `UserManagementService.register()` method.
5. Within a single, atomic transaction, the service validates that the username/email are unique, hashes the password using BCrypt, creates a new `User` entity, and creates a corresponding `Customer` entity.
6. If successful, the user is redirected to the login page with a success message. If any step fails, the entire transaction is rolled back, and an error message is displayed on the registration form.

## **5.2. Fund Transfer Flow**

1. A logged-in customer navigates to the "Transfer Funds" page.
2. The `TransferServlet` fetches the customer's accounts and displays the transfer form (`transfer.jsp`).
3. The customer fills in their source account, the destination account number, and the amount. They can optionally check a box to schedule the transfer for a future date/time.
4. Upon submission, the `TransferServlet` validates the input and calls the appropriate method on the `TransactionManager` facade:
    - **Immediate Transfer:** `transactionManager.transferFunds()` is called. This delegates to `FundTransferService`, which performs the atomic debit and credit.
    - **Scheduled Transfer:** A new `ScheduledTransfer` entity is created and saved via the `ScheduledTransferService`.
5. The customer is redirected to the dashboard with a success message.

# **6. Project Management & Deployment**

## **6.1. Version Control Strategy**

The project is managed using **Git**, hosted on GitHub. A workflow similar to **GitFlow** was adopted to ensure organized development:

- **`main` branch:** Contains stable, production-ready code for releases.
- **`testing` branch:** This branch used for testing completed components. All feature work is merged here before production.
- **Feature branches:** All new functionality was developed in dedicated feature branches (e.g., `feature/web`, `feature/timer-services`). These branches were created from `develop` and merged back into it upon completion via pull requests.

## **6.2. Deployment & Hosting**

- **Server:** GlassFish 7 on an Ubuntu VPS.
- **Proxy:** Nginx for SSL termination and reverse proxying.
- **CDN & Security:** Cloudflare for DNS, caching, and WAF/DDoS protection.

## **6.3. Administrator Access Credentials**

To assess the administrative privileges (managing users, customers, and accounts), the following credentials can be used on the login page:

- **Username:** `mac`
- **Password:** `password123A!`

## **6.4. External Documentation Portal**

Comprehensive, user-facing documentation and technical guides for the application are maintained at the following portal:

- **URL:** [https://doc.bank.nawwa.xyz](https://doc.bank.nawwa.xyz)
    
    ![light mode](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image.png)
    
    light mode
    
    ![dark mode](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%201.png)
    
    dark mode
    

# **7. Screenshots & Figures**

## **7.1. User Interface Screenshots**

*Screenshots of key application pages will be inserted here to showcase the user interface.*

- **Figure 1: Login Page** - Clean interface for user authentication.
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%202.png)
    

- **Figure 2: Customer Dashboard** - Overview of accounts and recent transactions.
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%203.png)
    

- **Figure 3: Fund Transfer Form** - Showing options for immediate and scheduled transfers.
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%204.png)
    

- **Figure 4: Transaction History** - A tabular view of an account's history with color-coded amounts.
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%205.png)
    

- **Figure 5: User Management (Admin)** - An administrative view for managing system users.
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%206.png)
    

- **Figure 6: Deposit / Withdraw Funds**
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%207.png)
    

## **7.2. Timer Services in Action**

**Figure 7: Server Log Showing Timer Service Execution.** This screenshot of the GlassFish `server.log` file shows the INFO messages logged by the `InterestCalculationService` and `ScheduledTransferProcessor` as they execute automatically at their scheduled times, confirming the successful operation of the EJB Timer Services.

![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%208.png)

## **7.3. Adding new feature to the `main` branch**

- **Figure 8: creating a PR for adding Transactions PDF report downloading feature**
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%209.png)
    

- figure 9: Merging the PR
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2010.png)
    

- figure 10: Final GitHub Repo
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2011.png)
    

## **7.4. Exception handlings are in action**

The application features comprehensive exception handling throughout the codebase. Here I will showcase only three examples:

- figure 11: when doing transaction across inactive accounts
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2012.png)
    

- figure 12: when customer try to visit the manage users URL
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2013.png)
    

- figure 13: when customer try to visit the manage Bank account URL
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2014.png)
    

## **7.4. Validations are in action**

- figure 14, figure 15, figure 16: here is the validation flow of the register phase.
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2015.png)
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2016.png)
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2017.png)
    

## **7.5. Downloading Transactions Report as PDF**

- figure 17: how the downloaded report look like
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2018.png)
    

## **7.6. How GlassFish Server Handles Modules**

- figure 18 :
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2019.png)
    

## **7.7. Uses of Google Analytics and Microsoft Clarity integration**

The Nawwa Banking System integrates both Google Analytics and Microsoft Clarity to gather comprehensive user behavior data, supporting ongoing UX improvements and performance monitoring:

### **7.7.1. Google Analytics Integration**

Google Analytics is implemented in the application to track key metrics including:

- **User Engagement:** Session duration, bounce rates, and page views to understand how users interact with different parts of the banking interface.
- **Conversion Tracking:** Monitoring completion rates of critical paths like account creation, fund transfers, and other banking transactions.
- **Traffic Sources:** Identifying how users discover and access the banking platform.
- **User Demographics:** Analyzing user base composition to better tailor features and interface elements.

### **7.7.2. Microsoft Clarity Implementation**

Microsoft Clarity complements analytics with qualitative insights:

- **Session Recordings:** Captures anonymized user interactions, showing exactly how customers navigate through banking processes.
- **Heatmaps:** Visualizes click, scroll, and attention patterns to identify areas of interest and potential friction points.
- **Frustration Signals:** Detects rage clicks, excessive scrolling, and abandoned processes to pinpoint UX issues.
- **Dead Clicks:** Identifies when users click non-interactive elements, highlighting potential misunderstandings in the interface.

These tools work together to provide both quantitative metrics and qualitative insights, enabling data-driven decisions for continual improvement of the banking platform while maintaining strict compliance with privacy regulations through anonymized data collection.

- figure 19: can get the full analytics data from google
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2020.png)
    

- figure 20,21,22,23,24: Microsoft Clarity to gather additional user behavior data, Live recording, Heat-maps
    
    ![figure 20](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/Clarity_nawwa_banking_Dashboard_07-19-2025_06_10_PM.png)
    
    figure 20
    
    ![figure 21](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2021.png)
    
    figure 21
    
    ![figure 22](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2022.png)
    
    figure 22
    
    ![figure 23](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2023.png)
    
    figure 23
    
    ![figure 24](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2024.png)
    
    figure 24
    

## **7.8. More about Admin privileges**

To assess the administrative privileges (managing users, customers, and accounts), the following credentials can be used on the login page:

- **Username:** `mac`
- **Password:** `password123A!`

- Figure 25: User management
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2025.png)
    

- Figure 26: Customer Management
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2026.png)
    

- Figure 27: Bank Account Management
    
    ![image.png](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2027.png)
    

# **8. Final Verdict**

The EJB Banking System successfully meets all the requirements outlined in the project brief. By leveraging the Jakarta EE platform and EJB best practices, the application provides a solution that is:

- **Robust & Reliable:** Through container-managed transactions and persistent EJB timers.
- **Data Persistence:** Database was backed up hourly using crontab, with dumps automatically transferred to an S3 bucket.
- **Secure:** By implementing strong password hashing and flexible, programmatic authorization.
- **Efficient:** Through the automation of key business processes, reducing the need for manual intervention.
- **Maintainable & Scalable:** Due to its well-defined multi-module architecture, which promotes separation of concerns and allows for independent development and deployment.

The project is a strong testament to the effectiveness of the Jakarta EE platform for building modern, scalable, and secure enterprise applications.

# **9. License**

This project is licensed under the MIT License.

```markdown
Copyright (c) 2025 Ishara Lakshitha

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```