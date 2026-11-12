package com.example.uthauth

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

object AppConfig {
    private val rc = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build()
        )
        setDefaultsAsync(mapOf("greeting_prefix" to "Chào"))
    }

    fun greeting(): String = rc.getString("greeting_prefix")

    fun fetch(onDone: () -> Unit) {
        rc.fetchAndActivate().addOnCompleteListener { onDone() }
    }
}