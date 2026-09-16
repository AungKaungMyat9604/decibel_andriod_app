# Menzies Design Wash UI

Monorepo for **@menzies-mariesta-com/menzies-design-wash-ui** (publishable npm package) and **@menzies/wash-demo** (private gallery and documentation site).

Watercolor-themed design system: pigment themes, wash atmosphere, CSS utilities, optional React adapter, ApexCharts-based analytics, and transactional email helpers.

## Structure

```text
watercolor-dashboard/
├── apps/demo/                      # Gallery + in-app docs (@menzies/wash-demo)
└── packages/menzies-design-wash-ui # npm package (@menzies-mariesta-com/menzies-design-wash-ui)
```

## Requirements

- Node.js **20+**

## Develop

From the repo root:

```bash
npm install
npm run dev          # demo site (Vite)
npm run build:lib    # package only
npm run build:demo   # demo only
npm run build        # package + demo
npm run lint         # demo lint (oxlint)
npm run preview      # preview demo production build
```

## Demo gallery

Run `npm run dev` and open the demo app. The sidebar includes:

| Section | Scope |
|---------|-------|
| **Docs** | Getting started, Theming, Tokens, Customize |
| **Components** | **115** gallery pages, alphabetized A-Z (buttons, forms, overlays, data display, and more) |
| **Charts** | **31** Apex chart categories plus an overview hub (line, area, bar, pie, heatmap, treemap, sunburst, violin, candlestick, dashboards, interactivity, and more) |
| **Templates** | **11** full-page layouts in five groups: **Auth** (screen, 2FA, forgot password, OTP), **Commerce** (checkout, payment), **Data** (data table), **Studio** (terminal logging, rich text, code editor, file tree), **Layout** (documentation layout) |

Each gallery page shows live previews with HTML, JSX, Svelte, and Kotlin snippets (imports included). Docs pages cover install, theming, tokens, and customization.

## Package usage

Published publicly on [npmjs.org](https://www.npmjs.com/org/menzies-mariesta-com) under `@menzies-mariesta-com` (also mirrored to GitHub Packages). Install from the public registry:

```bash
npm i @menzies-mariesta-com/menzies-design-wash-ui
```

For GitHub Packages instead, point the scope at `https://npm.pkg.github.com` (see `packages/menzies-design-wash-ui/.npmrc`).

React apps also need peer dependencies:

```bash
npm i @menzies-mariesta-com/menzies-design-wash-ui react react-dom
```

```tsx
import '@menzies-mariesta-com/menzies-design-wash-ui/styles.css'
import { WashProvider, Button } from '@menzies-mariesta-com/menzies-design-wash-ui'
```

### Entrypoints

| Import | Use |
|--------|-----|
| `@menzies-mariesta-com/menzies-design-wash-ui/styles.css` | Required stylesheet (tokens, pigments, utilities) |
| `@menzies-mariesta-com/menzies-design-wash-ui/core` | Framework-free: theme, ripple, tooltips, recipes, `initWash` |
| `@menzies-mariesta-com/menzies-design-wash-ui/react` | React components, provider, hooks |
| `@menzies-mariesta-com/menzies-design-wash-ui` | React adapter (alias of `/react`) |
| `@menzies-mariesta-com/menzies-design-wash-ui/theme` | Theme helpers |
| `@menzies-mariesta-com/menzies-design-wash-ui/icons` | Full Lucide UI icons + `DynamicIcon` / `iconNames` (React; lucide-react 1.28.0 inside Wash) |
| `@menzies-mariesta-com/menzies-design-wash-ui/icons/brands` | Curated brand marks (React; Simple Icons inside Wash) |
| `@menzies-mariesta-com/menzies-design-wash-ui/icons/brands/catalog` | Full brand catalog + `BrandIcon` by slug |
| `@menzies-mariesta-com/menzies-design-wash-ui/charts` | Pigment-aware ApexCharts components and theme helpers |
| `@menzies-mariesta-com/menzies-design-wash-ui/editors` | Optional rich text + code editors (web React; not on default barrel) |
| `@menzies-mariesta-com/menzies-design-wash-ui/email` | Transactional email builders (OTP verification) |

See [packages/menzies-design-wash-ui/README.md](./packages/menzies-design-wash-ui/README.md) for vanilla usage, charts, email, and customization details.

## License

Copyright (C) 2026 Zarni Hlawn. Licensed under the [GNU General Public License v3.0 or later](LICENSE) (GPL-3.0-or-later).
