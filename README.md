原文：[基于Binder的4种RPC调用 - Wesley's Blog](https://iwesley.top/article/f037b587/)

基于Binder的4种RPC调用一般有AIDL、Messenger、借助 intent 传递 messenger 和 基于 contentprovider 的 call

AIDL 和Messenger 比较常见了，可以参考：[绑定服务概览  |  Background work  |  Android Developers](https://developer.android.com/develop/background-work/services/bound-services?hl=zh-cn)

这里只讨论后面两种情况，代码链接：https://github.com/wesley666/BinderRPC

## 借助 intent 传递 messenger 

应用场景：比如授权弹窗需要借助另外一个进程或者组件完成，比如小米 adb 安装应用时的弹窗，安装流程是 system_server 完成的，但安全校验弹窗是安全中心负责的，方便解耦。

```kotlin
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
```

framework 的代码可以借助IMessenger.Stub，app的代码可以自定义IMessenger

```java
@SuppressLint({"RawAidl", "CallbackMethodName", "MissingNullability", "RethrowRemoteException"})
    public static class TestCallback extends IMessenger.Stub {
        boolean finished;
        String msg;
        int result;

        @SuppressLint({"RethrowRemoteException", "VisiblySynchronized", "MissingNullability"})
        public void send(@NonNull Message message) throws RemoteException {
            synchronized (this) {
                this.finished = true;
                this.result = message.what;
                Bundle data = message.getData();
                if (data != null) {
                    this.msg = data.getString("msg");
                }
                notifyAll();
            }
        }
    }
```



## 基于 contentprovider 的 call 方法

应用场景：比如需要同步 RPC 调用，aidl 是异步绑定的；新增或者删减方法更加方便，aidl 需要严格的方法定义顺序。

比如Settings.Secure.putInt()本质上就是利用了call方法进行rpc，可以参考：

[frameworks/base/core/java/android/provider/Settings.java](https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/provider/Settings.java?q=symbol%3A%5Cbandroid.provider.Settings.NameValueCache.putStringForUser%5Cb%20case%3Ayes)

[frameworks/base/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java](https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/packages/SettingsProvider/src/com/android/providers/settings/SettingsProvider.java?q=symbol%3A%5Cbcom.android.providers.settings.SettingsProvider.call%5Cb%20case%3Ayes)

定义服务端：contentprovider，call里面也可以增加鉴权等

```kotlin
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (ProviderHelper.CALL_FUNCTION == method && extras != null && arg != null) {
            return funMap.callFun(arg, extras)
        }
        return super.call(method, arg, extras)
    }
```

服务端方法定义

```kotlin
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
```

公共方法

```kotlin
object MethodDef {
    const val SAY_HELLO = "sayHello"
    const val GET_NAME = "getName"
}

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
```

客户端

```kotlin
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
```

