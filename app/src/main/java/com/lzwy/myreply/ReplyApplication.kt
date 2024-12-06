package com.lzwy.myreply

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV

const val TAG = "ReplyApplication"
class ReplyApplication: Application() {

    override fun onCreate() {
        super.onCreate()

    }

    private fun init() {
        // MMKV part
        val mmkvRootDir = MMKV.initialize(this)
        Log.i(TAG, "init MMKV, root dir: $mmkvRootDir")
    }

}