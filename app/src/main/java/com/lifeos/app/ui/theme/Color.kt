package com.lifeos.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * LifeOS Design System — Section 53/54 of the spec.
 * "Premium, glassmorphism, minimal, modern, futuristic, calm, personal."
 * Kept deliberately restrained: glass surfaces are used for key cards only,
 * never globally, per the spec's explicit warning against overusing the effect.
 */

// Base surfaces
val LifeOSBackgroundLight = Color(0xFFF5F6FA)
val LifeOSBackgroundDark = Color(0xFF0E0F13)

val LifeOSSurfaceLight = Color(0xFFFFFFFF)
val LifeOSSurfaceDark = Color(0xFF17181D)

// Glass surface tints (translucent, layered on background/blur)
val GlassLight = Color(0x99FFFFFF)   // white @ 60%
val GlassDark = Color(0x992A2B33)    // dark slate @ 60%
val GlassBorderLight = Color(0x33FFFFFF)
val GlassBorderDark = Color(0x1FFFFFFF)

// Brand accent — calm indigo-violet, avoids "childish" gamification colors
val LifeOSPrimary = Color(0xFF6C63FF)
val LifeOSPrimaryVariant = Color(0xFF574FDB)
val LifeOSSecondary = Color(0xFF00C2A8)

// Semantic
val LifeOSSuccess = Color(0xFF2ECC71)
val LifeOSWarning = Color(0xFFF5A623)
val LifeOSDanger = Color(0xFFE74C3C)

// Text
val LifeOSTextPrimaryLight = Color(0xFF14151A)
val LifeOSTextSecondaryLight = Color(0xFF6B6D76)
val LifeOSTextPrimaryDark = Color(0xFFF2F2F5)
val LifeOSTextSecondaryDark = Color(0xFFA6A8B3)

// Category accent colors (Expenses / Habits icons — Section 14/54)
val CategoryFood = Color(0xFFFF7A59)
val CategoryCafe = Color(0xFF9B6B43)
val CategoryShopping = Color(0xFFEA5FA0)
val CategoryTravel = Color(0xFF2F9BFF)
val CategoryEntertainment = Color(0xFFB15CFF)
val CategoryEducation = Color(0xFF3FBF7F)
val CategoryBills = Color(0xFFFFB13F)
val CategoryHealth = Color(0xFFFF5C5C)
val CategorySubscriptions = Color(0xFF5C7CFF)
val CategoryOther = Color(0xFF8A8C99)
