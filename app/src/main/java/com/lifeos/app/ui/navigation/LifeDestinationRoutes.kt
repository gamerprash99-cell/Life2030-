package com.lifeos.app.ui.navigation

import com.lifeos.app.core.life.LifeDestination

/**
 * Pure mapping from a [LifeDestination] to a [Screen] route, so every consumer
 * (AI assistant navigation, feedback opens, future calendar jumps) resolves
 * destinations the same way and the mapping stays unit-testable.
 */
fun LifeDestination.route(): String = when (this) {
    is LifeDestination.Note -> Screen.NoteEditor.createRoute(id)
    LifeDestination.Notes -> Screen.Notes.route
    LifeDestination.Diary -> Screen.Diary.route
    LifeDestination.Tasks -> Screen.Tasks.route
    LifeDestination.Habits -> Screen.Habits.route
    LifeDestination.Expenses -> Screen.Expenses.route
    LifeDestination.Home -> Screen.Home.route
}