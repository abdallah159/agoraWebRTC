package com.hamza.lsagorakotlin

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.ss.ScreenShare
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration


class BroadcasterActivity : Activity() {
    private val LOG_TAG = BroadcasterActivity::class.java.simpleName

    private var mRtcEngine: RtcEngine? = null
    private var mFlCam: FrameLayout? = null
    private var mFlSS: FrameLayout? = null
    private var mSS = false
    private var mVEC: VideoEncoderConfiguration? = null
    private var mSSInstance: ScreenShare? = null

    private val mListener = object : ScreenShare.IStateListener {
        override fun onError(error: Int) {
            Log.e(LOG_TAG, "Screen share service error happened: $error")
        }

        override fun onTokenWillExpire() {
            Log.d(LOG_TAG, "Screen share service token will expire")
            mSSInstance!!.renewToken(null) //Replace the token with your valid token
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(LOG_TAG, "onUserOffline: $uid reason: $reason")
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(LOG_TAG, "onJoinChannelSuccess: $channel $elapsed")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(LOG_TAG, "onUserJoined: " + (uid and 0xFFFFFF))
            runOnUiThread(Runnable {
                if (uid == Constant.SCREEN_SHARE_UID) {
                    setupRemoteView(uid)
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcaster)

        mFlCam = findViewById(R.id.camera_preview) as FrameLayout
        mFlSS = findViewById(R.id.screen_share_preview) as FrameLayout

        mSSInstance = ScreenShare.getInstance()
        mSSInstance!!.setListener(mListener)

        initAgoraEngineAndJoinChannel()
    }

    private fun initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine()
        setupVideoProfile()
        setupLocalVideo()
        joinChannel()
    }

    override fun onDestroy() {
        super.onDestroy()

        leaveChannel()
        RtcEngine.destroy()
        mRtcEngine = null
        if (mSS) {
            mSSInstance!!.stop(getApplicationContext())
        }
    }

    fun onCameraSharingClicked(view: View) {
        val button = view as Button
        if (button.isSelected) {
            button.isSelected = false
            button.setText(getResources().getString(R.string.label_start_camera))
        } else {
            button.isSelected = true
            button.setText(getResources().getString(R.string.label_stop_camera))
        }

        mRtcEngine!!.enableLocalVideo(button.isSelected)
    }

    fun onScreenSharingClicked(view: View) {
        val button = view as Button
        val selected = button.isSelected
        button.isSelected = !selected

        if (button.isSelected) {
            mSSInstance!!.start(
                getApplicationContext(), getResources().getString(R.string.agora_app_id), null,
                getResources().getString(R.string.label_channel_name), Constant.SCREEN_SHARE_UID, mVEC
            )
            button.setText(getResources().getString(R.string.label_stop_sharing_your_screen))
            mSS = true
        } else {
            mSSInstance!!.stop(getApplicationContext())
            button.setText(getResources().getString(R.string.label_start_sharing_your_screen))
            mSS = false
        }
    }

    private fun initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getApplicationContext(), getString(R.string.agora_app_id), mRtcEventHandler)
        } catch (e: Exception) {
            Log.e(LOG_TAG, Log.getStackTraceString(e))

            throw RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e))
        }

    }

    private fun setupVideoProfile() {
        mRtcEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        mRtcEngine!!.enableVideo()
        mVEC = VideoEncoderConfiguration(
            VideoEncoderConfiguration.VD_640x360,
            VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
            VideoEncoderConfiguration.STANDARD_BITRATE,
            VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        )
        mRtcEngine!!.setVideoEncoderConfiguration(mVEC)
        mRtcEngine!!.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
    }

    private fun setupLocalVideo() {
        val camV = RtcEngine.CreateRendererView(getApplicationContext())
        camV.setZOrderOnTop(true)
        camV.setZOrderMediaOverlay(true)
        mFlCam!!.addView(
            camV,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        mRtcEngine!!.setupLocalVideo(VideoCanvas(camV, VideoCanvas.RENDER_MODE_FIT, Constant.CAMERA_UID))
        mRtcEngine!!.enableLocalVideo(false)
    }

    private fun setupRemoteView(uid: Int) {
        val ssV = RtcEngine.CreateRendererView(getApplicationContext())
        ssV.setZOrderOnTop(true)
        ssV.setZOrderMediaOverlay(true)
        mFlSS!!.addView(
            ssV,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        mRtcEngine?.setupRemoteVideo(VideoCanvas(ssV, VideoCanvas.RENDER_MODE_FIT, uid))
    }


    private fun joinChannel() {
        mRtcEngine!!.joinChannel(
            null,
            getResources().getString(R.string.label_channel_name),
            "Extra Optional Data",
            Constant.CAMERA_UID
        )
        // if you do not specify the uid, we will generate the uid for you
    }

    private fun leaveChannel() {
        mRtcEngine!!.leaveChannel()
    }
}
