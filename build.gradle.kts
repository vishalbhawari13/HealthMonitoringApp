// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false  // ✅ Use stable AGP version
    id("com.android.library") version "8.2.2" apply false  // ✅ Add library module support
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false  // ✅ Add Kotlin support
}
