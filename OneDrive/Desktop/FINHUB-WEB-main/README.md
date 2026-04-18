<div align="center">

# 🏦 FinHub Web (Escrow & Trust)

**The secure, web-based companion to the FinHub financial ecosystem.**

[![PHP](https://img.shields.io/badge/PHP-777BB4?style=for-the-badge&logo=php&logoColor=white)](https://php.net/)
[![Symfony](https://img.shields.io/badge/Symfony-000000?style=for-the-badge&logo=symfony&logoColor=white)](https://symfony.com/)
[![Twig](https://img.shields.io/badge/Twig-000000?style=for-the-badge&logo=twig&logoColor=white)](https://twig.symfony.com/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Blockchain](https://img.shields.io/badge/Blockchain_Ledger-121D33?style=for-the-badge&logo=chainlink&logoColor=white)]()

</div>

<br />

## 🌟 Architecture & Engineering Concept

**FinHub Web** is the modern web portal for the FinHub distributed fintech architecture, built natively in **Symfony PHP**. It seamlessly integrates with the shared, core banking ledger and provides users with a responsive, intuitive interface for managing their funds, trusted contacts, and secure escrow agreements.

**The Engineering Challenge:** Replicating the strict security, tamper-proof blockchain ledger integrity, and atomic transaction flows of the desktop client in a responsive web environment, while ensuring seamless cross-platform interoperability.
**The Solution:** A strictly structured Symfony MVC architecture utilizing Doctrine ORM, alongside custom cryptographic hashing routines that mirror the core Java application's ledger logic. Real-time UI updates (like QR code scanning for escrow release) are handled via AJAX polling, creating a fluid, app-like experience in the browser.

---

## 🏗️ System Architecture

```mermaid
graph TD
    subgraph WebApp ["FinHub Web Application"]
        UI[Twig Templates / HTML / CSS / JS]
        User[Browser Client]
        User -->|Interact| UI
    end

    subgraph CoreBanking ["Symfony Backend"]
        Controllers[Symfony Controllers]
        Ledger[Cryptographic Ledger Engine]
        DB[(Shared MySQL Database)]
        
        UI <-->|AJAX / HTTP| Controllers
        Controllers <-->|Atomic Transactions| Ledger
        Ledger <-->|Read/Write| DB
    end
    
    subgraph Integrations ["External APIs"]
        QR[QR Server API]
        DocuSign[DocuSign API]
        
        Controllers -->|Fetch QR| QR
        Controllers -->|Agreements| DocuSign
    end
```

---

## ✨ Enterprise-Grade Features

- **🔐 Atomic Escrow Workflows:** Funds are securely locked in a digital vault. Release mechanisms include QR Code scanning, Secret Code input, or Admin Approval.
- **⛓️ Blockchain-Backed Ledger:** Every transaction (HOLD, RELEASE, REFUND) is cryptographically hashed and chained to the previous block in the `blockchain_ledger` table, ensuring immutable, tamper-proof records.
- **📱 Fully Responsive UI:** A modern, mobile-first design featuring animated circular progress bars, modal overlays, and real-time form calculations.
- **🤝 Trusted Contacts:** Manage a network of trusted trading partners with real-time trust score indicators and recent transaction histories.
- **📝 DocuSign Integration:** Optional binding of high-value escrow agreements to legally enforceable digital signatures.
- **🔄 Cross-Platform Sync:** Shares the exact same MySQL schema and cryptographic logic as the JavaFX desktop client, allowing users to switch between platforms seamlessly.

---

## 🛠️ Technology Stack

- **Backend Framework:** PHP 8+, Symfony
- **Templating Engine:** Twig
- **Database / ORM:** MySQL / MariaDB, Doctrine ORM
- **Frontend:** HTML5, CSS3 (Custom Responsive Styling), Vanilla JavaScript, SweetAlert2
- **Integrations:** API.QRServer (QR Generation), DocuSign API

---

## 🚀 Getting Started

### Prerequisites
- PHP 8.1 or higher
- Composer
- Symfony CLI
- MySQL or MariaDB

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/sadok-dridi/FINHUB-WEB.git
   cd FINHUB-WEB
   ```

2. **Install dependencies:**
   ```bash
   composer install
   ```

3. **Configure your environment:**
   Copy the `.env` file to `.env.local` and update your database credentials:
   ```env
   DATABASE_URL="mysql://db_user:db_password@127.0.0.1:3306/finhub_db?serverVersion=8.0"
   ```

4. **Initialize the database:**
   ```bash
   php bin/console doctrine:database:create
   php bin/console doctrine:migrations:migrate
   ```

5. **Start the local web server:**
   ```bash
   symfony server:start
   ```

6. **Access the application:**
   Open your browser and navigate to `http://localhost:8000`.

---

<div align="center">
  <i>Built to demonstrate high-security financial systems, web interoperability, and cryptographic ledger integrity.</i>
</div>