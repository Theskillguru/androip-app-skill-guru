package com.example.theskillguru

import com.example.theskillguru.media.RtcTokenBuilder2
import com.example.theskillguru.databinding.ActivityVideoCallBinding
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException

class VideoCallActivity : AppCompatActivity() {

    private val appId = "118547f390694ee394b766d61103db30"
    var appCertificate = "430a24e1e66b4ab18b0b894c8094e4d7"
    var expirationTimeInSeconds = 3600
    private var channelName = "TheSkillGuruFoundation"
    private var token: String? = "007eJxTYMhNyDX5974n1u3lFd+Y2pDt6zLebJWZubenge9NoMeRueYKDIaGFqYm5mnGlgZmliapqcaWJknmZmYpZoaGBsYpScYG81UfpjUEMjLE+91gZGSAQBBfjCEkIzU4OzMnx720qNQtvzQvJbEkMz+PgQEAGC8nGA=="
    private val uid = 0
    private var isJoined = false
    private var agoraEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var isAudioMuted = false
    private var isVideoEnabled = true
    private lateinit var agoraWhiteboardAPI: AgoraWhiteboardAPI


    private lateinit var whiteboardView: WhiteboardView
    private var room: Room? = null
    private var whiteSdk: WhiteSdk? = null

    private var currentTool: String = "pencil"
    private var currentColor: Int = Color.BLACK
    private var currentStrokeWidth: Float = 4f

    private var callRequestId: String? = null
    private var isIncomingCall = false

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private lateinit var binding: ActivityVideoCallBinding

    private lateinit var db: FirebaseFirestore

    private var guruId: String? = null
    private var guruName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        agoraWhiteboardAPI = AgoraWhiteboardAPI()


        // Get call information from intent
        guruId = intent.getStringExtra("guruId")
        guruName = intent.getStringExtra("guruName")
        callRequestId = intent.getStringExtra("callRequestId")
        isIncomingCall = callRequestId != null

        listenForCallStatusChanges()

        // Update channel name with guruId for a unique channel
        channelName = "TheSkillGuruFoundation_${guruId}"

        setupTokenAndPermissions()
        setupVideoSDKEngine()
        initializeWhiteboard()
        setupUIControls()

        // Update UI with call information
        binding.callerNameTextView.text = if (isIncomingCall) "Video Call with $guruName" else "Outgoing Video Call"

