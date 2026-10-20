# UI/UX Revamp: aMACEzing Sales (Activity Feed & Card-Based Layout)

Transform the entire user interface of **aMACEzing Sales** into a modern, professional business-management application inspired by the clean card architecture, information hierarchy, and intuitive navigation of LinkedIn and Facebook, while strictly respecting all business data, local Room database tables, and Cloud Firestore synchronization.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following critical product and UX decisions were confirmed through interactive clarification and will guide the entire implementation:

- **Confirmed Decision 1 (Orders Bottom Navigation Tab):** The `Orders` bottom navigation destination will be dedicated to customer quotations and active production orders, providing the business owner with immediate access to pending jobs and conversion workflows.
- **Confirmed Decision 2 (Secondary Tools Access):** Tools like **Customers Directory**, **Business Reports**, and **Excel Migration** will be directly accessible through dedicated quick-action shortcut cards on the **Home feed** and within the **Profile & Settings hub**, keeping the primary bottom navigation focused on the core 5 destinations (Home, Orders, Sales, Inventory, Profile).
- **Confirmed Decision 3 (Theme Posture):** Built around a sleek, corporate dark theme (`#0B0F14` background, `#151B23` cards, `#1C2430` elevated surfaces, `#00B8F0` primary cyan, and `#A78BFA` profit violet) with a theme toggle option preserved in Settings for light mode adaptability.
- **Strict Branding Rule:** Zero company/business logos or app icons in the main application UI (app bar, dashboard, cards, navigation). The app name will display solely as clean text **"aMACEzing Sales"** (with optional subtitle *"Engraving & Sintra Board Printing Manager"* where appropriate). Configured business logos will strictly be reserved for generated A4 Quotation and Receipt PDF documents.

---

## 1. Overview & Core Concept

- **What It Does:** Reimagines aMACEzing Sales from a standard utility dashboard into a streamlined business activity feed and operations console. The owner can monitor daily sales, track pending production jobs, review profit margins, manage inventory supplies, and generate professional documents without visual clutter.
- **Target Audience & Persona:** Small business owners, print shop operators, and craft studio managers specializing in Laser Engraving and Sintra Board printing who require rapid access to orders, finances, and material stocks on mobile.
- **Key Value:** Eliminates cognitive load through familiar social-app navigation patterns (top search/profile bar, 5-tab bottom bar, and vertical activity feed) while preserving 100% of the underlying business formulas, Room DAOs, and Firestore real-time sync.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Morning Briefing & Operations (Home Feed):**
   - User launches the app; the top greeting shows time-aware context (*"Good morning / afternoon / evening — Here's how your business is doing"*).
   - Glanceable summary cards display **Today's Net Sales**, **Today's Profit**, **Pending Orders**, and **Outstanding Balances**.
   - Tap **Quick Actions** row (`New Sale`, `New Order`, `Add Customer`, `Expenses`) to immediately trigger creation dialogs.
   - Scroll through **Pending Orders** cards, tap to review balance or record payments.
   - Scan the **Recent Transactions Feed** styled like clean feed cards with customer name, division badge, items summary, and payment badge.

2. **Managing Orders (Orders Tab):**
   - Centralized view of active price quotations and orders in progress.
   - Clean segmented status filters (`All`, `Pending`, `In Progress`, `Ready for Pickup`, `Completed`).
   - One-tap action to view A4 PDF quotation or convert directly to sales order.

3. **Tracking Sales & Payments (Sales Tab):**
   - Chronological list of completed and ongoing sales transactions.
   - Clear distinction between Gross Sales, Discounts, Net Sales, Payments Collected, Outstanding Balances, and Profit.
   - One-tap receipt generation and payment recording.

4. **Monitoring Raw Materials & Catalog (Inventory Tab):**
   - Visual catalog cards for Engraving blanks (Bamboo coasters, ballpens) and Sintra boards.
   - Stock counts, unit prices, minimum threshold warnings, and quick catalog management.

5. **Profile & Business Settings (Profile Tab):**
   - Account identity section with authenticated Google user photo/email.
   - Business branding configuration (TIN, address, payment instructions for PDFs).
   - System settings (order prefixes, profit masking, cloud sync status, update checks).

### Visual Identity & Theme Palette

| Token | Hex Value | Role & Usage |
|---|---|---|
| `Main Background` | `#0B0F14` | Primary scaffold background, immersive dark backdrop |
| `Card Background` | `#151B23` | Feed cards, summary cards, dialog surfaces |
| `Elevated Surface` | `#1C2430` | Top app bar, bottom navigation, active chips, modals |
| `Primary Text` | `#F3F6FA` | Headings, monetary amounts, customer names |
| `Secondary Text` | `#9AA6B2` | Supporting labels, timestamps, metadata |
| `Borders & Dividers` | `#2A3441` | Subtle card borders, horizontal dividers, input outlines |
| `Primary Accent Cyan` | `#00B8F0` | Net sales, primary buttons, active tab indicators |
| `Profit Accent Violet` | `#A78BFA` | Profit figures, secondary metrics, trend charts |
| `Success Green` | `#22C55E` | Paid status, positive profit, completed orders |
| `Warning Amber` | `#F59E0B` | Pending status, partial payment balance due, quote validity |
| `Error Red` | `#EF4444` | Cancelled orders, critical stock depleted, delete dialogs |

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Social-Style Activity Feed on Home**
  - *Chosen Approach:* Replace static dashboard grid tiles with an informative, vertically scrollable feed combining KPI summary chips, quick actions, performance chart, pending orders, and recent activity.
  - *Why:* Emulates the natural mobile scanning habits of Facebook and LinkedIn, allowing the business owner to digest high-priority items effortlessly.
