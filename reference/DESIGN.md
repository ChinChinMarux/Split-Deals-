---
name: Split the Deals!
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#3f4850'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#707881'
  outline-variant: '#bfc7d2'
  surface-tint: '#006398'
  primary: '#006194'
  on-primary: '#ffffff'
  primary-container: '#007bb9'
  on-primary-container: '#fdfcff'
  inverse-primary: '#93ccff'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#b90538'
  on-tertiary: '#ffffff'
  tertiary-container: '#dc2c4f'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#cce5ff'
  primary-fixed-dim: '#93ccff'
  on-primary-fixed: '#001d31'
  on-primary-fixed-variant: '#004b73'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffdadb'
  tertiary-fixed-dim: '#ffb2b7'
  on-tertiary-fixed: '#40000d'
  on-tertiary-fixed-variant: '#92002a'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 48px
    letterSpacing: -0.02em
  display-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  title-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  title-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
  currency-display:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 36px
    letterSpacing: -0.02em
  currency-item:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 20px
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes an approachable, hyper-dependable Material 3 financial utility tailored for mobile-first bill splitting, receipt OCR scanning, and peer-to-peer debt settlements. The design philosophy harmonizes friendly geometric contours with rigorous tabular clarity, removing social awkwardness and mental friction from shared expenses.

### Design Principles
- **Clarity Over Clutter:** Financial breakdowns, tax additions, and individual item allocations must be scannable at an arm's length in dim dining environments.
- **Frictionless Trust:** Clean oceanic blues provide institutional authority, while soft pastel tonal containers soften financial anxiety.
- **Tactile Material Polish:** Fluid gestures, pill-shaped interactive chips, and prominent central camera floating actions ground the system in tactile physical affordances.
- **Visual Status Parity:** Settlement status is unambiguous—clear emerald mint signals total resolution, while warm coral denotes pending actions without feeling punitive.

## Colors

The color palette centers on high-trust oceanic cyan-blue anchored by clean slate neutrals, complemented by functional mint and coral status tokens.

### Palette Architecture
- **Primary (`#0284C7`):** Represents system intent, active interactive selections, primary buttons, and OCR crop guides. The primary container (`#E0F2FE`) acts as the interactive backing for focused list items and active user assignments.
- **Secondary / Success (`#10B981`):** Represents settled amounts, verified OCR reads, and completed receipts ("Lunas"). Paired with its soft container (`#D1FAE5`) and deep label tint (`#065F46`).
- **Tertiary / Warning (`#F43F5E`):** Represents pending actions, unassigned split lines, overdue balances, and deletion confirmations ("Belum Lunas"). Paired with container (`#FFE4E6`) and deep label tint (`#9F1239`).
- **Surface & Backgrounds:** The base app background uses `#F8FAFC`, stepping up to pure `#FFFFFF` for elevated receipt cards, with `#F1F5F9` providing tonal surface-variant containment.
- **Text & Hierarchy:** The high-contrast slate `#0F172A` delivers sharp legibility for monetary values, while muted slate `#475569` supports metadata, timestamps, and breakdown descriptions.

## Typography

The typographic hierarchy implements a high-clarity dual system: **Plus Jakarta Sans** brings warm, modern geometric structure to titles, totals, and screen headers, while **Inter** delivers neutral, highly legible figures for itemized bills, tax rates, and participants.

### Currency Formatting Guidelines
- Indonesian Rupiah must be formatted with strict spacing: `Rp 45.000` (non-breaking space between the symbol and value, period as thousands separator).
- In receipt breakdowns and summary lines, currency labels should use tabular figures (`font-feature-settings: "tnum"`) to guarantee exact column alignment across multiple participants.
- In zero-decimal contexts, drop the `,00` suffix to conserve horizontal space on mobile list views.

## Layout & Spacing

The layout is built around a mobile-first 4-column grid (expanding to 8 columns on tablets and 12 columns on desktop web view). 

### Layout Model
- **Mobile Handheld (320px–599px):** 4 columns, `margin` of `1rem` (16px), `gutter` of `1rem` (16px). Bottom navigation clearance is pinned at a fixed 80px buffer plus system safe areas to accommodate the central scan FAB.
- **Tablet / Large Foldable (600px–839px):** 8 columns, `margin-tablet` of `1.5rem` (24px), displaying a master-detail split (Receipt List / Item Allocator side-by-side).
- **Desktop (840px+):** Centered max-width canvas constrained to 720px for singular bill inspection, or full 1200px dashboard layout for group deal histories.

### Spacing Usage
- `space-xs` (4px): Inner badge padding, micro spacing between user avatars.
- `space-sm` (8px): Gaps between participant chips, item checkbox-to-label spacing.
- `space-md` (16px): Standard receipt card inner padding, row gaps between bill items.
- `space-lg` (24px): Card-to-card stacking separation and bottom modal drawer padding.
- `space-xl` (32px): Major flow division (e.g., transition from OCR review to settlement summary).

## Elevation & Depth

