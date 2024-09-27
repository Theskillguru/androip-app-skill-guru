package com.example.theskillguru

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import io.agora.rtc2.*

class AudioCallActivity : AppCompatActivity() {

    private lateinit var rtcEngine: RtcEngine
    private val appId = "118547f390694ee394b766d61103db30"
    private var channelName = "TheSkillGuruFoundation"
    private val token = "007eJxTYMhNyDX5974n1u3lFd+Y2pDt6zLebJWZubenge9NoMeRueYKDIaGFqYm5mnGlgZmliapqcaWJknmZmYpZoaGBsYpScYG81UfpjUEMjLE+91gZGSAQBBfjCEkIzU4OzMnx720qNQtvzQvJbEkMz+PgQEAGC8nGA==" // Use null for testing. In production, you should use a token for security.

    private lateinit var callerNameTextView: TextView
    private lateinit var callStatusTextView: TextView
    private lateinit var speakerButton: ImageButton
    private lateinit var muteButton: ImageButton
    private lateinit var endCallButton: ImageButton

    private var callRequestId: String? = null
    private var isIncomingCall = false

    private var guruId: String? = null
    private var guruName: String? = null

    private lateinit var db: FirebaseFirestore

    private var isSpeakerEnabled = false
    private var isMuted = false

    companion object {
        private const val PERMISSION_REQ_ID = 22
        private val REQUESTED_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_call)

        guruId = intent.getStringExtra("guruId")
        guruName = intent.getStringExtra("guruName")

        initializeUI()

        db = FirebaseFirestore.getInstance()

        listenForCallStatusChanges()

        channelName = "TheSkillGuruFoundation_$guruId"

        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID)) {
            initializeAndJoinChannel()
        }
    }

    private fun initializeUI() {
        callerNameTextView = findViewById(R.id.callerNameTextView)
        callStatusTextView = findViewById(R.id.callStatusTextView)
        speakerButton = findViewById(R.id.speakerButton)
        muteButton = findViewById(R.id.muteButton)
        endCallButton = findViewById(R.id.endCallButton)

        callerNameTextView.text = "Audio Call with $guruName"

        callStatusTextView.text = "Calling..."

        speakerButton.setOnClickListener { toggleSpeaker() }
        muteButton.setOnClickListener { toggleMute() }
        endCallButton.setOnClickListener { endCall() }
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode)
            return false
        }
        return true
    }

    private fun initializeAndJoinChannel() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            rtcEngine = RtcEngine.create(config)
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine.joinChannel(token, channelName, "", 0)

            if (isIncomingCall) {
                // Update call request status to "connected" in Firestore
                callRequestId?.let { id ->
                    db.collection("callRequests").document(id)
                        .update("status", "connected")
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error updating call status: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            val uniqueChannelName = "TheSkillGuruFoundation_$guruId"

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread { callStatusTextView.text = "Connected" }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                callStatusTextView.text = "User Offline"
                endCall()
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread { callStatusTextView.text = "Connected" }
        }
    }



    private fun toggleSpeaker() {
        isSpeakerEnabled = !isSpeakerEnabled
        rtcEngine.setEnableSpeakerphone(isSpeakerEnabled)
        speakerButton.setImageResource(if (isSpeakerEnabled) R.drawable.ic_speaker_on else R.drawable.ic_speaker)
    }

    private fun toggleMute() {
        isMuted = !isMuted
        rtcEngine.muteLocalAudioStream(isMuted)
        muteButton.setImageResource(if (isMuted) R.drawable.ic_mute_on else R.drawable.ic_mute)
    }

    private fun endCall() {
        rtcEngine.leaveChannel()
        callRequestId?.let { id ->
            db.collection("callRequests").document(id)
                .update("status", "ended")
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating call status: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        finish()    }

    fun addGuru(name: String, expertise: String) {
        val guru = hashMapOf(
            "name" to name,
            "available" to false,  // Default to unavailable
            "expertise" to expertise
        )

        db.collection("gurus")
            .add(guru)
            .addOnSuccessListener { documentReference ->
                val guruId = documentReference.id
                // Update the document with its own ID
                documentReference.update("id", guruId)
            }
            .addOnFailureListener { e ->
                // Handle any errors
            }
    }

    private fun listenForCallStatusChanges() {
        callRequestId?.let { id ->
            db.collection("callRequests").document(id)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("VideoCallActivity", "Listen failed.", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        when (snapshot.getString("status")) {
                            "ended" -> {
                                Toast.makeText(this, "Call ended by the other party", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        RtcEngine.destroy()
    }
}