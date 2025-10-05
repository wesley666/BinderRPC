package com.wesley.binder.rpc

import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import java.io.Serializable

private const val TAG = "IPC"

const val MESSENGER_KEY = "test_key"
const val ACTIVITY_INTENT_KEY = "intent_key"
fun Bundle.putValue(key: String, value: Any) {
    if (value is String) {
        putString(key, value)
    } else if (value is Int) {
        putInt(key, value)
    } else if (value is Boolean) {
        putBoolean(key, value)
    } else if (value is ByteArray) {
        putByteArray(key, value)
    } else if (value is Binder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            putBinder(key, value as IBinder)
        } else {
            try {
                Reflector.with(this).method(
                    "putIBinder",
                    String::class.java,
                    IBinder::class.java
                ).call<Any>(key, value)
            } catch (e: Reflector.ReflectedException) {
                Log.e(TAG, "put Binder Value error: ", e)
            }
        }
    } else if (value is Serializable) {
        putSerializable(key, value)
    } else if (value is Parcelable) {
        putParcelable(key, value)
    }
}

fun Bundle.getBinderValue(key: String): IBinder? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        getBinder(key)
    } else {
        try {
            Reflector.with(this).method("getIBinder", String::class.java)
                .call<IBinder>(key)
        } catch (e: Reflector.ReflectedException) {
            Log.e(TAG, "get Binder Value error: ", e)
            null
        }
    }
}