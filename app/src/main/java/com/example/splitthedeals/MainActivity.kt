package com.example.splitthedeals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.example.splitthedeals.ui.navigation.AppNavHost
import com.example.splitthedeals.ui.theme.SplitDealsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_SplitDeals_Splash)
        super.onCreate(savedInstanceState)
        setContent {
            SplitDealsTheme {
                AppNavHost()
            }
        }
    }
}
