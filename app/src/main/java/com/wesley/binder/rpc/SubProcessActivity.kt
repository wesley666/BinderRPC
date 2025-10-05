package com.wesley.binder.rpc

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.wesley.binder.rpc.contentprovider.TestProviderRPC
import com.wesley.binder.rpc.intent.Test

class SubProcessActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "TestSubProcessActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sub_process)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Test().testClient(intent)
        TestProviderRPC(this).apply {
            sayHello("wesley")
            Log.e(TAG, "onCreate: ${getName()}")
        }
    }
}