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
