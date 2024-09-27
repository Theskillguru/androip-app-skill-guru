package com.example.theskillguru

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class IncomingCallActivity : AppCompatActivity() {
    private lateinit var tvCallerName: TextView
    private lateinit var tvCallType: TextView
    private lateinit var btnAccept: Button
    private lateinit var btnDecline: Button

    private var callRequestId: String = ""
    private var type: String = ""
    private var callerName: String = ""

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        Log.d(TAG, "IncomingCallActivity created")
        Log.d(TAG, "Intent extras: ${intent.extras}")

        initializeViews()
        handleIncomingIntent(intent)
        setupButtonListeners()
    }

    private fun initializeViews() {
        tvCallerName = findViewById(R.id.tvCallerName)
        tvCallType = findViewById(R.id.tvCallType)
        btnAccept = findViewById(R.id.btnAccept)
        btnDecline = findViewById(R.id.btnDecline)
    }

    private fun handleIncomingIntent(intent: Intent) {
        callRequestId = intent.getStringExtra("callRequestId") ?: ""
        type = intent.getStringExtra("callType") ?: ""
        callerName = intent.getStringExtra("callerName") ?: ""

        Log.d(TAG, "Received intent data: callRequestId=$callRequestId, callType=$type, callerName=$callerName")

        if (callRequestId.isNotEmpty() && type.isNotEmpty() && callerName.isNotEmpty()) {
            displayCallInformation()
        } else {
            Log.w(TAG, "Missing call information, attempting to fetch from Firestore")
            fetchCallInformationFromFirestore()
        }
    }

    private fun displayCallInformation() {
        tvCallerName.text = callerName
        tvCallType.text = "$type Call"
        Log.d(TAG, "Displayed call information: $callerName, $type")
    }

    private fun fetchCallInformationFromFirestore() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e(TAG, "User not logged in")
            showErrorAndFinish("User not logged in")
            return
        }

        db.collection("callRequests")
            .whereEqualTo("guruId", currentUserId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.w(TAG, "No pending call requests found")
                    showErrorAndFinish("No pending call requests found")
                } else {
                    // Sort the documents by timestamp locally
                    val sortedDocuments = documents.sortedByDescending { it.getTimestamp("timestamp") }
                    val document = sortedDocuments.firstOrNull()

                    if (document != null) {
                        callRequestId = document.id
                        type = document.getString("callType") ?: ""
                        callerName = document.getString("callerName") ?: ""

                        Log.d(TAG, "Retrieved call information from Firestore: $callRequestId, $type, $callerName")
                        displayCallInformation()
                    } else {
                        Log.w(TAG, "No valid call request found")
                        showErrorAndFinish("No valid call request found")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching call information: ${e.message}")
                showErrorAndFinish("Failed to fetch call information: ${e.message}")
            }
    }

    private fun setupButtonListeners() {
        btnAccept.setOnClickListener { acceptCall() }
        btnDecline.setOnClickListener { declineCall() }
    }

    private fun acceptCall() {
        if (callRequestId.isEmpty()) {
            Log.e(TAG, "Cannot accept call: callRequestId is empty")
            showErrorAndFinish("Cannot accept call: missing information")
            return
        }

        db.collection("callRequests").document(callRequestId)
            .update("status", "accepted")
            .addOnSuccessListener {
                Log.d(TAG, "Call accepted successfully")
                startAppropriateCallActivity()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to accept call: ${e.message}")
                Toast.makeText(this, "Failed to accept call: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startAppropriateCallActivity() {
        val intent = when (type.lowercase(Locale.getDefault())) {
            "video" -> Intent(this, VideoCallActivity::class.java)
            "audio" -> Intent(this, AudioCallActivity::class.java)
            else -> {
                Log.e(TAG, "Invalid call type: $type")
                showErrorAndFinish("Invalid call type")
                return
            }
        }
        intent.putExtra("callRequestId", callRequestId)
        intent.putExtra("callerName", callerName)
        intent.putExtra("guruId", FirebaseAuth.getInstance().currentUser?.uid)
        intent.putExtra("guruName", FirebaseAuth.getInstance().currentUser?.displayName)
        startActivity(intent)
        finish()
    }

    private fun declineCall() {
        if (callRequestId.isEmpty()) {
            Log.e(TAG, "Cannot decline call: callRequestId is empty")
            showErrorAndFinish("Cannot decline call: missing information")
            return
        }

        db.collection("callRequests").document(callRequestId)
            .update("status", "rejected")
            .addOnSuccessListener {
                Log.d(TAG, "Call declined successfully")
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to decline call: ${e.message}")
                Toast.makeText(this, "Failed to decline call: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showErrorAndFinish(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Error: $errorMessage")
        finish()
    }

    private fun showIndexBuildingDialog() {
        AlertDialog.Builder(this)
            .setTitle("Please Wait")
            .setMessage("The app is updating. This may take a few minutes. Please try again shortly.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val TAG = "IncomingCallActivity"
    }
}