package com.tuya.smart.videomanagerdemo.helper

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.tuya.smart.videomanagerdemo.writeToFile

/**
 *  author : wanlinruo
 *  date : 2020/10/26 20:08
 *  contact : ln.wan@tuya.com
 *  description :
 */
class ThumbUtil(private val context: Context) {
    companion object {
        const val TAG = "ThumbExoPlayerView"
    }

    private val metadataRetriever = MediaMetadataRetriever()

    fun getThumb(
        videoPathInput: String,
        millsecsPerFrame: Int,
        thubnailCount: Int,
        callback: (String, Int) -> Unit
    ) {
        //子线程中执行
        Thread {
            metadataRetriever.setDataSource(videoPathInput)
            //时刻
            var millSec = 0L
            //循环
            for (index in 0 until thubnailCount) {
                //这里传入的是ms
                val frameAtIndex = metadataRetriever.getFrameAtTime(millSec)
                val frame = Bitmap.createScaledBitmap(
                    frameAtIndex,
                    frameAtIndex.width / 8,
                    frameAtIndex.height / 8,
                    false
                )
                frameAtIndex.recycle()

                //存储缩略图
                context.externalCacheDir?.let {
                    val fileName = it.absolutePath + "thumbnail_" + index
                    writeToFile(frame, fileName)
                    callback.invoke(fileName, index)
                }
                //时刻增加
                millSec += millsecsPerFrame * 1000.toLong()
            }
        }.start()
    }

    fun release() {
        metadataRetriever.release()
    }
}