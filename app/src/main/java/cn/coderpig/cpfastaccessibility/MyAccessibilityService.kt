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
            @SerializedName("data") val data: MessageData?
        )

        data class MessageData(
            @SerializedName("text") val text: String?
        )
    }

    override val enableListenApp = true

    private var pollingJob: Job? = null
    private var pendingMessage: String? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    // 状态机实例
    private lateinit var stateMachine: StateMachine

    override fun onServiceConnected() {
        super.onServiceConnected()
//        pendingMessage = "测试ai消息原子发送"
        initializeStateMachine()
        startPolling()
    }
    
    /**
     * 初始化状态机，定义微信发送消息的步骤流程
     */
    private fun initializeStateMachine() {
        // 使用工厂类创建动态消息发送流程
        val steps = StepFactory.createDynamicWeChatSendFlow("penelop") {
            "测试" // 返回当前待发送的消息
        }
        
        stateMachine = StateMachine(steps, TAG)
        Log.d(TAG, "状态机初始化完成，共 ${steps.size} 个步骤")
    }
    
    /**
     * 触发新的消息发送流程
     * @param message 要发送的消息
     * @param contactName 联系人名称，默认为 "penelop"
     */
    private fun triggerMessageSend(message: String, contactName: String = "penelop") {
        pendingMessage = message
        
        // 如果状态机已完成或未开始，重置并开始新的流程
        if (stateMachine.isCompleted() || stateMachine.getCurrentStepIndex() == 0) {
            // 创建新的发送流程
            val steps = StepFactory.createDynamicWeChatSendFlow(contactName) {
                pendingMessage
            }
            stateMachine = StateMachine(steps, TAG)
            Log.d(TAG, "创建新的消息发送流程: $message -> $contactName")
        }
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

            val apiResponse = try {
                gson.fromJson(responseBody, ApiResponse::class.java)
            } catch (e: com.google.gson.JsonSyntaxException) {
                Log.e(TAG, "JSON解析失败: ${e.message}")
                null
            }

            if (apiResponse?.status == "success" && apiResponse?.data?.text?.isNullOrBlank() == false) {
                val messageText = apiResponse.data?.text.orEmpty()
                if (messageText.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "成功提取消息: $messageText")
                        // 触发新的消息发送流程
                        triggerMessageSend(messageText)
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
        // 使用状态机处理事件
        val stepExecuted = stateMachine.processEvent(wrapper, result)
        
        // 记录状态机状态
        if (stepExecuted) {
            Log.d(TAG, "步骤执行成功，当前状态: ${stateMachine.getStatus()}")
        }
        
        // 如果状态机已完成，清理当前消息
        if (stateMachine.isCompleted() && pendingMessage != null) {
            Log.d(TAG, "消息发送流程完成: $pendingMessage")
            pendingMessage = null // 清理已发送的消息
        }
        
        // 增加日志记录，便于调试
        Log.d(TAG, "analyzeCallBack - 包名: ${wrapper?.packageName}, 类名: ${wrapper?.className}")
    }    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        pollingJob = null
    }
}
