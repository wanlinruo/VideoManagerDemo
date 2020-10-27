package com.tuya.smart.videomanagerdemo.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tuya.smart.videomanagerdemo.Config
import com.tuya.smart.videomanagerdemo.Config.Companion.DEFAULT_FRAME_COUNT
import com.tuya.smart.videomanagerdemo.Config.Companion.MAX_FRAME_INTERVAL_MS
import com.tuya.smart.videomanagerdemo.Config.Companion.MSG_UPDATE
import com.tuya.smart.videomanagerdemo.Config.Companion.maxSelection
import com.tuya.smart.videomanagerdemo.R
import com.tuya.smart.videomanagerdemo.getVideoDuration
import com.tuya.smart.videomanagerdemo.helper.ThumbUtil
import com.tuya.smart.videomanagerdemo.log
import com.tuya.smart.videomanagerdemo.player.VideoPlayTimeController
import com.tuya.smart.videomanagerdemo.player.VideoPlayer
import com.tuya.smart.videomanagerdemo.player.VideoPlayerOfExoPlayer
import com.tuya.smart.videomanagerdemo.ui.ClipContainer
import kotlinx.android.synthetic.main.activity_video_clip.*
import java.io.File
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min

class VideoClipActivity : AppCompatActivity(), ClipContainer.Callback {

    companion object {
        const val TAG = "VideoClipActivity"

    }

    private lateinit var videoPathInput: String

    // 播放器实例
    private var videoPlayer: VideoPlayer? = null

    private var videoPlayTimeController: VideoPlayTimeController? = null

    private val thumbUtil: ThumbUtil = ThumbUtil(this)

    private var millsecPerThumbnail = 1000

    private var thumbnailCount = 0

    private var mStartMillSec: Long = 0

    private var mEndMillSec: Long = 0

    private var frozontime = 0L

    private var mediaDuration: Long = 0

    private var secFormat = DecimalFormat("##0.0")

