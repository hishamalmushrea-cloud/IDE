package com.example.multimodule.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.multimodule.feature.FeatureManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = FeatureManager.executeFeature("AIDE Next User")
        setContentView(tv)
    }
}
