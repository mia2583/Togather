package com.example.togather

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Togather)
        // if not logined -> activity_login
        // else -> activity_main
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}