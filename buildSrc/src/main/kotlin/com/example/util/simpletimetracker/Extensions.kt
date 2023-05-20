package com.example.util.simpletimetracker

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

fun Project.applyAndroidLibrary() = apply(from = "$rootDir/gradle/android_library.gradle")
