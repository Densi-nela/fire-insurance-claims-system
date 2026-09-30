# 🛡️ SafeHaven Insurance — Fullstack Insurtech & Claims Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4%2B-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Vue 3](https://img.shields.io/badge/Vue.js-3.5%2B-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-6.0%2B-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4.0%2B-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tests](https://img.shields.io/badge/Tests-42%20Passing-brightgreen?style=for-the-badge)](https://junit.org/junit5/)

**SafeHaven Insurance** is a production-grade, fullstack digital casualty insurance platform for homeowners, property managers, and claims adjusters. Modeled after modern insurtech leaders (like Lemonade and Hippo), it unifies **instant quote-to-bind self-service**, **First Notice of Loss (FNOL) damage claim submission**, and a **back-office claims adjudication desk**.

---

## 🌟 Key Capabilities

### 1. 🏠 Customer & Homeowner Experience
* **Instant Quote & Buy Wizard (Public Homepage):** 3-step property onboarding allowing visitors to input their home address, select a customized coverage tier (Standard $150k, Premium $350k, Deluxe $500k), and instantly bind an official policy (`POL-FIRE-XXXX`) with automated account creation.
* **First Notice of Loss (FNOL) Submission:** Direct claim filing where active policies and covered property addresses are pre-selected. Enforces detailed fire incident statements, damage breakdowns (Structure vs. Belongings), and habitability declarations.
* **Photo & Evidence Intake:** Multi-part file upload supporting damage photographs, fire department incident reports, and repair contractor invoices.
* **Inline Evidence Lightbox:** View uploaded damage photos in a full-screen, darkened lightbox modal without downloading raw files.
* **Official Proof of Loss PDF Generator:** One-click generation of formal 8.5" x 11" settlement audit documents with corporate letterheads, damage valuation schedules, evidence audit logs, dual signature lines, and state regulatory disclaimers.
* **Customer Profile & Portfolio (`/profile`):** Review unique account IDs (`#ACC-XXXXX`), manage contact phone numbers and names, and audit bound active policies.

### 2. ⚖️ Back-Office Claims Adjuster & Underwriting Desk
* **Executive KPI Dashboard:** Real-time metrics for total claims, pending reviews queue, settlement rate percentages, and aggregate financial loss exposure.
* **Interactive Claims Distribution Bar:** Multi-segment visualization of active claims lifecycle (Submitted vs. Approved vs. Rejected) with emergency relocation badges.
* **Urgent Habitability Triage:** Automatically flags properties marked as `isLivable = false` with emergency badges for rapid temporary hotel placement.
* **Policy Issuance Console:** Ability to bind and issue new insurance policies directly to registered customer accounts across the company.
* **Full-Text Instant Search & Filter Chips:** Real-time search across Claim IDs, policy numbers, claimant names, and fire causes.

### 3. 🎭 Developer & Evaluator Demo Switcher
* Built-in floating widget at the bottom-right corner allowing 1-click persona transitions between:
  * **Michael Scott (Customer):** Policyholder with pre-existing policy `#POL-FIRE-9988` and an active claim.
  * **David Wallace (Adjuster):** Claims Examiner with platform-wide approval and policy issuance authority.
  * **Faith Korosso (Customer):** Clean-slate customer with active policy `#POL-FIRE-8301`.
  * **Public Visitor (Guest):** Public quote-to-bind landing page.

---

## 🏛️ System Architecture

```
                       ┌────────────────────────────────────────────────────────┐
                       │                   Client Browser                       │
                       │           (Vue 3 + Vite + Tailwind CSS)                │
                       └──────────────────────────┬─────────────────────────────┘
                                                  │
                               HTTP / REST (JWT Bearer Auth)
                                                  │
                                                  ▼
                       ┌────────────────────────────────────────────────────────┐
                       │          Spring Boot REST API (Kotlin)                 │
                       │                                                        │
                       │   ┌─────────────────────┐   ┌──────────────────────┐   │
                       │   │ SecurityInterceptor │   │  SpaForwardController│   │
                       │   │ (JWT & Role Guards) │   │  (SPA Client Routes) │   │
                       │   └──────────┬──────────┘   └──────────────────────┘   │
                       │              │                                         │
                       │   ┌──────────┴─────────────────────────────────────┐   │
                       │   │ REST Controllers:                              │   │
                       │   │ - AuthController        - PolicyController     │   │
                       │   │ - CustomerController    - AttachmentController │   │
                       │   │ - ClaimController                              │   │
                       │   └──────────┬─────────────────────────────────────┘   │
                       └──────────────┼─────────────────────────────────────────┘
                                      │
                   ┌──────────────────┴──────────────────┐
                   ▼                                     ▼
        ┌─────────────────────┐               ┌─────────────────────┐
        │  Spring Data JPA    │               │ FileStorageService  │
        │  (SQLite / HSQLDB)  │               │ (Local Disk / S3)   │
        └─────────────────────┘               └─────────────────────┘
```