    //Handler
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            updatePlayPosition()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_clip)

        //获取传递参数
        videoPathInput = intent.getStringExtra("video_path").toString()

        //初始化播放器
        initPlayer()

        //设置播放器
        setupPlayer()
    }


    override fun onPause() {
        super.onPause()
        pausePlayer()
        this.mHandler.removeCallbacksAndMessages(null)
    }


    override fun onDestroy() {
        super.onDestroy()
        thumbUtil.release()
        videoPlayTimeController?.stop()
    }


    private fun initPlayer() {
        videoPlayer = VideoPlayerOfExoPlayer(player_view_exo)
    }

    private fun setupPlayer() {

        val file = File(videoPathInput)
        if (!file.exists()) {
            Toast.makeText(this, "请更新videoPlayUrl变量为本地手机的视频文件地址", Toast.LENGTH_LONG).show()
        }


        mediaDuration = getVideoDuration(this, videoPathInput)
        Log.d(TAG, "onProcessCompleted mediaDuration:$mediaDuration")
        mEndMillSec = if (mediaDuration > maxSelection) {
            maxSelection
        } else {
            mediaDuration
        }

        thumbnailCount = if (mediaDuration > maxSelection) {
            millsecPerThumbnail = MAX_FRAME_INTERVAL_MS
            Math.ceil(((mediaDuration * 1f / millsecPerThumbnail).toDouble())).toInt()
        } else {
            millsecPerThumbnail = (mediaDuration / DEFAULT_FRAME_COUNT).toInt()
            DEFAULT_FRAME_COUNT
        }

        clipContainer.initRecyclerList(thumbnailCount)


        if (videoPlayer?.isPlaying() == true) {
            releasePlayer()
//            initPlayer()
        }

        videoPlayer?.setupPlayer(this, videoPathInput)

        videoPlayTimeController = VideoPlayTimeController(videoPlayer!!)
        videoPlayTimeController?.start()

        thumbUtil.getThumb(
            videoPathInput,
            millsecPerThumbnail,
            thumbnailCount,
        ) { bitmap: String, index: Int ->
            this.mHandler.post { clipContainer.addThumbnail(index, bitmap) }
        }

        clipContainer.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                log("onGlobalLayout()  mediaDuration:$mediaDuration,  size:${clipContainer.list.size}")
                clipContainer.updateInfo(mediaDuration, clipContainer.list.size)
                updatePlayPosition()

                clipContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        clipContainer.callback = (this)
    }

    private fun updatePlayPosition() {

        val currentPosition = getPlayerCurrentPosition()
        if (currentPosition > mEndMillSec) {
            seekToPosition(0)
        } else {
            clipContainer.setProgress(currentPosition.toLong(), frozontime)
        }

        this.mHandler.removeMessages(MSG_UPDATE)
        this.mHandler.sendEmptyMessageDelayed(MSG_UPDATE, 20)
    }

    private fun getPlayerCurrentPosition(): Int {
        return videoPlayer?.getPlayerCurrentPosition() ?: 0
    }

    private fun seekToPosition(startMillSec: Long) {
        videoPlayer?.seekToPosition(startMillSec)
    }

    private fun startPlayer() {
        videoPlayer?.startPlayer()
    }

    private fun pausePlayer() {
        videoPlayer?.pausePlayer()
    }

    private fun releasePlayer() {
        videoPlayer?.releasePlayer()
    }

    override fun onSelectionChang(
        totalCount: Int,
        startMillSec: Long,
        endMillSec: Long,
        finished: Boolean
    ) {
        Log.d(TAG, "onSelectionChang ...startMillSec:$startMillSec, endMillSec:$endMillSec")
        this.mStartMillSec = startMillSec
        this.mEndMillSec = endMillSec

        var time = (mEndMillSec - mStartMillSec)
        if (time > mediaDuration) {
            time = mediaDuration
        }
        adjustSelection()

        val selSec = time / 1000f
        toast_msg_tv.text = "已截取${secFormat.format(selSec)}s, [$mStartMillSec - $mEndMillSec]"
        toast_msg_tv.visibility = View.VISIBLE

        this.mHandler.removeMessages(MSG_UPDATE)
        if (finished) {
            this.mHandler.sendEmptyMessageDelayed(MSG_UPDATE, 20)
        }

        if (!finished) {
            pausePlayer()
        }

        seekToPosition(mStartMillSec)

        if (finished) {
            frozontime = System.currentTimeMillis() + 500
            startPlayer()
            videoPlayTimeController?.setPlayTimeRange(mStartMillSec, mEndMillSec)
        }
    }

    override fun onPreviewChang(startMillSec: Long, finished: Boolean) {
//        Log.d(TAG, "onPreviewChang   startMillSec:$startMillSec")
        val selSec = startMillSec / 1000f
        toast_msg_tv.text = "预览到${secFormat.format(selSec)}s"
        toast_msg_tv.visibility = View.VISIBLE
        if (!finished) {
            pausePlayer()
        }

        seekToPosition(startMillSec)


        if (finished) {
            frozontime = System.currentTimeMillis() + 500
            startPlayer()
        }

        this.mHandler.removeMessages(MSG_UPDATE)
        if (finished) {
            this.mHandler.sendEmptyMessageDelayed(MSG_UPDATE, 20)
        }
    }

    private fun adjustSelection() {
        if (mEndMillSec > mediaDuration) {
            mEndMillSec = mediaDuration
        }
        if (mStartMillSec < 0) {
            mStartMillSec = 0
        }

        if (mStartMillSec + Config.minSelection > mEndMillSec && mEndMillSec < mediaDuration) {
            mEndMillSec = min(mStartMillSec + Config.minSelection, mediaDuration)
            if (mStartMillSec + Config.minSelection > mEndMillSec && mStartMillSec > 0) {
                mStartMillSec = max(0, mEndMillSec - Config.minSelection)
            }
        }
    }
}