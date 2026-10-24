# UI Style Guide --- aMACEzing Sales

This guide records the existing UI conventions in the Android app. The
implementation under `app/src/main/java/com/amacezing/sales/ui/` is the
source of truth if the guide and code ever differ. Reuse the existing
theme and components before introducing new visual patterns.

## Design direction

-   The app uses Jetpack Compose and Material 3.
-   The default visual direction is a restrained dark business
    dashboard: near-black backgrounds, layered dark surfaces, cyan
    primary accents, violet secondary accents, and semantic status
    colors.
-   Favor clear hierarchy, compact data presentation, rounded corners,
    subtle borders, and readable secondary text.
-   Avoid adding unrelated accent colors, gradients, heavy shadows, or
    decorative effects without a clear product need.
-   `MyApplicationTheme` defaults to dark mode and disables dynamic
    system colors. A light color scheme is also defined; reusable
    components should use `MaterialTheme.colorScheme` when they need to
    adapt to the selected theme.

## Theme and colors

The main definitions are in `ui/theme/Color.kt` and `ui/theme/Theme.kt`.

  Purpose                  Existing token          Value
  ------------------------ ----------------------- -----------
  Dark background          `AppBackground`         `#0B0F14`
  Standard surface         `AppSurface`            `#151B23`
  Elevated surface         `AppSurfaceElevated`    `#1C2430`
  Border / outline         `AppBorder`             `#2A3441`
  Primary text             `AppTextPrimary`        `#F3F6FA`
  Secondary text           `AppTextSecondary`      `#9AA6B2`
  Muted text               `AppTextMuted`          `#64748B`
  Primary accent           `AccentCyan`            `#00B8F0`
  Secondary accent         `AccentViolet`          `#A78BFA`
  Paid / success           `StatusPaid`            `#22C55E`
  Partial payment / info   `StatusPartiallyPaid`   `#00B8F0`
  Pending / warning        `StatusPending`         `#F59E0B`
  Cancelled / error        `StatusCancelled`       `#EF4444`

-   Prefer theme tokens and `MaterialTheme.colorScheme` over hardcoded
    hex values in screens.
-   Keep business-specific colors consistent with existing
    helpers/configuration such as `LocalBusinesses`,
    `parseBusinessColor`, and `colorOf`.
-   Use semantic status colors only for their intended meaning; do not
    use them as arbitrary decoration.

## Typography and spacing

-   Prefer Material 3 typography styles for screen titles, dialog
    titles, body copy, and form labels.
-   Dashboard metric labels are compact (about 13 sp); values are bold
    and sized to fit; subtitles are around 12 sp.
-   Filter section labels use 11 sp, semibold, uppercase text, and 1 sp
    letter spacing.
-   Compact filter chips use 13 sp text. Status and business badges use
    about 11 sp medium/semibold text.
-   Use secondary/on-surface-variant colors for explanatory copy;
    reserve primary text for titles, key values, and main content.
-   Typical screen content uses 16 dp horizontal padding. Lists commonly
    use about 12 dp vertical spacing between items. Follow the nearest
    existing screen where it differs.
-   Use an 8 dp spacing rhythm by default, with 4 dp adjustments where
    needed. Avoid arbitrary, inconsistent spacing values.

## Buttons and actions

Prefer shared action components from `ui/components/DialogComponents.kt`
and related component files.

-   **Primary action:** use `PrimaryActionButton` for the main positive
    action, such as Save, Confirm, or Duplicate.
-   **Dismissal / secondary action:** use `DismissActionButton` for
    Cancel, Back, and non-destructive dismissal. It should not compete
    visually with the primary action.
-   **Destructive action:** use `DestructiveActionButton` for
    irreversible actions such as Delete. Communicate danger with the
    error/destructive color, not the normal primary accent.
-   Use outlined or text actions for lower-emphasis alternatives. Do not
    make every action a filled button.
-   Keep labels short and action-oriented. Add icons only when they
    improve recognition.
-   Maintain consistent button height, spacing, and alignment within an
    action row.
-   Preserve enabled, disabled, and in-progress states. Do not signal
    success before the operation completes.
-   Use `CardActionBar` and `CardActionButton` for actions attached to
    cards where appropriate.
-   Icon-only actions need meaningful content descriptions and usable
    touch targets.

## Cards and surfaces

-   Prefer Material 3 `Card` / `Surface` and existing components such as
    `MetricCard`, business comparison/product breakdown cards, and
    profile shortcut cards.
-   Use `AppSurface` for standard dark surfaces, `AppSurfaceElevated`
    for nested/elevated content, and `AppBorder` for subtle separation.
-   Rounded corners are a defining part of the UI. Existing compact
    chips and badges commonly use 8--12 dp corners; the shared dialog
    uses 22 dp. For new cards, follow the closest existing card
    component rather than inventing a radius.
-   Metric cards should have a clear label, prominent value, and
    optional supporting subtitle. Do not rely on color alone to
    communicate a metric.
-   Clickable cards must expose the correct semantics and look
    interactive. Do not give non-interactive cards misleading click
    affordances.
-   Avoid unnecessary nested cards, excessive outlines, and heavy
    elevation.

## Dialogs and confirmation flows

Use `DialogWindow`, `AppDialog`, and `DialogDefaults` from
`ui/components/DialogComponents.kt` for custom, larger dialogs where
appropriate.

-   Shared dialog defaults include a 22 dp corner radius, 20 dp content
    padding, 16 dp section spacing, 40 dp action buttons, a maximum
    width of 560 dp, and 16 dp screen margins.
-   Account for safe drawing/system bars, keyboard visibility, available
    height, and tablet width using the shared dialog helpers.
