package com.example.theskillguru

import FCMMessage
import FCMResponse
import Message
import Notification
import RetrofitClient
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class QuestionActivity : AppCompatActivity() {
    private lateinit var guruSpinner: Spinner
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedGuru: Guru? = null
    private lateinit var availabilitySwitch: SwitchCompat
    private lateinit var callButton: ImageButton

    private lateinit var functions: FirebaseFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_question)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        // Initialize views
        callButton = findViewById(R.id.imageView)
        guruSpinner = findViewById(R.id.guruSpinner)
        availabilitySwitch = findViewById(R.id.availabilitySwitch)

        functions = FirebaseFunctions.getInstance()

        refreshFCMToken()

        // Check user profile and set up UI accordingly
        checkUserProfile()



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionState == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }




    }

    private fun checkUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        when (document.getString("Profile")) {
                            "Guru" -> setupGuruUI(document)
                            "Learner" -> setupLearnerUI()
                            else ->setupBothUI(document)
                        }
                    } else {
                        // Handle case where user document doesn't exist
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to fetch user profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Handle case where user is not logged in
            navigateToLogin()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBothUI(document: DocumentSnapshot) {

        guruSpinner.visibility = View.VISIBLE
        callButton.visibility = View.VISIBLE

        // Hide availability switch
        availabilitySwitch.visibility = View.VISIBLE
        availabilitySwitch.isChecked = document.getBoolean("toggleOnline") ?: false
        setupAvailabilityToggle(auth.currentUser!!.uid)


        // Fetch and populate guru list
        guruSpinner.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Fetch available gurus when the spinner is clicked
                fetchAvailableGurus()
            }
            false // Return false to indicate that the touch event was not consumed
        }
        // Set up call button listener
        setupCallButton()

    }

    private fun refreshFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                FirebaseFirestore.getInstance().collection("Users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token refreshed and updated in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error updating FCM token in Firestore", e)
                    }
            }
        }
    }

    private fun setupGuruUI(document: DocumentSnapshot) {
        // Hide guru spinner and call button
        guruSpinner.visibility = View.GONE
        callButton.visibility = View.GONE

        // Show and set up availability switch
        availabilitySwitch.visibility = View.VISIBLE
        availabilitySwitch.isChecked = document.getBoolean("toggleOnline") ?: false
        setupAvailabilityToggle(auth.currentUser!!.uid)

        // Add any other UI elements specific to gurus
    }

    private fun setupLearnerUI() {
        // Show guru spinner and call button
        guruSpinner.visibility = View.VISIBLE
        callButton.visibility = View.VISIBLE

        // Hide availability switch
        availabilitySwitch.visibility = View.GONE

        // Fetch and populate guru list
        fetchAvailableGurus()

        // Set up call button listener
        setupCallButton()
    }

    private fun setupCallButton() {
        callButton.setOnClickListener {
            if (selectedGuru != null) {
                showCallOptionsDialog()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("No Guru Selected")
                    .setMessage("Please select a guru before initiating a call.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }


    private fun initiateCall(type: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to make a call.", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch the latest FCM token for the selected guru
        selectedGuru?.let { guru ->
            FirebaseFirestore.getInstance().collection("Users").document(guru.id)
                .get()
                .addOnSuccessListener { document ->
                    val guruFCMToken = document.getString("fcmToken")
                    if (guruFCMToken != null) {
                        createCallRequest(currentUser, guru, type, guruFCMToken)
                    } else {
                        Toast.makeText(this, "Unable to reach the guru at this time.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to initiate call: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun createCallRequest(currentUser: FirebaseUser, guru: Guru, type: String, guruFCMToken: String) {
        val callRequest = hashMapOf(
            "callerId" to currentUser.uid,
            "callerName" to currentUser.displayName,
            "guruId" to guru.id,
            "guruFCMToken" to guruFCMToken,
            "callType" to type,
            "status" to "pending"
        )
        db.collection("callRequests")
            .add(callRequest)
            .addOnSuccessListener { documentReference ->
                val callRequestId = documentReference.id
                sendPushNotificationToGuru(callRequestId, type, guruFCMToken)
                listenForCallAcceptance(callRequestId, type)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to initiate call: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startCall(type: String, callRequestId: String) {
        val intent = when (type) {
            "video" -> Intent(this, VideoCallActivity::class.java)
            "audio" -> Intent(this, AudioCallActivity::class.java)
            else -> throw IllegalArgumentException("Invalid call type")
        }
        intent.putExtra("guruId", selectedGuru?.id)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("guruName", selectedGuru?.name)
        intent.putExtra("callRequestId", callRequestId)
        startActivity(intent)
    }

    private fun listenForCallAcceptance(callRequestId: String, type: String) {
        db.collection("callRequests")
            .document(callRequestId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Failed to listen for call acceptance: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    when (snapshot.getString("status")) {
                        "accepted" -> startCall(type, callRequestId)
                        "rejected" -> showCallRejectedDialog()
                        // "pending" status is handled by the waiting dialog
                    }
                }
            }

        showWaitingForAcceptanceDialog(callRequestId)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun sendPushNotificationToGuru(callRequestId: String, type: String, guruFCMToken: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val accessToken = getAccessToken()
                val fcmMessage = FCMMessage(
                    message = Message(
                        token = guruFCMToken,
                        notification = Notification(
                            title = "Incoming Call",
                            body = "${auth.currentUser?.displayName} is calling you"
                        ),
                        data = mapOf(
                            "callRequestId" to callRequestId,
                            "callerName" to (auth.currentUser?.displayName ?: ""),
                            "callType" to type
                        )
                    )
                )

                RetrofitClient.instance.sendMessage("Bearer $accessToken", fcmMessage)
                    .enqueue(object : Callback<FCMResponse> {
                        override fun onResponse(call: Call<FCMResponse>, response: Response<FCMResponse>) {
                            if (response.isSuccessful) {
                                Log.d(TAG, "Push notification sent successfully")
                            } else {
                                Log.e(TAG, "Error sending push notification: ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<FCMResponse>, t: Throwable) {
                            Log.e(TAG, "Error sending push notification", t)
                        }
                    })
            } catch (e: Exception) {
                Log.e(TAG, "Error sending push notification", e)
            }
        }
    }

    private fun getAccessToken(): String {
        val stream = resources.openRawResource(R.raw.service_account)
        val credentials = GoogleCredentials.fromStream(stream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
        credentials.refresh()
        return credentials.accessToken.tokenValue
    }


    private fun fetchAvailableGurus() {

        db.collection("Users")
            // .whereEqualTo("Profile", "Guru")
            .whereEqualTo("toggleOnline", true)
            .whereIn("Profile", listOf("Guru", "Both"))
            .get()
            .addOnSuccessListener { documents ->

                val gurus = documents.mapNotNull { doc ->
                  Guru(
                        id = doc.id,
                        name = doc.getString("Name") ?: "",
                        mobile = doc.getString("Mobile") ?: "",
                        available = doc.getBoolean("toggleOnline") ?: true
                    )

                }.toMutableList()

                Log.d("QuestionActivity", "Fetched ${gurus.size} gurus")
              val userIndex =  gurus.indexOfFirst { it.id == auth.currentUser!!.uid.toString() }
                if (userIndex != -1){
                    gurus.removeAt(userIndex)
                }
                populateGuruSpinner(gurus as List<Guru>)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch gurus: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun setupAvailabilityToggle(userId: String) {
        availabilitySwitch.setOnCheckedChangeListener { _, isChecked ->
            updateGuruAvailability(userId, isChecked)
        }
    }

    private fun updateGuruAvailability(userId: String, available: Boolean) {
        db.collection("Users").document(userId)
            .update("toggleOnline", available)
            .addOnSuccessListener {
                // Toast.makeText(this, "Availability updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update availability: ${e.message}", Toast.LENGTH_SHORT).show()
                // Revert the switch state if the update failed
                availabilitySwitch.isChecked = !available
            }
    }

    private fun populateGuruSpinner(gurus: List<Guru>) {
        Log.d("QuestionActivity", "Populating spinner with ${gurus.size} gurus")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, gurus.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        guruSpinner.adapter = adapter

        guruSpinner.visibility = View.VISIBLE
        Log.d("QuestionActivity", "Spinner visibility after populating: ${guruSpinner.visibility}")


        guruSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedGuru = gurus[position]
                Log.d("QuestionActivity", "Selected guru: ${selectedGuru?.name}")

            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedGuru = null
                Log.d("QuestionActivity", "No guru selected")

            }
        }
    }

    private fun showCallOptionsDialog() {
        val options = arrayOf("Video Call", "Audio Call")

        AlertDialog.Builder(this)
            .setTitle("Choose Call Type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> initiateCall("video")
                    1 -> initiateCall("audio")
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }




    private fun showWaitingForAcceptanceDialog(callRequestId: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Waiting for Guru")
            .setMessage("Please wait while the guru responds to your call request...")
            .setNegativeButton("Cancel") { _, _ ->
                cancelCallRequest(callRequestId)
            }
            .create()

        dialog.show()
    }

    private fun cancelCallRequest(callRequestId: String) {
        db.collection("callRequests")
            .document(callRequestId)
            .update("status", "cancelled")
    }

    private fun startCall(type: String) {
        val intent = when (type) {
            "video" -> Intent(this, VideoCallActivity::class.java)
            "audio" -> Intent(this, AudioCallActivity::class.java)
            else -> throw IllegalArgumentException("Invalid call type")
        }
        intent.putExtra("guruId", selectedGuru?.id)
        intent.putExtra("guruName", selectedGuru?.name)
        startActivity(intent)
    }

    private fun showCallRejectedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Call Rejected")
            .setMessage("The guru is currently unavailable. Please try again later.")
            .setPositiveButton("OK", null)
            .show()
    }



    override fun onPause() {
        auth.currentUser?.let {
            db.collection("Users").document(it.uid).update("toggleOnline", false).addOnSuccessListener {
                availabilitySwitch.isChecked = false
            }.addOnFailureListener {
                Toast.makeText(this@QuestionActivity,"You are online",Toast.LENGTH_LONG).show()
            }
        }
        super.onPause()
    }

    override fun onDestroy() {
        auth.currentUser?.let {
            db.collection("Users").document(it.uid).update("toggleOnline", false).addOnSuccessListener {
                availabilitySwitch.isChecked = false
            }.addOnFailureListener {
                Toast.makeText(this@QuestionActivity,"You are online",Toast.LENGTH_LONG).show()
            }
        }
        super.onDestroy()
    }

    override fun onResume() {
        fetchAvailableGurus()
        super.onResume()
    }

}

data class Guru(
    val id: String = "",
    val name: String = "",
    val mobile: String = "",
    val available: Boolean = false
)

