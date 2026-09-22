package com.nj.parentguard
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text("ParentGuard", style=MaterialTheme.typography.headlineMedium)
                    Text("Parental-control MVP")
                    Button(Modifier.padding(top=20.dp), onClick={ requestPermissions(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.POST_NOTIFICATIONS,
                            android.Manifest.permission.CAMERA,
                            android.Manifest.permission.READ_PHONE_STATE,
                            android.Manifest.permission.RECEIVE_SMS,
                            android.Manifest.permission.READ_SMS
                        ), 1001)
                    }) { Text("Request permissions") }
                }
            }
        }
    }
}
