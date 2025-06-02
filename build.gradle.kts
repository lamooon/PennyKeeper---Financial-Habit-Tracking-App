buildscript {
    extra.apply {
        set("room_version", "2.6.0")
    }

}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}