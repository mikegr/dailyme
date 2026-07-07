---
name: Cyanic Studio
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#bacac6'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#859491'
  outline-variant: '#3c4a47'
  surface-tint: '#3adccc'
  primary: '#66fdec'
  on-primary: '#003732'
  primary-container: '#40e0d0'
  on-primary-container: '#006058'
  inverse-primary: '#006a62'
  secondary: '#76d6d5'
  on-secondary: '#003737'
  secondary-container: '#007f7f'
  on-secondary-container: '#ddfffe'
  tertiary: '#84f8ff'
  on-tertiary: '#003739'
  tertiary-container: '#00e0e9'
  on-tertiary-container: '#005f63'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#61f9e9'
  primary-fixed-dim: '#3adccc'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#005049'
  secondary-fixed: '#93f2f2'
  secondary-fixed-dim: '#76d6d5'
  on-secondary-fixed: '#002020'
  on-secondary-fixed-variant: '#004f4f'
  tertiary-fixed: '#63f7ff'
  tertiary-fixed-dim: '#00dce5'
  on-tertiary-fixed: '#002021'
  on-tertiary-fixed-variant: '#004f53'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  headline-lg:
    fontFamily: Montserrat
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Montserrat
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
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
  code-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.01em
  label-caps:
    fontFamily: Montserrat
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.08em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-margin: 20px
  gutter: 12px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
  safe-area-bottom: 34px
---

## Brand & Style

The design system is engineered for a high-end mobile editing experience that feels both surgical and sophisticated. The brand personality is "Technical Luxury"—combining the precision of a professional developer tool with the sleek, polished aesthetics of a premium lifestyle app.

The style leverages **Glassmorphism** and **Corporate Modern** influences. It utilizes deep, dark canvases to make vibrant turquoise accents pop, creating a sense of depth and focus. The emotional response should be one of "effortless power"—a workspace that feels expansive despite the mobile form factor, evoking clarity through transparency and light.

## Colors

The palette is anchored in a high-contrast dark mode environment.

- **Primary (Vibrant Turquoise):** Used for primary actions, active states, and brand identifiers.
- **Secondary (Deep Teal):** Used for structural elements, secondary buttons, and subtle background fills.
- **Tertiary (Neon Cyan):** Reserved for highlights, notifications, and interactive "glow" effects.
- **Background (Deep Slate):** A rich `#0F172A` serves as the foundation, providing more depth than pure black.
- **Surfaces:** Utilize semi-transparent versions of the primary color (5-12% opacity) to create the glass effect.

## Typography

This design system uses **Montserrat** for display and navigational elements to provide a confident, geometric character. **Inter** is used for all functional text and file content to ensure maximum legibility and a systematic feel.

For the editor view, Inter’s medium weight at a slightly smaller scale (13px) mimics a monospaced feel while remaining more readable on high-density mobile displays. All labels should be uppercase with increased letter-spacing to distinguish them from editable content.

## Layout & Spacing

The layout follows a **Fluid Grid** model optimized for thumb-reachability on mobile devices. 

1.  **Margins:** A consistent 20px horizontal margin wraps all primary content.
2.  **The "Editor Zone":** The main text area is padded with 16px to ensure text doesn't touch the screen edges.
3.  **Vertical Rhythm:** An 8px base unit controls all spacing. 
4.  **Mobile Adaptivity:** Bottom-sheet modals are preferred over full-screen transitions for secondary settings to maintain the "layered" glass feel. High-action buttons (Save, Run, Share) are docked at the bottom within the safe area.

## Elevation & Depth

Depth is achieved through **Tonal Layering** combined with **Glassmorphism**.

- **Level 0 (Base):** Deep Slate (`#0F172A`).
- **Level 1 (Cards/Lists):** 5% opacity Turquoise overlay with a 1px border at 10% opacity.
- **Level 2 (Modals/Popovers):** 12% opacity Turquoise overlay with a 20px Backdrop Blur.
- **Glow Effects:** Interactive elements like active toggles or the cursor use a soft Gaussian blur shadow (8px radius) tinted with the Neon Cyan (`#00F5FF`) at 30% opacity to create a "lithographic" glow.

## Shapes

The design system utilizes **Rounded** corners to soften the technical nature of a file editor. 

- **Small Components (Chips/Badges):** 4px - 8px radius.
- **Standard Components (Buttons/Inputs):** 12px radius.
- **Large Containers (Cards/Editor Sheets):** 16px - 24px radius (top only for bottom sheets).

All borders on glass surfaces should be 1px wide and use a linear gradient from top-left (low opacity white) to bottom-right (low opacity turquoise) to simulate a light-catching edge.

## Components

### Buttons
- **Primary:** Solid Vibrant Turquoise with dark navy text. Use a subtle outer glow on press.
- **Ghost:** Transparent background with a 1px Turquoise border. Text is Turquoise.

### Input Fields
- Darker than the background surface, 12px radius. 
- Active state: 1px Neon Cyan border and a subtle inner shadow to look "recessed."

### File List Items
- Clean rows with 12px vertical padding.
- Left-aligned icons in Deep Teal. 
- Right-aligned metadata (size/date) in low-opacity Inter font.

### The Editor Surface
- Line numbers should be in a muted Teal at 40% opacity.
- The "Active Line" should be highlighted with a 5% opacity Turquoise horizontal strip across the full width.

### Toolbar (Bottom Dock)
- A floating glass bar (80% opacity + blur) containing icons for common actions (Undo, Redo, Search, Settings).
- Icons should be minimal line-art style.