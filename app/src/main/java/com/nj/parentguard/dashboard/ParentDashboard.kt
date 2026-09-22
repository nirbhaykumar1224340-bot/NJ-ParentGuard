package com.nj.parentguard.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ParentDashboard(onPairAnother: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var children by remember { mutableStateOf(0) }
    var lastUpdate by remember { mutableStateOf("No location yet") }
    var status by remember { mutableStateOf("Connected") }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        FirebaseFirestore.getInstance()
            .collection("families").document(uid).collection("children")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    status = "Connection error"
                    return@addSnapshotListener
                }
                children = snapshot?.size() ?: 0
                val latest = snapshot?.documents?.maxByOrNull {
                    it.getTimestamp("pairedAt")?.toDate()?.time ?: 0L
                }
                lastUpdate = latest?.getTimestamp("pairedAt")?.toDate()?.toString()
                    ?: "No location yet"
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Parent Dashboard", style = MaterialTheme.typography.headlineMedium)
        Text("Status: $status")

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Paired children", style = MaterialTheme.typography.titleMedium)
                Text(children.toString(), style = MaterialTheme.typography.headlineLarge)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Latest device update", style = MaterialTheme.typography.titleMedium)
                Text(lastUpdate, modifier = Modifier.padding(top = 6.dp))
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Location", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Offline locations are queued on the child device and synced when internet returns.",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Device status", style = MaterialTheme.typography.titleMedium)
                Text("Battery and last-seen telemetry will appear here.", modifier = Modifier.padding(top = 6.dp))
            }
        }

        Button(onClick = onPairAnother, modifier = Modifier.fillMaxWidth()) {
            Text("Pair another child")
        }
    }
}
