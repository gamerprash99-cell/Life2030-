package com.lifeos.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lifeos.app.R

/**
 * The app's primary face: Poppins, bundled in `res/font` so the app stays
 * offline-first with no runtime font download. A geometric sans with open
 * counters reads as modern and premium at display sizes while staying highly
 * legible at body sizes, which is why it carries the whole hierarchy.
 */
val PoppinsFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/**
 * The diary's own hand: a real handwriting typeface (Kalam) bundled in
 * `res/font`, so the app stays offline-first with no runtime font download.
 *
 * Used only by the diary module for the things a person actually writes — a
 * memory's title and body, the day's date, the empty-page invitation — while
 * controls and small labels keep the app's legible default sans. Light is
 * reserved for the large date so it reads as a pen flourish rather than bold
 * body copy.
 */
val DiaryHandwriting = FontFamily(
    Font(R.font.kalam_light, FontWeight.Light),
    Font(R.font.kalam_regular, FontWeight.Normal),
    Font(R.font.kalam_bold, FontWeight.Bold)
)

/**
 * Calm, high-legibility hierarchy on Poppins — tuned for Aurora Noir.
 *
 * Display sizes carry a slightly tighter tracking so large headings feel
 * composed and "editorial" rather than loose; titles step up to SemiBold for a
 * firmer hierarchy; labels keep a touch of positive tracking so small caps read
 * as deliberate eyebrows. Line heights stay generous enough for 200% font
 * scaling without text colliding.
 */
val LifeOSTypography = Typography(
    displayLarge = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 50.sp, letterSpacing = (-0.8).sp),
    displayMedium = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 35.sp, lineHeight = 43.sp, letterSpacing = (-0.6).sp),
    displaySmall = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 29.sp, lineHeight = 37.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 23.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 27.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 25.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = PoppinsFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)