- **Decision 2: 5-Tab Canonical Bottom Navigation**
  - *Chosen Approach:* Fix primary navigation to `Home`, `Orders`, `Sales`, `Inventory`, and `Profile`.
  - *Why:* Prevents mobile navigation congestion while keeping 100% of existing destinations (Customers, Reports, Migration) readily accessible via quick-action cards and settings menus.
- **Decision 3: Complete Removal of In-App Decorative Logos**
  - *Chosen Approach:* Remove all image drawables from the top bar, dashboard header, and cards. Use clean, high-contrast typography ("aMACEzing Sales").
  - *Why:* Strictly aligns with user requirements for an uncluttered, modern business interface.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          aMACEzing Sales UI                            │
├────────────────────────────────────────────────────────────────────────┤
│  Top App Bar: "aMACEzing Sales"  •  Search Action  •  Avatar/Profile   │
├────────────────────────────────────────────────────────────────────────┤
│                       Navigation Host (Screens)                        │
│                                                                        │
│   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │
│   │  HOME FEED   │ │ ORDERS TAB   │ │  SALES TAB   │ │INVENTORY TAB │  │
│   │ • Greeting   │ │ • Quotations │ │ • Net Sales  │ │ • Products   │  │
│   │ • 4 KPI Cards│ │ • Pipeline   │ │ • Balances   │ │ • Stock Stat │  │
│   │ • Actions Row│ │ • Filters    │ │ • Detail/Pay │ │ • Categories │  │
│   │ • Trend Chart│ │ • A4 Quote   │ │ • A4 Receipt │ │ • Edit Modal │  │
│   │ • Recent Feed│ └──────────────┘ └──────────────┘ └──────────────┘  │
│   └──────────────┘                                                     │
│                                                                        │
│   Secondary Destinations:                                              │
│   [Customers Directory]  [Reports & Analytics]  [Data Migration]       │
├────────────────────────────────────────────────────────────────────────┤
│  Bottom Navigation: [Home] [Orders] [Sales] [Inventory] [Profile]      │
├────────────────────────────────────────────────────────────────────────┤
│                         Shared Data Layer                              │
│         SalesViewModel (StateFlows, Calculated Totals, Dialogs)        │
│          ├── Room Local Database (SQLite: Transactions, Items)         │
│          └── Cloud Firestore Repository (Offline-first sync)           │
└────────────────────────────────────────────────────────────────────────┘
```

### Component State & Navigation Mapping

1. **`MainActivity.kt` & `Screen.kt`:**
   - Update `Screen` enum and `MainAppScaffold` bottom navigation items to display the 5 core tabs: `HOME`, `ORDERS`, `SALES`, `INVENTORY`, and `PROFILE`.
   - Top App Bar simplified: text-only title `"aMACEzing Sales"` with subtle subtitle on large screens, search icon button, and user profile avatar button.
2. **`Color.kt` & `Theme.kt`:**
   - Establish the requested Facebook/LinkedIn-inspired dark theme tokens (`#0B0F14`, `#151B23`, `#1C2430`, `#2A3441`, `#00B8F0`, `#A78BFA`).
3. **`DashboardScreen.kt` (Home Activity Feed):**
   - Time-based greeting ("Good morning", etc.) with current date.
   - Summary cards: Today's Net Sales, Today's Profit, Pending Orders count, Outstanding Customer Balance.
   - Quick Actions row: New Sale, New Order, Add Customer, Reports shortcut.
   - Sales Performance trend chart with Cyan Net Sales and Violet Profit series.
   - Feed cards for Pending Orders requiring owner attention and Recent Transactions.
4. **`TransactionsScreen.kt` & `QuotationsScreen.kt`:**
   - Refined card design with subtle border (`#2A3441`), status badge pills, clear typography, and action buttons.
5. **`ProductsScreen.kt` (Inventory):**
   - Clean product and supply list with division chips, stock indicators, and unit pricing.
6. **`ProfileScreen.kt` & `SettingsScreen.kt`:**
   - Unified profile hub featuring authenticated user info, business configuration, and system preferences.

---

## 5. Verification Plan

- Run `compile_applet` to ensure full Kotlin compilation and Compose preview validity.
- Run `gradle :app:testDebugUnitTest` to verify that all 28 Robolectric/unit tests for Room, calculations, and PDF generation pass.
- Verify that no application logo is visible anywhere in the main application UI.
