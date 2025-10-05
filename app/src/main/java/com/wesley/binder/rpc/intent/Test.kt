package com.wesley.binder.rpc.intent

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.RemoteException
import android.util.Log
import com.wesley.binder.rpc.ACTIVITY_INTENT_KEY
import com.wesley.binder.rpc.IMessenger
import com.wesley.binder.rpc.MESSENGER_KEY
import com.wesley.binder.rpc.getBinderValue
import com.wesley.binder.rpc.putValue

class Test {
    companion object {
        private const val TAG = "TestIntentRPC"
    }

    fun testServer(context: Context) {
        val messenger = TestMessenger()
        val intent = Intent()
        intent.`package` = context.packageName
        intent.action = "android.intent.action.AUTHORIZE"
        val bundle = Bundle().apply {
            putValue(MESSENGER_KEY, messenger)
        }
        intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(ACTIVITY_INTENT_KEY, bundle)
        }
        context.startActivity(intent)
        synchronized(messenger.lock) {
            while (!messenger.finished) {
                try {
                    messenger.lock.wait(10 * 1000L)
                    messenger.finished = true
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        Log.e(TAG, "testServer messenger.result: ${messenger.result}")
    }

    fun testClient(intent: Intent?) {
        var iBinder: IBinder? = null
        var messenger: IMessenger? = null
        intent?.getBundleExtra(ACTIVITY_INTENT_KEY)?.apply {
            iBinder = getBinderValue(MESSENGER_KEY)
        }
        iBinder?.let {
            messenger = IMessenger.Stub.asInterface(it)
        }
        messenger?.let {
            val message = Message()
            message.what = 99
            it.send(message)
        }

    }

    private inner class TestMessenger : IMessenger.Stub() {
        @Volatile
        var finished: Boolean = false
        var result: Int = 0
        val lock = Object()

        @SuppressLint("RethrowRemoteException", "VisiblySynchronized", "MissingNullability")
        @Throws(
            RemoteException::class
        )
        override fun send(message: Message) {
            synchronized(lock) {
                Log.i(TAG, "onAuthCheck send result: ${message.what}")
                this.finished = true
                this.result = message.what
                lock.notifyAll()
            }
        }
    }
}