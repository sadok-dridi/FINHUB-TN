<div align="center">

# 🏦 FinHub-TN (Escrow Engine)

**A secure, blockchain-inspired escrow & trading platform.**

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![JavaFX](https://img.shields.io/badge/JavaFX-Primary-blue?style=for-the-badge)](https://openjfx.io/)
[![Symfony](https://img.shields.io/badge/Symfony-000000?style=for-the-badge&logo=symfony&logoColor=white)](https://symfony.com/)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![n8n](https://img.shields.io/badge/n8n-Workflow_Automation-FF6D5A?style=for-the-badge&logo=n8n&logoColor=white)](https://n8n.io/)
[![Telegram](https://img.shields.io/badge/Telegram_Bot-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://core.telegram.org/bots)

</div>

<br />

## 🌟 Architecture & Engineering Concept

**FinHub-TN** is a distributed fintech architecture designed to facilitate secure, trustless peer-to-peer transactions through an automated escrow system.

**The Engineering Challenge:** Financial platforms require absolute atomicity (no partial transactions) and real-time operator alerts without coupling the core ledger to notification services.
**The Solution:** I decoupled the backend (Symfony REST API) from the frontend (JavaFX) and integrated an event-driven automation layer using self-hosted **n8n**. Webhooks trigger a custom **Telegram Bot** that allows administrators to remotely control wallets and monitor transactions.

---

## 🏗️ System Architecture

```mermaid
graph TD
    subgraph ClientApp ["Client Application"]
        JFX[JavaFX Desktop UI]
        Trader[User/Trader]
        Trader -->|Interact| JFX
    end

    subgraph CoreBanking ["Core Banking Engine"]
        Sym[Symfony REST API]
        DB[(MariaDB Ledger)]
        Sym <-->|Atomic Transactions| DB
        JFX <-->|HTTPS/JSON| Sym
    end

    subgraph AutomationOps ["Automation & Ops (n8n)"]
        Webhook[n8n Webhook Listener]
        Telegram[Telegram Control Bot]
        
        Sym -->|Emit Events| Webhook
        Webhook -->|Format & Alert| Telegram
        Telegram -->|Approve/Reject| Webhook
        Webhook -->|Callback| Sym
    end
```

---

## ✨ Enterprise-Grade Features

- **🔐 Atomic Escrow Workflows:** Funds are cryptographically locked in a digital vault until multi-party validation is received, preventing race conditions or double-spending.
- **🤖 Automated Telegram Ops:** Remote wallet control and real-time transaction approval flows handled entirely through a Telegram bot via `n8n` webhooks.
- **🏛️ Decoupled Architecture:** Strict separation between the Front Office (trading/UI) and Back Office (administration/ledger).
- **📈 Smart Trading Support:** Basic AI-assisted support integrations for users navigating the trading platform.

---

## 🛠️ Technology Stack

- **Client Terminal:** Java, JavaFX (Desktop Client)
- **Core Ledger API:** PHP, Symfony Framework
- **Database:** MariaDB / MySQL (ACID Compliant)
- **Orchestration:** n8n (Webhooks & Event Routing)
- **Ops:** Telegram API

---

<div align="center">
  <i>Built to demonstrate high-security financial systems and event-driven architecture.</i>
</div>