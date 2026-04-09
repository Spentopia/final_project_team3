package com.ict.spentopia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ict.spentopia.navigation.AppNavGraph
import com.ict.spentopia.ui.theme.SpentopiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpentopiaTheme {
                AppNavGraph()
            }
        }
    }
}