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

    override fun onServiceConnected() {
        super.onServiceConnected()
        startPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel() // 防止重复启动
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    checkForNewMessages()
                } catch (e: Exception) {
                    Log.e(TAG, "轮询出错: ${e.message}")
                }
                delay(POLLING_INTERVAL)
            }
        }
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
        if (wrapper?.packageName == "com.tencent.mm" && wrapper.className == "android.widget.LinearLayout") {
            pendingMessage?.let { message ->
                pendingMessage = null
                sendWeChatMessageAsync(result, message)
            }
        }
    }

    private fun sendWeChatMessageAsync(result: AnalyzeSourceResult, message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val inputNode = result.findNodesByIdAndClassName("com.tencent.mm:id/bkk", "android.widget.EditText")
            if (inputNode != null) {
                inputNode.click()
                delay(400)
                inputNode.input(message)
                delay(400)
            } else {
                Log.e(TAG, "找不到输入框节点")
            }

            val sendButton = result.findNodesByIdAndClassName("com.tencent.mm:id/bql", "android.widget.Button")
            if (sendButton != null) {
                sendButton?.click(false)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        pollingJob = null
    }
}