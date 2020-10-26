package com.tuya.smart.videomanagerdemo.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import com.tuya.smart.videomanagerdemo.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {


    companion object {
        const val PERMISSION_REQUEST_CODE = 1000
        const val REQUEST_PICK_CLIP_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //视频裁剪
        btn_clip.setOnClickListener { selectVideo() }
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    /**
     * 从系统中选择视频
     */
    private fun selectVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_PICK_CLIP_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val videoItem = getVideoItem(contentResolver, data)
            videoItem?.run {
                log("video title:$title, duration:${durationSec}, size:$size, path:$path")
                when (requestCode) {
                    REQUEST_PICK_CLIP_CODE -> startActivity(this, VideoClipActivity::class.java)
                    else -> null
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun startActivity(videoItem: VideoItem, activityClass: Class<*>) {
        startActivity(Intent(this, activityClass).apply {
            videoItem.run {
                putExtra("video_path", path)
            }
        })
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        // request permission if it has not been grunted.
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@MainActivity,
                    "permission has been grunted.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "[WARN] permission is not grunted.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}