-   Keep the title/header and action area visible while long content
    scrolls. Scroll the content region rather than the entire dialog
    including its buttons.
-   When matching the current standard `AlertDialog` treatment, use
    `DialogDefaults.CornerRadius`, `MaterialTheme.colorScheme.surface`,
    and zero tonal elevation.
-   Use `PrimaryActionButton` for confirmation, `DismissActionButton`
    for cancellation, and `DestructiveActionButton` for destructive
    confirmation.
-   Make destructive consequences explicit. Do not save or delete until
    the user confirms.
-   Keep form fields full-width and consistently spaced. Place
    validation messages beside the relevant field and preserve entered
    values when validation fails.
-   Dialogs must remain usable on short screens and with the keyboard
    open. Avoid hardcoded heights that hide actions behind system
    navigation.

## Chips, filters, and badges

-   Use `CompactFilterChip` for compact, single-line filters and
    business selectors.
-   Existing compact chip defaults: 40 dp height, 14 dp horizontal
    padding, 12 dp corner radius, 1 dp border, 17 dp icon, 6 dp
    icon-to-label gap, and commonly 8 dp between neighboring chips.
-   Selected chips use the selected/business color as the fill with dark
    `AppBackground` content. Unselected chips use a transparent
    background, subtle `AppBorder` outline, and `AppTextSecondary`
    content.
-   Keep labels on one line. For long filter rows, use horizontal
    scrolling as in the business selector instead of allowing ambiguous
    wrapping or clipping.
-   Use `FilterSectionLabel` for small uppercase secondary labels where
    useful.
-   Use `PaymentStatusBadge` for payment status and `BusinessBadge` for
    business type. Status badges use a low-opacity status-color
    background with status-colored text; payment badges use 12 dp
    corners and business badges use 8 dp corners.
-   Chips represent selectable options; badges represent read-only
    state/category. Do not interchange their semantics.
-   Selected state must be communicated accessibly, not through color
    alone.

## Screens and layout

-   Build screens from Compose components and Material 3 primitives.
    Keep screen-level state/business logic in the existing state
    owners/ViewModels and reusable UI in `ui/components/`.
-   Use `LazyColumn` for long or data-heavy lists. Respect safe insets
    and keep important actions clear of system bars and the keyboard.
-   A typical screen uses 16 dp horizontal padding, compact section
    gaps, and consistent list-item spacing.
-   Screen headings should be easy to scan: title first, then a short
    secondary description where useful.
-   Keep loading, empty, error, and populated states intentional and
    consistent. Empty states should provide a useful next step when
    appropriate.
-   Reserve floating action buttons for the screen's primary
    creation/action flow; avoid competing floating actions.
-   Preserve existing test tags, semantics, navigation, and state
    handling during UI refactors.

## Forms and inputs

-   Prefer Material 3 fields such as `OutlinedTextField`.
-   Use clear labels and mark required fields; placeholders must not be
    the only labels.
-   Keep field widths and vertical spacing consistent.
-   Choose appropriate keyboard types, capitalization, and validation
    for the data being entered.
-   Show validation near the relevant field and retain user input when
    validation fails.

## Accessibility and interaction

-   Maintain adequate foreground/background contrast and never use color
    as the only status indicator.
-   Provide content descriptions for meaningful icons and accessible
    labels/roles for custom clickable elements.
-   Ensure chips expose selected state and custom controls expose
    appropriate semantics.
-   Keep touch targets usable, text scalable, and important content
    available on smaller devices.
-   Preserve visible focus, pressed, and disabled states through
    Material components or explicit state styling.
-   Preserve system and back-button dismissal behavior for dialogs.

## Implementation rules

1.  Search for an existing shared component before creating a new style.
2.  Reuse theme tokens and `MaterialTheme.colorScheme`; avoid scattering
    raw hex values through screen files.
3.  Prefer shared button, dialog, chip, badge, and card components for
    consistent appearance and behavior.
4.  If a reusable pattern is genuinely missing, add it under
    `ui/components/`, document its purpose, and migrate repeated use
    where safe.
5.  Keep visual-only changes separate from business-logic changes.
6.  Preserve semantics, test tags, state handling, and navigation.
7.  Run the project's available Gradle build and tests after
    implementation changes.

## Unused-file audit policy

A file that has no obvious direct reference is not necessarily unused.
Kotlin symbols may be reached indirectly through dependency injection,
reflection, generated code, Android manifest entries, resources, Gradle
configuration, tests, or CI/release workflows.

-   Do not delete files based only on filename searches or text search.
-   Audit production references separately from test-only, build-only,
    manifest, resource, and CI references.
-   Review possible compatibility aliases, parallel local/Firestore
    repositories, migration/import helpers, update/release files, and
    Firebase configuration before considering removal.
-   Confirm candidates using IDE Find Usages or equivalent symbol
    analysis, inspect indirect references, and run a clean Gradle
    build/test suite.
-   Record each candidate, evidence, and confidence before deleting
    anything.
-   This guide does not declare any file safe to delete; a dedicated
    reference/build audit is required.

## Source files to consult

-   Theme and palette: `ui/theme/Color.kt`, `ui/theme/Theme.kt`
-   Shared buttons and dialogs: `ui/components/DialogComponents.kt`,
    `ui/components/ConfirmDeleteDialog.kt`
-   Chips and badges: `ui/components/FilterChips.kt`,
    `ui/components/BusinessFilterChips.kt`,
    `ui/components/StatusBadge.kt`
-   Cards and card actions: `ui/components/MetricCard.kt`,
    `ui/components/CardActions.kt`,
    `ui/components/ProfileShortcutsCard.kt`
-   Screen examples: `ui/screens/DashboardScreen.kt`,
    `ui/screens/ProductsScreen.kt`
