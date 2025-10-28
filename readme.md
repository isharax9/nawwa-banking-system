<div align="center">

# 🏦 Nawwa Banking System

### Enterprise-Grade Banking Platform Built with Jakarta EE 10 & EJB

[![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)](https://www.oracle.com/java/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-10-blue?style=for-the-badge&logo=jakarta-ee)](https://jakarta.ee/)
[![GlassFish](https://img.shields.io/badge/GlassFish-7.0-green?style=for-the-badge)](https://glassfish.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

[![GitHub stars](https://img.shields.io/github/stars/isharax9/nawwa-banking-system?style=social)](https://github.com/isharax9/nawwa-banking-system/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/isharax9/nawwa-banking-system?style=social)](https://github.com/isharax9/nawwa-banking-system/network/members)
[![GitHub issues](https://img.shields.io/github/issues/isharax9/nawwa-banking-system)](https://github.com/isharax9/nawwa-banking-system/issues)

**[📖 Documentation](https://doc.bank.nawwa.xyz)** • **[🚀 Live Demo](https://bank.nawwa.xyz)** • **[🐛 Report Bug](https://github.com/isharax9/nawwa-banking-system/issues)** • **[✨ Request Feature](https://github.com/isharax9/nawwa-banking-system/issues)**

</div>

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Technology Stack](#-technology-stack)
- [Getting Started](#-getting-started)
- [Module Structure](#-module-structure)
- [Application Workflows](#-application-workflows)
- [Screenshots](#-screenshots)
- [Deployment](#-deployment)
- [Analytics & Monitoring](#-analytics--monitoring)
- [Security](#-security)
- [License](#-license)
- [Support & Community](#-support--community)

---

## 🎯 Overview

The **Nawwa Banking System** is a modern, enterprise-grade core banking platform designed to revolutionize banking operations. Built on the robust **Jakarta EE 10** platform with **Enterprise JavaBeans (EJB)**, this system delivers unparalleled security, scalability, and reliability for mission-critical banking functions.

### 🎓 Core Objectives

✅ **Secure & Efficient** - Manage core banking functions with enterprise-level security  
✅ **Transactional Integrity** - Leverage EJB for ACID-compliant transactions  
✅ **Automated Operations** - Utilize EJB Timer Services for scheduled tasks  
✅ **Scalable Architecture** - Multi-module design for independent development and deployment

---

## ✨ Key Features

<div align="center">

| Feature | Description |
|---------|-------------|
| 🔐 **Secure Authentication** | BCrypt password hashing with robust user management |
| 👥 **Customer Management** | Complete lifecycle management of customer profiles and accounts |
| 💰 **Transaction Processing** | Real-time and scheduled fund transfers with ACID compliance |
| ⏰ **Automated Services** | EJB Timer Services for interest calculation and scheduled operations |
| 📊 **Analytics Integration** | Google Analytics & Microsoft Clarity for user behavior insights |
| 📄 **PDF Reports** | Downloadable transaction statements and reports |
| 🛡️ **Multi-layer Security** | WAF, DDoS protection, and programmatic authorization |
| 📱 **Responsive UI** | Modern interface with Bootstrap 5 and dynamic components |

</div>

---

## 🏗️ System Architecture

The Nawwa Banking System employs a multi-module architecture that ensures separation of concerns and enables parallel development.

```mermaid
%%{init: {'theme': 'dark'}}%%
graph TB
    subgraph "Client Layer"
        A[Web Browser]
    end
    
    subgraph "CDN & Security"
        B[Cloudflare CDN/WAF]
    end
    
    subgraph "Proxy Layer"
        C[Nginx Reverse Proxy<br/>SSL Termination]
    end
    
    subgraph "Application Server - GlassFish 7"
        D[Web Module<br/>Servlets & JSP]
        E[Security Module<br/>EJB]
        F[Banking Services<br/>EJB]
        G[Transaction Services<br/>EJB]
        H[Timer Services<br/>EJB Singleton]
        I[Notification Services<br/>EJB]
        J[Core Module<br/>JPA Entities & DTOs]
    end
    
    subgraph "Data Layer"
        K[(MySQL 8.0<br/>Database)]
        L[S3 Bucket<br/>Backups]
    end
    
    A -->|HTTPS| B
    B -->|Cached/Filtered| C
    C -->|HTTP| D
    D --> E
    D --> F
    D --> G
    E --> J
    F --> J
    G --> J
    H --> J
    I --> J
    J -->|JPA| K
    K -.->|Hourly Backup| L
    H -.->|Scheduled Tasks| G
    H -.->|Interest Calculation| F

    style A fill:#e1f5ff
    style B fill:#fff4e1
    style C fill:#ffe1e1
    style D fill:#e1ffe1
    style E fill:#f0e1ff
    style F fill:#f0e1ff
    style G fill:#f0e1ff
    style H fill:#f0e1ff
    style I fill:#f0e1ff
    style J fill:#ffe1f5
    style K fill:#e1f5ff
    style L fill:#e1f5ff
```

### 📦 Module Structure

| Module | Type | Description |
|--------|------|-------------|
| **core** | JAR | Foundational module with JPA entities, DTOs, mappers, and exceptions |
| **security-module** | EJB | Authentication, authorization, and user management |
| **banking-services** | EJB | Customer and account CRUD operations |
| **transaction-services** | EJB | Financial transaction processing with Facade pattern |
| **timer-services** | EJB | Automated background tasks (interest, scheduled transfers) |
| **notification-services** | EJB | Notification and alert management |
| **web** | WAR | Presentation layer with Servlets and JSP |
| **ear** | EAR | Final deployable Enterprise Archive |

```mermaid
%%{init: {'theme': 'dark'}}%%
graph LR
    A[core<br/>JAR] --> B[security-module<br/>EJB]
    A --> C[banking-services<br/>EJB]
    A --> D[transaction-services<br/>EJB]
    A --> E[timer-services<br/>EJB]
    A --> F[notification-services<br/>EJB]
    B --> G[web<br/>WAR]
    C --> G
    D --> G
    E --> G
    F --> G
    G --> H[ear<br/>Enterprise Archive]
    B --> H
    C --> H
    D --> H
    E --> H
    F --> H

    style A fill:#FFE5B4
    style B fill:#B4D7FF
    style C fill:#B4D7FF
    style D fill:#B4D7FF
    style E fill:#B4D7FF
    style F fill:#B4D7FF
    style G fill:#B4FFB4
    style H fill:#FFB4B4
```

---

## 💻 Technology Stack

<div align="center">

### Backend Technologies

![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-10.0-blue?style=flat-square)
![EJB](https://img.shields.io/badge/EJB-4.0-blue?style=flat-square)
![JPA](https://img.shields.io/badge/JPA-3.1-blue?style=flat-square)
![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)

### Frontend Technologies

![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat-square&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=flat-square&logo=css3&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat-square&logo=javascript&logoColor=black)
![Bootstrap](https://img.shields.io/badge/Bootstrap-5.0-7952B3?style=flat-square&logo=bootstrap&logoColor=white)
![jQuery](https://img.shields.io/badge/jQuery-0769AD?style=flat-square&logo=jquery&logoColor=white)

### Infrastructure

![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)
![GlassFish](https://img.shields.io/badge/GlassFish-7.0-green?style=flat-square)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white)
![Cloudflare](https://img.shields.io/badge/Cloudflare-F38020?style=flat-square&logo=cloudflare&logoColor=white)

</div>

| Category | Technology | Purpose |
|----------|-----------|----------|
| **Backend Framework** | Jakarta EE 10 | Enterprise platform foundation |
| **Business Logic** | EJB 4.0 (`@Stateless`, `@Singleton`) | Transaction management & business components |
| **Persistence** | JPA 3.1 with Hibernate | Object-Relational Mapping |
| **Database** | MySQL 8.0 | Relational data storage |
| **Frontend** | Servlets 6.0 & JSP 3.1 with JSTL | Server-side rendering |
| **UI Framework** | Bootstrap 5, jQuery, DataTables, Chart.js | Responsive UI & data visualization |
| **Security** | jBCrypt | Adaptive password hashing |
| **Build Tool** | Apache Maven 3.9+ | Dependency & build management |
| **App Server** | GlassFish 7 | Jakarta EE 10 runtime |
| **Proxy** | Nginx | Reverse proxy & SSL termination |
| **CDN/Security** | Cloudflare | WAF, DDoS protection & caching |

---

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:

- ☕ **Java Development Kit (JDK) 17** or higher
- 🔧 **Apache Maven 3.9+**
- 🗄️ **MySQL 8.0+**
- 🐠 **GlassFish 7.0** Application Server

### Installation & Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/isharax9/nawwa-banking-system.git
   cd nawwa-banking-system
   ```

2. **Configure MySQL Database**
   ```sql
   CREATE DATABASE nawwa_banking;
   CREATE USER 'banking_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON nawwa_banking.* TO 'banking_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Update Database Configuration**
   
   Configure your database connection in the persistence configuration file.

4. **Build the Project**
   ```bash
   mvn clean install
   ```

5. **Deploy to GlassFish**
   
   Deploy the generated EAR file from `ear/target/` to your GlassFish server.

6. **Access the Application**
   
   Navigate to: `http://localhost:8080/nawwa-banking/`

### 👤 Default Admin Credentials

For testing administrative features:

- **Username:** `mac`
- **Password:** `password123A!`

> ⚠️ **Important:** Change default credentials immediately in production!

---

## 📋 Module Structure

### Core Module (JAR)
The foundational library containing:
- 🏗️ JPA Entities (User, Customer, Account, Transaction)
- 📦 Data Transfer Objects (DTOs)
- 🔄 Mappers for entity-DTO conversion
- ⚠️ Custom exception hierarchy

### Security Module (EJB)
- 🔐 User authentication and registration
- 🔑 Password management with BCrypt
- ✅ Authorization and role management
- 👥 User lifecycle operations

### Banking Services (EJB)
- 👤 Customer profile management
- 🏦 Account CRUD operations
- 📊 Account activation/deactivation
- 🔄 Account type management

### Transaction Services (EJB)
- 💸 Fund transfers (atomic operations)
- 💰 Payments and deposits
- 📄 Transaction history
- 📥 PDF statement generation

### Timer Services (EJB)
Automated background operations:
- 📈 **Interest Calculation** - Daily interest for savings accounts
- ⏰ **Scheduled Transfers** - Execute scheduled fund movements
- 📊 **Daily Reports** - Generate system summary reports
- 🧹 **Maintenance Tasks** - Weekly data archival and cleanup

### Web Module (WAR)
- 🌐 Servlet-based controllers
- 🎨 JSP views with JSTL
- 📱 Responsive Bootstrap UI
- 📊 Interactive dashboards with Chart.js

---

## 🔄 Application Workflows

### User Registration Flow

```mermaid
%%{init: {'theme': 'dark'}}%%
sequenceDiagram
    actor User
    participant Browser
    participant RegisterServlet
    participant UserManagementService
    participant Database
    
    User->>Browser: Fill registration form
    Browser->>RegisterServlet: POST /register
    RegisterServlet->>RegisterServlet: Validate input
    RegisterServlet->>UserManagementService: register(userData)
    
    UserManagementService->>Database: Check username/email uniqueness
    Database-->>UserManagementService: Validation result
    
    alt Credentials unique
        UserManagementService->>UserManagementService: Hash password (BCrypt)
        UserManagementService->>Database: Create User entity
        UserManagementService->>Database: Create Customer entity
        Database-->>UserManagementService: Success
        UserManagementService-->>RegisterServlet: Registration complete
        RegisterServlet-->>Browser: Redirect to login (success)
    else Credentials exist
        UserManagementService-->>RegisterServlet: Throw exception
        RegisterServlet-->>Browser: Show error message
    end
    
    Browser-->>User: Display result
```

### Fund Transfer Flow

```mermaid
%%{init: {'theme': 'dark'}}%%
sequenceDiagram
    actor Customer
    participant Browser
    participant TransferServlet
    participant TransactionManager
    participant FundTransferService
    participant ScheduledTransferService
    participant Database
    
    Customer->>Browser: Navigate to Transfer Funds
    Browser->>TransferServlet: GET /transfer
    TransferServlet->>Database: Fetch customer accounts
    Database-->>TransferServlet: Account list
    TransferServlet-->>Browser: Display transfer form
    
    Customer->>Browser: Fill transfer details
    Browser->>TransferServlet: POST /transfer
    TransferServlet->>TransferServlet: Validate input
    
    alt Immediate Transfer
        TransferServlet->>TransactionManager: transferFunds()
        TransactionManager->>FundTransferService: execute transfer
        FundTransferService->>Database: BEGIN TRANSACTION
        FundTransferService->>Database: Debit source account
        FundTransferService->>Database: Credit destination account
        FundTransferService->>Database: Create transaction records
        FundTransferService->>Database: COMMIT
        Database-->>FundTransferService: Success
        FundTransferService-->>TransactionManager: Transfer complete
        TransactionManager-->>TransferServlet: Success
    else Scheduled Transfer
        TransferServlet->>ScheduledTransferService: create scheduled transfer
        ScheduledTransferService->>Database: Save ScheduledTransfer entity
        Database-->>ScheduledTransferService: Success
        ScheduledTransferService-->>TransferServlet: Scheduled
    end
    
    TransferServlet-->>Browser: Redirect to dashboard (success)
    Browser-->>Customer: Show success message
```

### Automated Interest Calculation Flow

```mermaid
%%{init: {'theme': 'dark'}}%%
sequenceDiagram
    participant TimerService
    participant InterestCalculationService
    participant Database
    participant Account
    
    Note over TimerService: Daily @ 00:00
    TimerService->>InterestCalculationService: Execute scheduled task
    InterestCalculationService->>Database: Fetch all savings accounts
    Database-->>InterestCalculationService: Savings accounts list
    
    loop For each eligible account
        InterestCalculationService->>InterestCalculationService: Calculate interest
        InterestCalculationService->>Database: Update account balance
        InterestCalculationService->>Database: Create interest transaction
    end
    
    InterestCalculationService->>Database: COMMIT all changes
    Database-->>InterestCalculationService: Success
    InterestCalculationService-->>TimerService: Task complete
    Note over TimerService: Log execution results
```

---

## 🎨 Screenshots

### Login Page
Clean and secure authentication interface

![Login Page](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%202.png)

### Customer Dashboard
Comprehensive overview of accounts and recent transactions

![Customer Dashboard](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%203.png)

### Fund Transfer Interface
Support for both immediate and scheduled transfers

![Fund Transfer](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%204.png)

### Transaction History
Detailed transaction logs with color-coded amounts

![Transaction History](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%205.png)

### User Management (Admin)
Administrative interface for user management

![User Management](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%206.png)

### Deposit/Withdrawal Interface
Simple and secure fund operations

![Deposit/Withdraw](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%207.png)

### Customer Management (Admin)
Manage customer profiles and information

![Customer Management](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2026.png)

### Bank Account Management (Admin)
Administrative control over all bank accounts

![Account Management](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2027.png)

### PDF Transaction Reports
Downloadable transaction statements

![PDF Report](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2018.png)

### EJB Timer Services in Action
Server logs showing automated timer execution

![Timer Logs](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%208.png)

---

## 🚀 Deployment

### Deployment Architecture

The application is deployed in a production-grade environment optimized for security, performance, and scalability.

```mermaid
%%{init: {'theme': 'dark'}}%%
graph TB
    A[User Request] -->|HTTPS| B[Cloudflare CDN]
    B -->|WAF & DDoS Protection| C[Nginx Reverse Proxy]
    C -->|SSL Termination| D[GlassFish 7 Server]
    D --> E[Nawwa Banking EAR]
    E --> F[(MySQL 8.0)]
    F -.->|Hourly Backup<br/>via Crontab| G[Amazon S3]
    
    style B fill:#f9a825
    style C fill:#00bcd4
    style D fill:#4caf50
    style E fill:#2196f3
    style F fill:#ff9800
    style G fill:#9c27b0
```

### Infrastructure Components

| Component | Technology | Purpose |
|-----------|-----------|----------|
| 🌐 **Application Server** | GlassFish 7 on Ubuntu VPS | Jakarta EE 10 runtime environment |
| 🔒 **Reverse Proxy** | Nginx | SSL/TLS termination, load balancing |
| 🛡️ **CDN & Security** | Cloudflare | WAF, DDoS protection, edge caching |
| 💾 **Database Backup** | Crontab + AWS S3 | Hourly automated backups |
| 📊 **Monitoring** | GlassFish Admin Console | Server health & performance |

### Version Control Strategy

```mermaid
%%{init: {'theme': 'dark'}}%%
gitGraph
    commit id: "Initial commit"
    branch testing
    checkout testing
    commit id: "Setup testing environment"
    branch feature/security
    checkout feature/security
    commit id: "Implement authentication"
    commit id: "Add BCrypt hashing"
    checkout testing
    merge feature/security
    branch feature/banking-services
    checkout feature/banking-services
    commit id: "Account management"
    checkout testing
    merge feature/banking-services
    branch feature/timer-services
    checkout feature/timer-services
    commit id: "Interest calculation"
    commit id: "Scheduled transfers"
    checkout testing
    merge feature/timer-services
    checkout main
    merge testing tag: "v1.0"
```

**Branching Strategy:**
- `main` - Production-ready releases
- `testing` - Integration and QA testing
- `feature/*` - Individual feature development

### 📚 Documentation Portal

Comprehensive documentation available at: **[doc.bank.nawwa.xyz](https://doc.bank.nawwa.xyz)**

<table>
<tr>
<td width="50%">

**Light Mode**
![Documentation Light](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image.png)

</td>
<td width="50%">

**Dark Mode**
![Documentation Dark](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%201.png)

</td>
</tr>
</table>

---

## 📊 Analytics & Monitoring

### Google Analytics Integration

The system leverages Google Analytics for comprehensive usage insights:

- 📈 **User Engagement** - Session duration, bounce rates, page views
- 🎯 **Conversion Tracking** - Account creation, transaction completion rates
- 🌍 **Traffic Analysis** - User acquisition sources and demographics
- 📱 **Device Analytics** - Cross-platform usage patterns

![Google Analytics](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2020.png)

### Microsoft Clarity Integration

Qualitative user behavior analysis through:

- 🎥 **Session Recordings** - Anonymized user interaction playback
- 🔥 **Heatmaps** - Click, scroll, and attention pattern visualization
- 😤 **Frustration Detection** - Rage clicks and abandoned processes
- 🎯 **Dead Click Analysis** - Non-interactive element identification

<table>
<tr>
<td width="50%">

**Dashboard Overview**
![Clarity Dashboard](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/Clarity_nawwa_banking_Dashboard_07-19-2025_06_10_PM.png)

</td>
<td width="50%">

**Session Recordings**
![Session Recording](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2021.png)

</td>
</tr>
<tr>
<td width="50%">

**Click Heatmaps**
![Click Heatmap](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2022.png)

</td>
<td width="50%">

**Scroll Heatmaps**
![Scroll Heatmap](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2023.png)

</td>
</tr>
</table>

### Additional Analytics Views

![Analytics Detail](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2024.png)

---

## 🔒 Security

### Multi-Layer Security Architecture

```mermaid
%%{init: {'theme': 'dark'}}%%
graph TD
    A[User Request] --> B{Cloudflare WAF}
    B -->|Threat Detected| C[Block Request]
    B -->|Clean Traffic| D{Nginx SSL/TLS}
    D --> E{Authentication Layer}
    E -->|Invalid| F[Deny Access]
    E -->|Valid| G{Authorization Check}
    G -->|Unauthorized| F
    G -->|Authorized| H[Business Logic]
    H --> I{Transaction Validation}
    I -->|Failed| J[Rollback]
    I -->|Success| K[Commit]
    
    style B fill:#ff6b6b
    style D fill:#4ecdc4
    style E fill:#45b7d1
    style G fill:#96ceb4
    style H fill:#ffeaa7
    style I fill:#dfe6e9
```

### Security Features

| Layer | Implementation | Description |
|-------|---------------|-------------|
| 🌐 **Network** | Cloudflare WAF | DDoS protection, malicious traffic filtering |
| 🔐 **Transport** | Nginx SSL/TLS | End-to-end encryption with HTTPS |
| 🔑 **Authentication** | BCrypt Hashing | Adaptive password hashing (2^10 rounds) |
| ✅ **Authorization** | Programmatic | Role-based access control (RBAC) |
| 💼 **Transaction** | Container-Managed | ACID compliance via EJB CMT |
| ⚠️ **Exception Handling** | Custom Hierarchy | Automatic transaction rollback on errors |
| 🔍 **Input Validation** | Server-side | Comprehensive form validation |
| 📝 **Audit Logging** | EJB Interceptors | Operation tracking and monitoring |

### Exception Handling Examples

**Inactive Account Transaction Attempt**

![Exception - Inactive Account](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2012.png)

**Unauthorized Access Attempt**

![Exception - Unauthorized User](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2013.png)

![Exception - Unauthorized Account](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2014.png)

### Input Validation Examples

**Registration Validation Flow**

<table>
<tr>
<td width="33%">

![Validation 1](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2015.png)

</td>
<td width="33%">

![Validation 2](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2016.png)

</td>
<td width="33%">

![Validation 3](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2017.png)

</td>
</tr>
</table>

### GlassFish Module Management

**Server Module Deployment**

![GlassFish Modules](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2019.png)

---

## 🔧 Development Workflow

### Feature Development Process

**Example: Adding PDF Report Feature**

<table>
<tr>
<td width="50%">

**Step 1: Create Pull Request**
![Create PR](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%209.png)

</td>
<td width="50%">

**Step 2: Merge to Main**
![Merge PR](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2010.png)

</td>
</tr>
</table>

**Final Repository State**

![GitHub Repository](Project%20Documentation%20Nawwa%20Banking%20System%2023526417036d80988509cc32b5ce97f4/image%2011.png)

---

## 🏆 Key Achievements

<div align="center">

✅ **Robust & Reliable** - Container-managed transactions and persistent EJB timers  
✅ **Enterprise Security** - Multi-layer security with BCrypt and programmatic authorization  
✅ **Process Automation** - Scheduled tasks reduce manual intervention by 80%  
✅ **High Availability** - Hourly database backups with S3 redundancy  
✅ **Scalable Architecture** - Multi-module design enables independent scaling  
✅ **Data-Driven Insights** - Integrated analytics for continuous improvement

</div>

---

## 📄 License

This project is licensed under the **MIT License**.

```
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

---

## 📞 Support & Community

<div align="center">

### 💬 **Get Help & Connect**

| Platform | Link | Purpose |
|----------|------|---------|
| 📧 **Email** | [isharax9@gmail.com](mailto:isharax9@gmail.com) | Direct support & inquiries |
| 💬 **Telegram** | [@mac_knight141](https://t.me/mac_knight141) | Quick questions & community |
| 💼 **LinkedIn** | [isharax9](https://www.linkedin.com/in/isharax9/) | Professional networking |
| 📸 **Instagram** | [@mac_knight141](https://www.instagram.com/mac_knight141/) | Updates & behind-the-scenes |
| 🐦 **Twitter** | [@isharax9](https://twitter.com/isharax9) | Announcements & tech discussions |

### 🐛 **Issues & Contributions**

| Type | Link | Description |
|------|------|-------------|
| 🐛 **Bug Reports** | [GitHub Issues](https://github.com/isharax9/nawwa-banking-system/issues) | Report bugs and request features |
| 💬 **Discussions** | [GitHub Discussions](https://github.com/isharax9/nawwa-banking-system/discussions) | Community Q&A and ideas |
| 📖 **Documentation** | [Project Docs](https://doc.bank.nawwa.xyz) | Complete guides and references |

### 🌟 **Show Your Support**

</div>

<div align="center">

**⭐ Star this repository if it helped you!**

[![GitHub stars](https://img.shields.io/github/stars/isharax9/nawwa-banking-system?style=social)](https://github.com/isharax9/nawwa-banking-system/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/isharax9/nawwa-banking-system?style=social)](https://github.com/isharax9/nawwa-banking-system/network/members)
[![GitHub watchers](https://img.shields.io/github/watchers/isharax9/nawwa-banking-system?style=social)](https://github.com/isharax9/nawwa-banking-system/watchers)
[![GitHub issues](https://img.shields.io/github/issues/isharax9/nawwa-banking-system)](https://github.com/isharax9/nawwa-banking-system/issues)

---

### 🚀 **Ready to Transform Banking Management?**

<div align="center" style="margin: 20px 0;">

**[⬇️ Download Now](https://github.com/isharax9/nawwa-banking-system/archive/refs/heads/main.zip)** • **[📖 Read Docs](https://doc.bank.nawwa.xyz)** • **[🤝 Contribute](https://github.com/isharax9/nawwa-banking-system/blob/main/CONTRIBUTING.md)**

</div>

**Made with ❤️ by [Ishara Lakshitha](https://github.com/isharax9) • © 2025**

*Empowering Banking Through Technology*

</div>