        // Check permissions and join channel
        if (checkSelfPermission()) {
            joinChannel()
        }
    }


    private fun setupTokenAndPermissions() {
        val tokenBuilder = RtcTokenBuilder2()
        val timestamp = (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
        token = tokenBuilder.buildTokenWithUid(
            appId, appCertificate,
            channelName, uid, RtcTokenBuilder2.Role.ROLE_PUBLISHER, timestamp, timestamp
        )

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }
    }

    private fun setupUIControls() {
        binding.btnToggleAudio.setOnClickListener { toggleAudio() }
        binding.btnToggleVideo.setOnClickListener { toggleVideo() }
        binding.btnToggleWhiteboard.setOnClickListener { toggleWhiteboard() }

        binding.btnPen.setOnClickListener { setWhiteboardTool("pencil") }
        binding.btnEraser.setOnClickListener { setWhiteboardTool("eraser") }
        binding.btnText.setOnClickListener { setWhiteboardTool("text") }
        binding.btnRectangle.setOnClickListener { setWhiteboardTool("rectangle") }
        binding.btnEllipse.setOnClickListener { setWhiteboardTool("ellipse") }

        // Fix for issue 1 and 2: Replace setOnColorChangeListener with a proper color picker library method
        // For example, if using the ColorPicker library:
        binding.colorPicker.setOnColorChangedListener { color ->
            currentColor = color
            updateWhiteboardColor()
        }

        binding.strokeWidthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentStrokeWidth = progress.toFloat()
                updateWhiteboardStrokeWidth()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnClearWhiteboard.setOnClickListener { clearWhiteboard() }
    }
    // Existing video calling related functions

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }

    fun joinChannel(view: View) {
        if (checkSelfPermission()) {
            val options = ChannelMediaOptions()
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView!!.visibility = View.VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, options)
        } else {
            Toast.makeText(applicationContext, "Permissions were not granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun leaveChannel(view: View) {
        if (!isJoined) {
            //showMessage("Join a channel first")
            finish()
        } else {
            agoraEngine!!.leaveChannel()
            showMessage("You left the channel")
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
            isJoined = false
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        binding.remoteVideoViewContainer.addView(remoteSurfaceView)
        agoraEngine!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                uid
            )
        )
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)
        binding.localVideoViewContainer.addView(localSurfaceView)
        agoraEngine!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    private fun toggleAudio() {
        isAudioMuted = !isAudioMuted
        agoraEngine?.muteLocalAudioStream(isAudioMuted)
        binding.btnToggleAudio.setImageResource(
            if (isAudioMuted) R.drawable.ic_mute else R.drawable.ic_unmute
        )
    }

    private fun toggleVideo() {
        isVideoEnabled = !isVideoEnabled
        agoraEngine?.enableLocalVideo(isVideoEnabled)
        binding.btnToggleVideo.setImageResource(
            if (isVideoEnabled) R.drawable.ic_video_on else R.drawable.ic_video_off
        )
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            showMessage("Remote user joined $uid")
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            isJoined = true
            showMessage("Joined Channel $channel")
            localSurfaceView?.visibility = View.VISIBLE
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            showMessage("Remote user offline $uid $reason")
            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }
        }
    }

    private fun checkSelfPermission(): Boolean {
        return !(ContextCompat.checkSelfPermission(
            this,
            REQUESTED_PERMISSIONS[0]
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    REQUESTED_PERMISSIONS[1]
                ) != PackageManager.PERMISSION_GRANTED)
    }

    private fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // New whiteboard related functions

    private fun initializeWhiteboard() {
        whiteboardView = binding.whiteboardView
        val whiteSDKConfiguration = WhiteSdkConfiguration(appId, true)
        whiteSdk = WhiteSdk(whiteboardView, this, whiteSDKConfiguration)

        // Use coroutine to make the API call asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (roomUUID, roomToken) = agoraWhiteboardAPI.createRoom()
                    ?: throw Exception("Failed to create room")

                withContext(Dispatchers.Main) {
                    val roomParams = RoomParams(roomUUID, roomToken, uid.toString())
                    joinWhiteboardRoom(roomParams)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VideoCallActivity, "Failed to initialize whiteboard: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun joinWhiteboardRoom(roomParams: RoomParams) {
        whiteSdk?.joinRoom(roomParams, object : RoomListener {
            override fun onPhaseChanged(phase: RoomPhase) {
                if (phase == RoomPhase.connected) {
                    runOnUiThread {
                        Toast.makeText(this@VideoCallActivity, "Connected to Whiteboard", Toast.LENGTH_SHORT).show()
                        setWhiteboardTool(currentTool)
                    }
                }
            }
            override fun onRoomStateChanged(modifyState: RoomState) {}
            override fun onDisconnectWithError(error: Exception?) {}
            override fun onKickedWithReason(reason: String?) {}
            override fun onCanUndoStepsUpdate(canUndoSteps: Long) {}
            override fun onCanRedoStepsUpdate(canRedoSteps: Long) {}
            override fun onCatchErrorWhenAppendFrame(userId: Long, error: Exception?) {}
        }, object : Promise<Room> {
        override fun then(room: Room) {
            this@VideoCallActivity.room = room
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Joined Whiteboard Room", Toast.LENGTH_SHORT).show()
                setWhiteboardTool(currentTool)
            }
        }

        override fun catchEx(t: SDKError) {
            runOnUiThread {
                Toast.makeText(this@VideoCallActivity, "Failed to join Whiteboard: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    })
}

    private fun setWhiteboardTool(toolName: String) {
        currentTool = toolName
        room?.let { room ->
            val memberState = MemberState()
            memberState.currentApplianceName = when (toolName) {
                "pencil" -> Appliance.PENCIL
                "eraser" -> Appliance.ERASER
                "text" -> Appliance.TEXT
                "rectangle" -> Appliance.RECTANGLE
                "ellipse" -> Appliance.ELLIPSE
                else -> Appliance.PENCIL
            }
            room.setMemberState(memberState)
            runOnUiThread {
                updateToolButtonStates(toolName)
            }
        } ?: run {
            Toast.makeText(this, "Whiteboard not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateToolButtonStates(selectedTool: String) {
        binding.btnPen.isSelected = selectedTool == "pencil"
        binding.btnEraser.isSelected = selectedTool == "eraser"
        binding.btnText.isSelected = selectedTool == "text"
        binding.btnRectangle.isSelected = selectedTool == "rectangle"
        binding.btnEllipse.isSelected = selectedTool == "ellipse"
    }

    private fun updateWhiteboardColor() {
        room?.let { room ->
            val memberState = MemberState()
            memberState.strokeColor = intArrayOf(
                (Color.red(currentColor)),
                (Color.green(currentColor)),
                (Color.blue(currentColor))
            )
            room.setMemberState(memberState)
        }
    }

    private fun updateWhiteboardStrokeWidth() {
        room?.let { room ->
            val memberState = MemberState()
            // Fix for issue 4: Cast Float to Double
            memberState.strokeWidth = currentStrokeWidth.toDouble()
            room.setMemberState(memberState)
        }
    }
    private fun clearWhiteboard() {
        room?.let { room ->
            room.cleanScene(true)
            Toast.makeText(this, "Whiteboard cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleWhiteboard() {
        if (binding.whiteboardView.visibility == View.VISIBLE) {
            binding.whiteboardView.visibility = View.GONE
            binding.remoteVideoViewContainer.visibility = View.VISIBLE
            binding.whiteboardControls.visibility = View.GONE
        } else {
            binding.whiteboardView.visibility = View.VISIBLE
            binding.remoteVideoViewContainer.visibility = View.GONE
            binding.whiteboardControls.visibility = View.VISIBLE
        }
    }

    private fun joinChannel() {
        if (checkSelfPermission()) {
            val options = ChannelMediaOptions()
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView!!.visibility = View.VISIBLE
            agoraEngine!!.startPreview()
            agoraEngine!!.joinChannel(token, channelName, uid, options)

            if (isIncomingCall) {
                // Update call request status to "connected" in Firestore
                callRequestId?.let { id ->
                    db.collection("callRequests").document(id)
                        .update("status", "connected")
                        .addOnFailureListener { e ->
                            Log.e("VideoCallActivity", "Error updating call status", e)
                        }
                }
            }
        } else {
            Toast.makeText(applicationContext, "Permissions were not granted", Toast.LENGTH_SHORT).show()
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
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()
        room?.disconnect()

        // Update call request status to "ended" in Firestore
        callRequestId?.let { id ->
            db.collection("callRequests").document(id)
                .update("status", "ended")
                .addOnFailureListener { e ->
                    Log.e("VideoCallActivity", "Error updating call status", e)
                }
        }

        fun listenForCallStatusChanges() {
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

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }
}

class AgoraWhiteboardAPI {
    private val client = OkHttpClient()
    private val baseUrl = "https://api.netless.link/v5"
    private val sdkToken = "YOUR_SDK_TOKEN"  // Replace with your actual SDK token

    suspend fun createRoom(): Pair<String, String>? = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("isRecord", false)
        }

        val mediaType = "application/json".toMediaType()  // Use extension function

        val request = Request.Builder()
            .url("$baseUrl/rooms")
            .addHeader("token", sdkToken)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(mediaType, requestBody.toString()))  // Use updated mediaType
            .build()


        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            // Get the response body safely
            val responseBody = response.body

            // Safely handle null response body
            if (responseBody != null) {
                val jsonResponse = JSONObject(responseBody.string())
                val uuid = jsonResponse.getString("uuid")

                return@withContext Pair(uuid, generateRoomToken(uuid))
            } else {
                // Handle the case where response body is null
                throw IOException("Response body is null")
            }
        } else {
            // Handle unsuccessful response
            throw IOException("Unexpected response code: ${response.code}")
        }



        null
    }

    private suspend fun generateRoomToken(roomUUID: String): String = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("lifespan", 3600)  // Token valid for 1 hour
            put("role", "admin")
        }

        val mediaType = "application/json".toMediaType()  // Use the extension function

        val request = Request.Builder()
            .url("$baseUrl/tokens/rooms/$roomUUID")
            .addHeader("token", sdkToken)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(mediaType, requestBody.toString()))  // Use the updated mediaType
            .build()


        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            // Safely get the response body
            val responseBody = response.body

            // Safely handle null response body
            if (responseBody != null) {
                val jsonResponse = JSONObject(responseBody.string())
                return@withContext jsonResponse.getString("value")
            } else {
                // Handle the case where the response body is null
                throw IOException("Response body is null")
            }
        } else {
            // Handle unsuccessful response
            throw IOException("Unexpected response code: ${response.code}")
            throw Exception("Failed to generate room token")
        }



    }
}