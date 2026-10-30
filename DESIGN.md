---
name: Ballnote
description: Tap. Score. Play.
colors:
  primary: "#4C609B"
  primary-container: "#DCE2FF"
  on-primary-container: "#10244F"
  surface: "#FAF8FF"
  dark-primary: "#B4C5FF"
  dark-primary-container: "#304576"
  dark-surface: "#11131B"
rounded:
  scoreboard: "28dp"
  scoring-key: "18dp"
spacing:
  page: "20dp"
  section: "16dp"
  controls: "8dp"
---

## Overview

Operate mode. A calm native sports utility. The scoreboard and reachable controls lead; history and settings recede during scoring.

## Colors

Use Material 3 roles. Static blue is the default; System/Light/Dark and optional Android 12+ dynamic colours are supported. Error roles mark wickets and consequential removals.

## Typography

System sans with Material typography for headings, labels and body. Score uses 72sp bold and wickets 36sp. Large text and long totals use a stacked layout with a smaller score size. Respect system font scale and permit scrolling.

## Layout

20dp horizontal padding; 16dp general section spacing and 8dp in live scoring. The scoring grid is 0/1/2 then 3/4/6. Wicket/Wide/No-ball follow, then More, Undo and save status on one row. Root destinations are Matches, Teams, Settings. Live scoring has a Back affordance and details action.

## Shapes

28dp scoreboard, 18dp scoring buttons; native Material shapes elsewhere.

## Components

Filled keys for 4/6, tonal keys for other run outcomes, outlined extras, and a bottom sheet for combined outcomes. Corrections and replacement restore require confirmation. Ordinary scoring never does.

## Do's and Don'ts

Preserve controls in fixed order. Keep at least 48dp touch targets. Use native semantics, theme roles, screen insets and system font sizing. Do not add decorative animations, charts, networking or player-level complexity.
