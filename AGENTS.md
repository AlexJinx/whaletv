# WhaleTV Agent Notes

## Project Context

- This Android TV project is based on [iptv-org/iptv](https://github.com/iptv-org/iptv).
- The app is currently a clean rebuild baseline: the redesigned homepage plus the iptv-org data core.
- The active UI design reference is the local Figma Make source at `design/WhaleTV Android TV Homepage/`. Prefer this local design source when matching UI.

## Local Design Source

- Start with `design/WhaleTV Android TV Homepage/src/app/App.tsx`.
- The homepage reference component is `design/WhaleTV Android TV Homepage/src/app/components/WhaleTVHome.tsx`.
- Design tokens live in `design/WhaleTV Android TV Homepage/src/styles/theme.css`; match the dark TV palette (`#0d0f12`, `#00c8d4`, `#151921`, `#1c2233`, `#dde4f0`, `#6b7fa3`) unless Android app constraints require adaptation.
- The design source is intentionally minimal. Do not reintroduce generic Figma Make or shadcn/ui scaffold files unless a new design explicitly depends on them.

## UI Design Baseline

- Treat the homepage as the visual baseline for every new or redesigned screen.
- Before designing a new feature UI, first match the homepage's layout scale, spacing, typography hierarchy, dark TV palette, and remote-control focus style.
- Reuse homepage dimensions where applicable: 52dp global top bar, 56dp secondary bar when needed, 220dp left navigation, and 28dp horizontal / 20dp vertical content padding.
- New feature screens may adapt the layout to their workflow, but their base sizing, fonts, colors, borders, and focus states should feel inherited from the homepage rather than like a separate design system.

## UI Prompt Interpretation

- Treat "homepage style", "match the homepage", or "same visual system as the homepage" as a request to inherit the homepage's density, typography, color palette, spacing rhythm, borders, and remote-control focus behavior. Do not automatically add a left navigation rail for these prompts.
- Treat "homepage framework", "homepage layout framework", "same framework as the settings page", or "left menu plus content area" as a request to reuse the full homepage-style skeleton: 52dp global top bar, optional 56dp secondary bar, 220dp left rail, and right content area.
- When using the full homepage framework, copy the homepage left rail proportions: 220dp width, 12dp horizontal / 20dp vertical padding, 44dp row height, 20dp icons, 16sp medium labels, and the cyan left selection rail.
- When using the full homepage framework, copy the homepage content proportions: 28dp horizontal / 20dp vertical padding, 24sp title plus 16sp supporting text, compact right-side status text or chip, and the homepage card-grid width rhythm.
- Treat "homepage content-card style" as a request to reuse only the right content area's title row and card-grid rhythm, without adding the full page skeleton or left navigation.
- Player pages, full-screen experiences, dialogs, single-item editors, and temporary action screens should not use a left navigation rail unless the user explicitly asks for the homepage framework.
- If the user only says "reference the homepage" and the page type does not make the intended layout obvious, decide whether the request means full homepage framework, homepage visual style only, or homepage content-card style. Ask a clarification question when that choice would materially change the screen.
- In Android Compose screens that need homepage-equivalent scale, use the same density baseline as the homepage: `CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = platformDensity.fontScale))`.

### UI Prompt Examples

- "Build this page with the homepage framework" means use left navigation plus right content area.
- "Keep this page in the homepage style, but no left rail" means inherit the visual system while designing a workflow-specific layout.
- "Use the homepage content-card style for this list" means reuse the right content title row and card grid only.
- "Make this a full-screen operation page, but visually consistent with the homepage" means do not use a left rail; inherit only the visual language.

## Data Source Policy

- WhaleTV does not provide user-configurable playlist or XMLTV data sources.
- Channel and EPG data should come from the project's open-source defaults, primarily iptv-org playlist data and EPG/API sources discovered from that data.
- Do not add custom source URL inputs, custom XMLTV inputs, arbitrary source testing forms, or "clear custom source" actions unless the user explicitly reverses this policy.

## Settings Card Consistency

- Settings pages use the homepage framework and homepage card-grid rhythm.
- All cards inside Settings pages must share the same width, height, corner radius, background, border, padding, and focus treatment.
- Do not make individual Settings menu cards taller, wider, or multi-column just because their content is longer; truncate or split content into additional same-size cards instead.

## Change Summary Communication

- After each completed implementation, explain the change in user-facing terms before listing code files.
- Clearly say which screen was changed, such as the homepage, player page, or settings page.
- Describe what feature was added or adjusted, and what the user can now see, click, configure, or experience differently.
- If the change includes background logic, explain its purpose in plain language instead of only naming classes or technical APIs.
- If the working tree includes older uncommitted changes, distinguish the current change from previous leftover changes.
- Keep file references as supporting detail after the plain-language product explanation, not as the main explanation.
