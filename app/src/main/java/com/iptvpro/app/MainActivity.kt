package com.iptvpro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.iptvpro.app.ui.navigation.AppNavigation
import com.iptvpro.app.ui.theme.IPTVProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IPTVProTheme {
                AppNavigation()
            }
        }
    }
}

@Preview
@Composable
fun MainPreview() {
    IPTVProTheme {
        AppNavigation()
    }
}
