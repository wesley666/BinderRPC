package com.wesley.binder.rpc.contentprovider

import android.content.Context

class TestProviderRPC(private val context: Context) {
    private val providerHelper =
        ProviderHelper(context, "content://${context.packageName}.rpc.provider")

    fun sayHello(str: String) {
        providerHelper.callFunction(MethodDef.SAY_HELLO, providerHelper.setParams(str))
    }

    fun getName(): String {
        val ret = providerHelper.callFunction(MethodDef.GET_NAME, providerHelper.setParams())
        return ret?.getString(ProviderHelper.KEY_RET) ?: ""
    }
}