package com.wesley.binder.rpc.contentprovider

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.wesley.binder.rpc.putValue


@SuppressLint("StaticFieldLeak")
class ProviderHelper(private val context: Context, private val uriPath: String) {
    companion object {
        const val KEY_ONE: String = "key_one"
        const val KEY_TWO: String = "key_two"
        const val KEY_THREE: String = "key_three"
        const val KEY_FOUR: String = "key_four"
        const val KEY_FIVE: String = "key_five"
        const val KEY_RET: String = "ret"
        const val CALL_FUNCTION: String = "callFunction"
        const val TAG: String = "ProviderHelper"
    }

    private val mContentResolver: ContentResolver? = context.contentResolver


    fun callFunction(methodName: String, bundle: Bundle): Bundle? {
        val client = getClient() ?: return Bundle().apply {
            Log.e(TAG, "callFunction $methodName: client is null")
        }
        return try {
            client.call(CALL_FUNCTION, methodName, bundle)
        } catch (e: Exception) {
            Bundle()
        } finally {
            //如果频繁调用，可独立成一个函数，避免每次都close
            client.release()
        }
    }

    fun setParams(
        obj: Any? = null,
        obj2: Any? = null,
        obj3: Any? = null,
        obj4: Any? = null,
        obj5: Any? = null
    ): Bundle {
        val bundle = Bundle()
        putValue(bundle, KEY_ONE, obj)
        putValue(bundle, KEY_TWO, obj2)
        putValue(bundle, KEY_THREE, obj3)
        putValue(bundle, KEY_FOUR, obj4)
        putValue(bundle, KEY_FIVE, obj5)
        return bundle
    }

    private fun putValue(bundle: Bundle, key: String, obj: Any?) {
        if (obj == null) {
            return
        }
        bundle.putValue(key, obj)
    }

    private fun getClient(): ContentProviderClient? {
        val contentResolver = this.mContentResolver ?: return null
        return contentResolver.acquireUnstableContentProviderClient(Uri.parse(this.uriPath))
    }
}