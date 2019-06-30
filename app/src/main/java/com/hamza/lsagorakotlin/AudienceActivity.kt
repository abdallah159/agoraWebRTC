package com.hamza.lsagorakotlin

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas

class AudienceActivity : Activity() {

    private val TAG = AudienceActivity::class.java.simpleName

    private var mFlCam: FrameLayout? = null
    private var mFlSS: FrameLayout? = null
    private var mRtcEngine: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audience)

        mFlCam = findViewById<View>(R.id.fl_camera) as FrameLayout
        mFlSS = findViewById<View>(R.id.fl_screenshare) as FrameLayout

        initEngineAndJoin()
    }

    private fun initEngineAndJoin() {
        try {
            mRtcEngine = RtcEngine.create(
                getApplicationContext(),
                getString(R.string.agora_app_id),
                object : IRtcEngineEventHandler() {

                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        Log.d(TAG, "onJoinChannelSuccess: " + (uid and 0xFFFFFF))
                    }

                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        Log.d(TAG, "onUserJoined: " + (uid and 0xFFFFFF))
                        runOnUiThread(Runnable { setupRemoteView(uid) })
                    }

                })
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        mRtcEngine?.enableVideo()
        mRtcEngine?.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)

        mRtcEngine?.joinChannel(null, getResources().getString(R.string.label_channel_name), "", Constant.AUDIENCE_UID)

        mRtcEngine?.enableLocalAudio(true)
        mRtcEngine?.enableAudio()
    }

    private fun setupRemoteView(uid: Int) {
        val surfaceV = RtcEngine.CreateRendererView(getApplicationContext())
        surfaceV.setZOrderOnTop(true)
        surfaceV.setZOrderMediaOverlay(true)

        if (uid == Constant.CAMERA_UID) {
            mFlCam?.addView(
                surfaceV,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        } else if (uid == Constant.SCREEN_SHARE_UID) {
            mFlSS?.addView(
                surfaceV,
                FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        } else {
            Log.e(TAG, "unknown uid")
        }

        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    protected override fun onDestroy() {
        super.onDestroy()

        mRtcEngine?.leaveChannel()

        RtcEngine.destroy()
        mRtcEngine = null
    }

}
