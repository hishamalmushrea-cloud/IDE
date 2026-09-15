package com.example.multimodule.feature

import com.example.multimodule.core.CoreUtils

object FeatureManager {
    fun executeFeature(user: String): String {
        return CoreUtils.getGreeting(user) + " [Processed by Feature module]"
    }
}