Visual hierarchy leverages Material 3 tonal layering combined with soft, daylight-inspired ambient shadows. Surfaces use elevated tinting rather than dark heavy drop shadows, keeping calculations airy and clean.

### Elevation Hierarchy
- **Level 0 (Flat / Canvas):** `#F8FAFC`. Base background for application screens and scrollable areas.
- **Level 1 (Card Default):** `#FFFFFF` paired with an ambient shadow (`box-shadow: 0 1px 3px 0 rgba(15, 23, 42, 0.06), 0 1px 2px -1px rgba(15, 23, 42, 0.04)`). Used for collapsed receipt cards, participant list items, and standard panels.
- **Level 2 (Active / Focus Card):** `#FFFFFF` with heightened ambient spread (`box-shadow: 0 4px 6px -1px rgba(15, 23, 42, 0.07), 0 2px 4px -2px rgba(15, 23, 42, 0.05)`). Used for selected line-items undergoing editing or split assignment.
- **Level 3 (Floating Action Button):** Cyan ambient glow (`box-shadow: 0 10px 15px -3px rgba(2, 132, 199, 0.25), 0 4px 6px -4px rgba(2, 132, 199, 0.15)`). The central camera scan button hovers continuously over content.
- **Level 4 (Modal Drawers & Bottom Sheets):** `#FFFFFF` with backdrop blur of 8px and outer shadow (`box-shadow: 0 20px 25px -5px rgba(15, 23, 42, 0.12), 0 8px 10px -6px rgba(15, 23, 42, 0.08)`). Used for manual item entry sheets and participant debt reconciliations.

## Shapes

The design system adopts a pill-shaped and rounded shape language (`roundedness: 3`). Fully rounded corners promote an approachable, tactile, non-intimidating aesthetic for financial interactions.

### Component Rounding Rules
- **Pill Elements (`rounded-full` / 9999px):** Buttons, interactive participant assignment chips, quick-filter tabs, text input search fields, and status badges.
- **Cards & Modals (`rounded-2xl` / 1.5rem to `rounded-3xl` / 2rem):** Receipt breakdown containers, OCR item tables, and bottom action sheets.
- **List Item Selections (`rounded-xl` / 1rem):** Individual bill rows within an expanded receipt.

## Components

### 1. Elevated Floating Action Button (Center Scan Button)
- **Geometry:** 64px × 64px circular floating trigger, positioned precisely at the center of the bottom navigation bar.
- **Color & Style:** Solid Ocean Blue (`#0284C7`) background with white camera icon (`#FFFFFF`). Level 3 ambient cyan drop shadow.
- **Interaction:** Depresses to 56px scale on tap with an instant haptic pulse. Transitions to a split bottom-sheet menu when long-pressed (Scan OCR vs. Manual Entry).

### 2. Receipt Breakdown Cards
- **Geometry:** Elevated white container (`#FFFFFF`), `rounded-2xl`, with a subtle perforated divider style or dashed border separating item list from totals.
- **Content Row:** Three-column layout featuring Item Name (left, bold Inter 14px), Participant Avatar Rings (center), and Tabular Price (right, bold `currency-item` style, e.g., `Rp 35.000`).
- **Subtotal/Tax Section:** Tonal surface-variant (`#F1F5F9`) background container pinned to the bottom of the card displaying subtotal, service charge (e.g., 5%), PB1/tax (e.g., 10%), and Grand Total.

### 3. Status Badges ("Lunas" & "Belum Lunas")
- **Pill Badge Architecture:** Full pill radius (`rounded-full`), padding `0.25rem 0.75rem` (`space-xs` vertical, `space-sm` horizontal), label font `label-sm` in all caps.
- **Settled ("Lunas"):** Background `#D1FAE5`, text `#065F46`, paired with a solid 6px dot or mini check icon in `#10B981`.
- **Pending ("Belum Lunas"):** Background `#FFE4E6`, text `#9F1239`, paired with a solid 6px dot or alert icon in `#F43F5E`.

### 4. Participant Split Chips
- **Interactive Toggles:** Pill-shaped chips (`rounded-full`) used to toggle who is sharing an item.
- **Deselected State:** Transparent surface with a 1px border in `#E2E8F0`, slate text (`#475569`).
- **Selected State:** Light primary container (`#E0F2FE`), 1px border in `#0284C7`, primary text (`#0284C7`), showing the user’s initials or avatar with a check overlay.

### 5. Input Fields & Currency Inputs
- **Base Fields:** Tonal background (`#F1F5F9`), `rounded-xl`, zero border in default state, transitioning to a 2px `#0284C7` ring on focus.
- **Currency Field:** Prominent fixed prefix `Rp` in muted slate (`#64748B`), large typography (`title-lg` or `currency-display`), automatic real-time thousand-point formatting as the user types.

### 6. Checkboxes & Radio Buttons
- **Shape:** Rounded-md (8px) for checkboxes, circular for radio.
- **Active State:** Ocean Blue (`#0284C7`) fill with bold white iconography.
- **Inactive State:** 2px stroke in `#CBD5E1` against `#FFFFFF` background.