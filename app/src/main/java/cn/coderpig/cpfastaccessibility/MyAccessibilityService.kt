package cn.coderpig.cpfastaccessibility
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import cn.coderpig.cp_fast_accessibility.AnalyzeSourceResult
import cn.coderpig.cp_fast_accessibility.EventWrapper
import cn.coderpig.cp_fast_accessibility.FastAccessibilityService
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.findNodesByIdAndClassName
import cn.coderpig.cp_fast_accessibility.input
import cn.coderpig.cp_fast_accessibility.sleep
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import java.io.IOException
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import android.util.Log
import cn.coderpig.cp_fast_accessibility.findNodeByClassName
import cn.coderpig.cp_fast_accessibility.findNodeByLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyAccessibilityService : FastAccessibilityService() {

    companion object {
        private const val TAG = "CpFastAccessibility"
        private const val POLLING_URL = "http://10.6.2.110:8000/home.json"
        private const val POLLING_INTERVAL = 10000L // ms

        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        private val gson = Gson()

        data class ApiResponse(
            @SerializedName("status") val status: String?,
            @SerializedName("data") val data: List<MessageData>?
        )

        data class MessageData(
            @SerializedName("text") val text: String?
        )
    }

    override val enableListenApp = true

    private var pollingJob: Job? = null
    private var pendingMessage: String? = null
    private val weChatSender = WeChatMessageSender() // 使用封装的消息发送器
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onServiceConnected() {
        super.onServiceConnected()
//        pendingMessage = "测试ai消息原子发送"
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel() // 防止重复启动
        pollingJob = serviceScope.launch {
            Log.d(TAG, "开始轮询消息")
            while (isActive) {
                try {
                    checkForNewMessages()
                } catch (e: Exception) {
                    Log.e(TAG, "轮询出错: ${e.message}", e)
                }
                delay(POLLING_INTERVAL)
            }
        }
        Log.d(TAG, "轮询任务已启动")
    }

    private suspend fun checkForNewMessages() {
        val request = Request.Builder().url(POLLING_URL).build()

        try {
            val response = withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute()
            }

            val responseBody = response.body?.string().orEmpty()
            Log.d(TAG, "原始响应: $responseBody")

            val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)

            if (apiResponse.status == "success" && !apiResponse.data.isNullOrEmpty()) {
                val messageText = apiResponse.data.firstOrNull()?.text.orEmpty()
                if (messageText.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        pendingMessage = messageText
                        Log.d(TAG, "成功提取消息: $messageText")
                    }
                }
            } else {
                Log.e(TAG, "响应格式错误或数据为空")
            }
        } catch (e: Exception) {
            Log.e(TAG, "网络或解析失败: ${e.message}")
        }
    }

    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        // 增加日志记录，便于调试
        Log.d(TAG, "analyzeCallBack - 包名: ${wrapper?.packageName}, 类名: ${wrapper?.className}")

        // 处理微信消息发送
//        if (wrapper?.packageName == "com.tencent.mm") {
        pendingMessage?.let { message ->
            Log.d(TAG, "检测到微信界面，准备发送消息: $message")
            // 确保不会重复执行
            if (!weChatSender.isSending()) {
                pendingMessage = null
                weChatSender.sendMessage(result, message) { success ->
                    if (success) {
                        Log.d(TAG, "消息发送成功回调: $message")
                    } else {
                        Log.e(TAG, "消息发送失败回调: $message")
                    }
                }
            } else {
                Log.w(TAG, "消息正在发送中，跳过本次执行")
            }
        } ?: run {
            Log.d(TAG, "没有待发送的消息")
        }
//        }
    }

    /**
     * 公共接口：供外部调用的原子化消息发送方法
     * @param message 要发送的消息
     */
    fun sendMessageAtomic(message: String) {
        if (weChatSender.isSending()) {
            Log.w(TAG, "已有消息正在发送中，忽略本次请求")
            return
        }

        // 获取当前的窗口内容
        val result = weChatSender.getCurrentWindowAnalyzeResult(this)
        if (result != null) {
            weChatSender.sendMessage(result, message) { success ->
                if (success) {
                    Log.d(TAG, "外部调用发送消息成功: $message")
                } else {
                    Log.e(TAG, "外部调用发送消息失败: $message")
                }
            }
        } else {
            Log.e(TAG, "无法获取当前窗口内容，消息发送失败")
        }
    }

    /**
     * 测试方法：手动触发消息发送
     * 可以用于调试和测试
     */
    fun testSendMessage(testMessage: String = "测试消息") {
        Log.d(TAG, "手动触发测试消息发送: $testMessage")
        pendingMessage = testMessage

        // 强制设置一个测试消息来验证功能
        if (!weChatSender.isSending()) {
            Log.d(TAG, "开始测试发送流程")
        }
    }

    /**
     * 调试方法：打印当前窗口所有节点信息
     */
    fun debugCurrentWindow() {
        // TODO: Implement debug functionality
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        pollingJob = null
    }
}
