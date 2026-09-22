package com.nj.parentguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val missing = permissions.filter {
                    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ParentGuard", style = MaterialTheme.typography.headlineMedium)
                    Text("Parental-control MVP")
                    if (missing.isNotEmpty()) {
                        Text(
                            "Some permissions still need to be granted.",
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Button(
                            Modifier.padding(top = 20.dp),
                            onClick = { requestPermissions(missing.toTypedArray(), 1001) }
                        ) {
                            Text("Grant remaining permissions")
                        }
                    } else {
                        Text(
                            "Setup complete. No repeated in-app permission prompts.",
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }
            }
        }
    }
}
