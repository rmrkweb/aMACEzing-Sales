package com.amacezing.sales.ui.theme

import androidx.compose.ui.graphics.Color

// Visual Design System Palette (Facebook & LinkedIn Inspired Clean Dark Aesthetic)
val AppBackground = Color(0xFF0B0F14)
val AppSurface = Color(0xFF151B23)
val AppSurfaceElevated = Color(0xFF1C2430)
val AppBorder = Color(0xFF2A3441)

val AppTextPrimary = Color(0xFFF3F6FA)
val AppTextSecondary = Color(0xFF9AA6B2)
val AppTextMuted = Color(0xFF64748B)

// Accents
val AccentCyan = Color(0xFF00B8F0)
val AccentViolet = Color(0xFFA78BFA)

// Semantic Status Colors
val StatusPaid = Color(0xFF22C55E)          // Accessible success green
val StatusPartiallyPaid = Color(0xFF00B8F0)  // Cyan accent
val StatusPending = Color(0xFFF59E0B)       // Accessible warning amber
val StatusCancelled = Color(0xFFEF4444)     // Accessible error red

// Business Divisions
val EngravingCyan = Color(0xFF00B8F0)
val SintraBoardViolet = Color(0xFFA78BFA)

// Compatibility aliases for existing references across codebase
val PrimaryTeal = AccentCyan
val PrimaryTealLight = AccentCyan
val PrimaryTealDark = Color(0xFF00384D)
val SecondaryAmber = StatusPending
val SecondaryAmberLight = Color(0xFFFBBF24)
val SecondaryAmberDark = Color(0xFFB45309)
val EngravingBrown = AccentCyan
val SintraBoardBlue = AccentViolet
val SurfaceDark = AppBackground
val SurfaceCardDark = AppSurface
val TextPrimaryDark = AppTextPrimary
val TextSecondaryDark = AppTextSecondary
val SurfaceLight = Color(0xFFF8FAFC)
val SurfaceCardLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)

// Technology theme aliases
val TechBgNearBlack = AppBackground
val TechSurfaceDeepNavy = AppSurface
val TechSurfaceSecondary = AppSurfaceElevated
val TechAccentCyan = AccentCyan
val TechAccentViolet = AccentViolet
val TechAccentDeepBlue = Color(0xFF0284C7)
val TechTextPrimary = AppTextPrimary
val TechTextSecondary = AppTextSecondary
val TechTextSilver = AppTextPrimary
val TechBorderSubtle = AppBorder
val TechBadgeBg = AppSurfaceElevated
