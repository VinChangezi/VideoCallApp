package com.changezi.videocallapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas


class HomeActivity : AppCompatActivity() {

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    private fun checkSelfPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                REQUESTED_PERMISSIONS[0]
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                REQUESTED_PERMISSIONS[1]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else true
    }

    fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val uid = 5

    private var isJoined = false

    private var agoraEngin: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = resources.getString(com.changezi.videocallapp.R.string.appId)
            config.mEventHandler = mRtcHandler
            agoraEngin = RtcEngine.create(config)
            agoraEngin?.enableVideo()
        } catch (e: Exception) {
            showMessage(e.toString())
        }
    }

    private val mRtcHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            showMessage("Remote user joined $uid")
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            isJoined = true
            showMessage("Joined Channel $channel")
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            showMessage("Remote user offline $uid $reason")
            runOnUiThread { remoteSurfaceView!!.visibility = View.GONE }
        }
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = findViewById<FrameLayout>(com.changezi.videocallapp.R.id.remoteVideo)
        remoteSurfaceView = SurfaceView(baseContext)
        remoteSurfaceView!!.setZOrderMediaOverlay(true)
        container.addView(remoteSurfaceView)
        agoraEngin!!.setupRemoteVideo(
            VideoCanvas(
                remoteSurfaceView,
                VideoCanvas.RENDER_MODE_FIT,
                uid
            )
        )
        remoteSurfaceView!!.visibility = View.VISIBLE
    }

    private fun setupLocalVideo() {
        val container = findViewById<FrameLayout>(com.changezi.videocallapp.R.id.localVideo)
        localSurfaceView = SurfaceView(baseContext)
        container.addView(localSurfaceView)
        agoraEngin!!.setupLocalVideo(
            VideoCanvas(
                localSurfaceView,
                VideoCanvas.RENDER_MODE_HIDDEN,
                0
            )
        )
    }

    fun joinChannel(view: View?) {
        if (checkSelfPermission()) {
            val options = ChannelMediaOptions()
            options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            setupLocalVideo()
            localSurfaceView!!.visibility = View.VISIBLE
            agoraEngin!!.startPreview()
            agoraEngin!!.joinChannel(
                resources.getString(com.changezi.videocallapp.R.string.token),
                resources.getString(com.changezi.videocallapp.R.string.channel),
                uid,
                options
            )
        } else {
            showMessage("Permission was not granted")
        }
    }

    fun leaveChannel(view: View?) {
        if (!isJoined) {
            showMessage("Join a channel first")
        } else {
            agoraEngin!!.leaveChannel()
            showMessage("You left channel")
            if (remoteSurfaceView != null) remoteSurfaceView!!.visibility = View.GONE
            if (localSurfaceView != null) localSurfaceView!!.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngin!!.stopPreview()
        agoraEngin!!.leaveChannel()
        Thread {
            RtcEngine.destroy()
            agoraEngin = null
        }.start()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.changezi.videocallapp.R.layout.activity_home)

        /*AgoraRTC.instance().bootstrap(this, "appID", "channel");
        setContentView(R.layout.group_template);*/
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }
        setupVideoSDKEngine()
    }
}