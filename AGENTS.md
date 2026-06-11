# WhaleTV Agent Notes

## Project Context

- This Android TV project is based on [iptv-org/iptv](https://github.com/iptv-org/iptv).
- The UI design reference is [Whale TV Android TV UI](https://www.figma.com/design/6b5xPKYpg4JKBXyu1HHuMl/Whale-TV-Android-TV-UI).
- A local copy of the Figma Make source lives at `design/Whale TV Android TV UI/`. Prefer this local design source over the remote Figma URL when matching UI.

## Local Design Source

- Start with `design/Whale TV Android TV UI/src/app/App.tsx` for screen routing and the 1920x1080 scaled TV canvas.
- Screen references are in `design/Whale TV Android TV UI/src/app/components/`: `HomeScreen.tsx`, `PlayerScreen.tsx`, `EPGScreen.tsx`, `SearchScreen.tsx`, and `SettingsScreen.tsx`.
- Sample channel/category data is in `design/Whale TV Android TV UI/src/app/components/tvData.ts`.
- Design tokens live in `design/Whale TV Android TV UI/src/styles/theme.css`; match the dark TV palette (`#0d0f12`, `#00c8d4`, `#151921`, `#1c2233`, `#dde4f0`, `#6b7fa3`) unless Android app constraints require adaptation.
- Treat `design/Whale TV Android TV UI/guidelines/Guidelines.md` as placeholder content, not project-specific guidance.
