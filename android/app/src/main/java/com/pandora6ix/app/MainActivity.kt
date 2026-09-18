package com.pandora6ix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pandora6ix.app.ui.theme.Pandora6ixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Pandora6ixApp() }
    }
}

@Composable
fun Pandora6ixApp() {
    Pandora6ixTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Text(text = "Pandora6ix")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Pandora6ixAppPreview() {
    Pandora6ixApp()
}
