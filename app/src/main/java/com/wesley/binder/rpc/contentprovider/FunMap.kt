package com.wesley.binder.rpc.contentprovider


import android.content.Context
import android.os.Bundle
import android.util.Log

class FunMap(private val context: Context) {
    private val functions: HashMap<String, (Bundle) -> Bundle?> = HashMap()

    init {
        init()
    }

    private fun init() {
        functions[MethodDef.SAY_HELLO] = { bundle ->
            Log.i(TAG, "say hello from ${bundle.getString(ProviderHelper.KEY_ONE)}")
            null
        }
        functions[MethodDef.GET_NAME] = lambda@{
            return@lambda Bundle().apply {
                putString(ProviderHelper.KEY_RET, context.packageName)
            }
        }
    }


    fun callFun(methodName: String, bundle: Bundle): Bundle {
        return functions[methodName]?.invoke(bundle) ?: Bundle()
    }


    companion object {
        private const val TAG = "ProviderFunMap"
    }
}