---

## 🚀 Quick Start Guide

### Option 1: Docker Compose (Recommended — Single Command)
Prerequisites: [Docker Desktop](https://www.docker.com/products/docker-desktop/)

```bash
# Clone the repository
git clone https://github.com/yourusername/safehaven-insurance.git
cd safehaven-insurance

# Build and start the unified container
docker compose up --build
```
* **Web Application:** `http://localhost:8080`
* Persistent named volumes (`app-data` and `app-uploads`) ensure SQLite databases and photo evidence persist across restarts.

---

### Option 2: Local Fullstack Development

#### 1. Start the Spring Boot Backend
Prerequisites: JDK 17+

```powershell
# In the project root folder
.\mvnw spring-boot:run
```
* Backend API listens on `http://localhost:8080`.

#### 2. Start the Vue 3 Frontend
Prerequisites: Node.js 20+

```powershell
cd frontend
npm install
npm run dev
```
* Frontend dev server runs on `http://localhost:5174` (proxies `/api` calls directly to `8080`).

---

### Option 3: Single Executable JAR / WAR

Because compiled Vue assets are embedded directly into Spring Boot's static resources, you can package the entire application into a single executable file:

```powershell
# Build executable production archive
.\mvnw clean package -DskipTests

# Run on any machine with Java 17
java -jar target/demo-0.0.1-SNAPSHOT.war
```
* Open `http://localhost:8080` in your browser.

---

## 🔑 Demo & Test Accounts

Use the **🎭 Demo Persona Switcher** on the screen, or log in manually using these seeded accounts:

| Role | Name | Email | Password | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Customer** | Michael Scott | `michael.scott@dundermifflin.com` | `password123` | Owns Policy `#POL-FIRE-9988` with an active claim |
| **Adjuster** | David Wallace | `david.wallace@dundermifflin.com` | `admin123` | Admin & Claims Examiner with full review powers |
| **Customer** | Faith Korosso | `faithkorosso@gmail.com` | `password123` | Owns Policy `#POL-FIRE-8301` with zero claims |

---

## 📡 REST API Reference

| Method | Endpoint | Access Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Public | Authenticates credentials and returns JWT Bearer token |
| `GET` | `/api/customers/me` | Customer / Adjuster | Retrieves authenticated profile metadata and bound policies |
| `PUT` | `/api/customers/me` | Customer / Adjuster | Updates legal name and contact phone number |
| `POST` | `/api/customers` | Public | Self-service customer account registration |
| `GET` | `/api/policies` | Scoped | Returns customer-owned policies (or all policies for Adjusters) |
| `POST` | `/api/policies` | Public / Adjuster | Issues a new insurance policy bound to a customer ID |
| `GET` | `/api/claims` | Scoped | Retrieves claims (filtered by customer ownership or all for Adjusters) |
| `POST` | `/api/claims` | Customer | Files a First Notice of Loss against an owned policy |
| `GET` | `/api/claims/{id}` | Customer / Adjuster | Retrieves claim details, damage schedules, and evidence |
| `PUT` | `/api/claims/{id}/review` | Adjuster | Settle claim: submits `APPROVED` or `REJECTED` with adjuster notes |
| `POST` | `/api/claims/{id}/attachments`| Customer | Uploads supporting photographic evidence or incident PDF reports |
| `GET` | `/api/attachments/{id}` | Customer / Adjuster | Streams and downloads evidence files inline |

---

## 🧪 Testing & Validation

The application features comprehensive integration and controller tests using JUnit 5, MockMvc, and Spring Security test harnesses.

```powershell
# Run the complete test suite
.\mvnw test
```

```text
[INFO] Results:
[INFO] Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🛠️ Built With

* **Backend:** Kotlin, Spring Boot 3 / 4, Spring Data JPA, Spring Security (JWT), SQLite, Flyway.
* **Frontend:** Vue 3 (Composition API), Vite, Vue Router 4, Pinia, Tailwind CSS.
* **DevOps & Packaging:** Docker (Multi-stage build), Docker Compose, Maven.

---

## 📄 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
