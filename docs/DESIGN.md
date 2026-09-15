# LifeOS — Design System & UI/UX Specification

**Status:** Current design specification — 2026-09-16

## 1. Design direction

LifeOS uses a premium, calm consumer-product language:

- soft and spacious
- cute but mature
- minimal rather than decorative
- lavender/violet primary identity
- restrained pink/magenta accents
- near-white surfaces
- large rounded cards
- subtle shadows and glow
- expressive but familiar icons
- tactile micro-interactions

The provided product screenshots and the LifeOS Figma exploration establish the visual direction. Screenshots are references, not pixel-perfect requirements; existing LifeOS functionality and Android conventions remain authoritative.

## 2. Core palette

| Token | Light value | Purpose |
|---|---|---|
| `LifeOSBackgroundLight` | `#FDF8FF` | Main page background |
| `LifeOSSurfaceLight` | `#FFFFFF` | Primary cards/surfaces |
| `LifeOSPrimary` | `#7C4DFF` | Primary actions/progress |
| `LifeOSPrimaryVariant` | `#5B21B6` | Strong primary text/accent |
| `LifeOSSecondary` | `#D946EF` | Controlled pink/magenta accent |
| `LifeOSAccentLavender` | `#EADDFF` | Selected states/chips |
| `LifeOSAccentPink` | `#FCE7F3` | Soft secondary accents |
| `LifeOSTextPrimaryLight` | `#21005D` | Primary light-theme text |
| `LifeOSTextSecondaryLight` | `#7A6A91` | Secondary light-theme text |

Dark-theme values live in `ui/theme/Color.kt` and must be used through `MaterialTheme.colorScheme` where possible. Screens must not hardcode light-only colors when an existing theme token can express the same intent.

## 3. Typography

Use the existing Material 3 typography system and the LifeOS theme rather than per-screen font declarations.

Hierarchy:

- display/headline: screen identity and major totals
- title: card and section identity
- body: explanatory/content text
- label: chips, compact actions and metadata

Do not use typography size as the only way to communicate hierarchy; combine weight, spacing, surface treatment and semantics.

## 4. Spacing

Shared spacing lives in `ui/theme/Spacing.kt`.

Primary rules:

- screen padding: `24.dp` where the viewport permits
- compact content padding: `16.dp`
- card-to-card spacing: approximately `12.dp`
- section spacing: approximately `20.dp`
- minimum interactive target: `48.dp`
- avoid arbitrary offsets and absolute positioning

Responsive layouts may reduce visual density on small screens, but should not solve overflow by clipping content.

## 5. Shapes and surfaces

The LifeOS shape language is rounded and friendly:

- cards: approximately `28.dp`
- major containers: up to `32.dp`
- buttons/chips: pill or strongly rounded
- borders: subtle, low-contrast
- elevation: soft and restrained

Avoid heavy shadows, excessive glassmorphism, or gradients on every component.

## 6. Reusable Compose components

Current shared primitives include:

- `LifeOSCard`
- `LifeOSGradientButton`
- `LifeOSBadge`
- `LifeOSSectionHeader`
- `LifeOSBottomBar`
- `LifeOSIntelligenceCard`
- `GlassCard` / existing glass components

A new reusable component should only be introduced when the interaction or visual rule appears in multiple screens.

## 7. Home composition

Target hierarchy:

**Header → Today → Quick access → Tasks → Habits → Spending/Recent Activity → Intelligence → navigation**

The greeting and date must come from real current time/state. Statistics must come from existing ViewModels/use cases/repositories. Reference values such as `3 of 6 tasks` or `4 day streak` must never become production constants.

## 8. Onboarding

Four-page structure:

1. Welcome to LifeOS
2. Your life. Your data.
3. AI, on your terms
4. Everything connects

Required actions:

- Skip
- Back/Next where applicable
- Get started
- Restore a LifeOS backup

Restore uses Android `ACTION_OPEN_DOCUMENT` with `application/json` and passes the selected file through `BackupRepository`.

## 9. App Lock UX

LifeOS exposes:

- None
- Biometric
- PIN / Passcode

Biometric authentication is always Android's `BiometricPrompt`. LifeOS never stores biometric templates and cannot determine which enrolled person authenticated.

First-time biometric setup should:

1. explain device-level biometric behavior;
2. check availability;
3. guide the user to Android enrollment/security settings when necessary;
4. re-check availability on return;
5. require successful authentication before enabling the setting.

The PIN path remains independent and uses the existing secure hashing/recovery architecture.

## 10. Settings

Settings use grouped sections rather than one undifferentiated list:

- Appearance
- Security
- Privacy
- Notifications / Reminders
- Diary
- Alarms/reminder access where supported by existing infrastructure
- Intelligence
- Backup & Restore
- About

Only supported functionality should be exposed. A reference screenshot must not be treated as permission to invent an unrelated feature.

## 11. Bottom navigation and FAB

Navigation remains Navigation Compose with the existing `NavController`.

The visual treatment uses:

- floating rounded container
- animated selected pill
- icon + label for selected state
- safe-area-aware placement
- tactile press feedback

The FAB follows the same rounded/gradient language and must retain its existing action semantics.

## 12. Motion system

| Motion | Target |
|---|---:|
| Press feedback | 100–180 ms |
| Small state change | 180–280 ms |
| Screen/content transition | 250–350 ms |
| Typical press scale | ~0.985 |

Preferred Compose APIs:

- `animateFloatAsState`
- `animateColorAsState`
- `animateDpAsState`
- `AnimatedVisibility`
- `AnimatedContent`
- `animateContentSize`
- `updateTransition`
- spring animation for tactile feedback where appropriate

Motion should communicate state, not decorate every element. Avoid continuous bouncing and long transitions.

## 13. Responsive behavior

Design for:

- small phones
- large phones
- font scaling
- gesture navigation
- 3-button navigation
- landscape where a screen reasonably supports it

Use `WindowInsets`, adaptive Compose layouts and scroll containers. Never rely on fixed absolute screen coordinates.

## 14. Accessibility

Every actionable element needs:

- a meaningful semantic/content description when its visual label is insufficient
- at least the recommended touch target
- readable contrast
- font-scale-safe layout
- non-color-only state communication

## 15. Dark mode

Dark mode is preserved through `LifeOSTheme`. New UI must consume theme colors or dedicated dark/light tokens rather than embedding light-theme surface colors directly into screen code.

## 16. Performance

UI work must remain compatible with the existing local architecture:

- prefer `LazyColumn`/`LazyRow` for long collections;
- avoid repeated database work from composition;
- keep expensive analysis outside composition;
- keep animation scopes small;
- avoid unnecessary remembered state;
- preserve ViewModel/repository ownership of feature state.

## 17. Figma relationship

A dedicated LifeOS 2026 Figma exploration was created for the premium UI direction. Figma is the visual design reference; the Android source remains the implementation authority.

When converting Figma designs to Compose:

1. inspect existing LifeOS components/tokens first;
2. map Figma colors/spacing/shapes into `ui/theme`;
3. reuse existing repositories/ViewModels/navigation;
4. never paste React/Tailwind output into the Android project;
5. keep assets local or tied to the existing data model where required;
6. verify the result on real Android screen sizes.

## 18. Product guardrails

Visual references must not introduce:

- cloud services
- external AI providers
- fake statistics
- fake biometric systems
- unrelated alarm products
- duplicate navigation stacks
- direct UI-to-Room access
- destructive database changes
