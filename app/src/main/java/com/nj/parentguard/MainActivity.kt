package com.nj.parentguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.ContentValues
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import android.content.Intent
import android.os.BatteryManager
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.nj.parentguard.dashboard.ParentDashboard
import com.nj.parentguard.location.LocationTracking
import com.nj.parentguard.notification.ParentTelephonySyncWorker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS
    )

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var pendingCameraUri: android.net.Uri? = null
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) uploadCameraPhoto(uri)
        else uri?.let { contentResolver.delete(it, null, null) }
        pendingCameraUri = null
    }

    private fun captureCameraPhoto() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ParentGuard_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ParentGuard")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) { Toast.makeText(this, "Camera storage unavailable", Toast.LENGTH_SHORT).show(); return }
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    private fun uploadCameraPhoto(uri: android.net.Uri) {
        val uid = auth.currentUser?.uid
        if (uid == null) { Toast.makeText(this, "Sign in first", Toast.LENGTH_SHORT).show(); return }
        val ref = FirebaseStorage.getInstance().reference.child("users/$uid/camera/${System.currentTimeMillis()}.jpg")
        ref.putFile(uri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                db.collection("users").document(uid).collection("cameraCaptures").add(
                    mapOf("url" to url.toString(), "createdAt" to FieldValue.serverTimestamp())
                )
            }
            Toast.makeText(this, "Photo uploaded", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocationTracking.scheduleSync(this)
        ParentTelephonySyncWorker.schedule(this)
        setContent {
            MaterialTheme {
                ParentGuardApp()
            }
        }
    }

    @Composable
    private fun ParentGuardApp() {
        var role by remember { mutableStateOf<String?>(null) }
        var pairingCode by remember { mutableStateOf("") }
        var generatedCode by remember { mutableStateOf<String?>(null) }
        var status by remember { mutableStateOf("") }

        when (role) {
            null -> RoleScreen(
                onParent = {
                    auth.signInAnonymously().addOnSuccessListener { result ->
                        val uid = result.user?.uid ?: return@addOnSuccessListener
                        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                            db.collection("users").document(uid).set(
                                mapOf("role" to "parent", "fcmToken" to token, "createdAt" to FieldValue.serverTimestamp()),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                        }
                        role = "parent"
                    }
                },
                onChild = { role = "child" }
            )
            "parent" -> ParentDashboard(onPairAnother = { role = "parentSetup" })
            "parentSetup" -> ParentPairingScreen(
                generatedCode = generatedCode,
                status = status,
                onGenerate = {
                    auth.signInAnonymously().addOnSuccessListener { result ->
                        val parentUid = result.user?.uid ?: return@addOnSuccessListener
                        val code = Random.nextInt(100000, 1000000).toString()
                        val expires = System.currentTimeMillis() + 10 * 60 * 1000L
                        db.collection("pairingCodes").document(code).set(
                            mapOf("parentUid" to parentUid, "expiresAtEpochMs" to expires, "used" to false)
                        ).addOnSuccessListener {
                            generatedCode = code
                            status = "Code valid for 10 minutes."
                        }.addOnFailureListener { status = "Could not generate code." }
                    }.addOnFailureListener { status = "Firebase sign-in failed." }
                },
                onBack = { role = null }
            )
            "child" -> ChildPairingScreen(
                code = pairingCode,
                onCodeChange = { pairingCode = it.filter(Char::isDigit).take(6) },
                status = status,
                onPair = {
                    if (pairingCode.length != 6) {
                        status = "Enter the 6-digit code."
                        return@ChildPairingScreen
                    }
                    auth.signInAnonymously().addOnSuccessListener { result ->
                        val childUid = result.user?.uid ?: return@addOnSuccessListener
                        db.collection("pairingCodes").document(pairingCode).get()
                            .addOnSuccessListener { doc ->
                                val parentUid = doc.getString("parentUid")
                                val expires = doc.getLong("expiresAtEpochMs") ?: 0L
                                val used = doc.getBoolean("used") ?: true
                                if (parentUid.isNullOrBlank() || used || System.currentTimeMillis() > expires) {
                                    status = "Code invalid or expired."
                                    return@addOnSuccessListener
                                }
                                val deviceId = UUID.randomUUID().toString()
                                val batch = db.batch()
                                batch.set(
                                    db.collection("users").document(childUid),
                                    mapOf("role" to "child", "parentUid" to parentUid, "createdAt" to FieldValue.serverTimestamp())
                                )
                                batch.set(
                                    db.collection("families").document(parentUid)
                                        .collection("children").document(childUid),
                                    mapOf(
                                        "childUid" to childUid,
                                        "deviceId" to deviceId,
                                        "pairedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                                batch.update(
                                    db.collection("pairingCodes").document(pairingCode),
                                    mapOf("used" to true)
                                )
                                batch.commit().addOnSuccessListener {
                                    status = "Pairing complete."
                                        LocationTracking.captureOnce(this@MainActivity)
                                        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                                        val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                                        db.collection("users").document(childUid).set(
                                            mapOf("batteryPercent" to battery, "lastSeenEpochMs" to System.currentTimeMillis()),
                                            com.google.firebase.firestore.SetOptions.merge()
                                        )
                                }.addOnFailureListener {
                                    status = "Pairing failed."
                                }
                            }
                    }.addOnFailureListener {
                        status = "Firebase sign-in failed."
                    }
                },
                onBack = { role = null }
            )
        }
    }

    @Composable
    private fun RoleScreen(onParent: () -> Unit, onChild: () -> Unit) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ParentGuard", style = MaterialTheme.typography.headlineMedium)
            Text("Choose device role", modifier = Modifier.padding(top = 8.dp))
            Button(onClick = onParent, modifier = Modifier.padding(top = 24.dp).fillMaxWidth()) {
                Text("I am Parent")
            }
            OutlinedButton(onClick = onChild, modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                Text("I am Child")
            }
        }
    }

    @Composable
    private fun ParentPairingScreen(
        generatedCode: String?,
        status: String,
        onGenerate: () -> Unit,
        onBack: () -> Unit
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Parent setup", style = MaterialTheme.typography.headlineSmall)
            Text("Generate a temporary pairing code for the child's device.")
            Button(onClick = onGenerate, modifier = Modifier.padding(top = 24.dp)) {
                Text("Generate 6-digit code")
            }
            if (generatedCode != null) {
                Text(generatedCode, style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(top = 20.dp))
            }
            if (status.isNotBlank()) Text(status, modifier = Modifier.padding(top = 12.dp))
            TextButton(onClick = onBack) { Text("Back") }
        }
    }

    @Composable
    private fun ChildPairingScreen(
        code: String,
        onCodeChange: (String) -> Unit,
        status: String,
        onPair: () -> Unit,
        onBack: () -> Unit,
        onCamera: () -> Unit
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Child setup", style = MaterialTheme.typography.headlineSmall)
            Text("Enter the code shown on the parent device.")
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text("6-digit code") },
                singleLine = true,
                modifier = Modifier.padding(top = 20.dp)
            )
            Button(onClick = onPair, modifier = Modifier.padding(top = 16.dp)) {
                Text("Pair device")
            }
            OutlinedButton(onClick = onCamera, modifier = Modifier.padding(top = 12.dp).fillMaxWidth()) {
                Text("Take camera photo")
            }
            if (status.isNotBlank()) Text(status, modifier = Modifier.padding(top = 12.dp))
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}
