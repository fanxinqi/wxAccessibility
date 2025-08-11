package cn.coderpig.cpfastaccessibility

import android.util.Log
import cn.coderpig.cp_fast_accessibility.AnalyzeSourceResult
import cn.coderpig.cp_fast_accessibility.FastAccessibilityService
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.findNodesByIdAndClassName
import cn.coderpig.cp_fast_accessibility.input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 微信消息发送服务
 * 提供原子化的消息发送功能，确保点击输入框、输入内容、点击发送按钮作为一个完整的操作
 */
class WeChatMessageSender {

    companion object {
        private const val TAG = "WeChatMessageSender"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val OPERATION_TIMEOUT = 10000L // 10秒超时
        private const val INPUT_BOX_ID = "com.tencent.mm:id/bkk"
        private const val SEND_BUTTON_ID = "com.tencent.mm:id/bql"
    }

    private val isSendingMessage = AtomicBoolean(false)

    /**
     * 原子化发送微信消息
     * @param result 无障碍服务分析结果
     * @param message 要发送的消息内容
     * @param onComplete 发送完成回调，参数为发送是否成功
     */
    fun sendMessage(
        result: AnalyzeSourceResult,
        message: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (isSendingMessage.compareAndSet(false, true)) {
            executeAtomicMessageSend(result, message, onComplete)
        } else {
            Log.w(TAG, "消息正在发送中，忽略本次请求: $message")
            onComplete?.invoke(false)
        }
    }

    /**
     * 检查是否正在发送消息
     */
    fun isSending(): Boolean = isSendingMessage.get()

    /**
     * 原子化的消息发送服务
     * 这个方法确保：点击输入框 -> 输入内容 -> 点击发送 作为一个完整的原子操作
     */
    private fun executeAtomicMessageSend(
        result: AnalyzeSourceResult,
        message: String,
        onComplete: ((Boolean) -> Unit)?
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val success = withTimeoutOrNull(OPERATION_TIMEOUT) {
                    performAtomicSendOperation(result, message)
                }

                if (success == true) {
                    Log.d(TAG, "消息发送成功: $message")
                    onComplete?.invoke(true)
                } else {
                    Log.e(TAG, "消息发送失败或超时: $message")
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送消息时出现异常: ${e.message}")
                onComplete?.invoke(false)
            } finally {
                // 重置发送状态
                isSendingMessage.set(false)
            }
        }
    }

    /**
     * 执行原子化发送操作
     * @param result 无障碍服务分析结果
     * @param message 要发送的消息
     * @return 是否成功发送
     */
    private suspend fun performAtomicSendOperation(result: AnalyzeSourceResult, message: String): Boolean {
        var retryCount = 0

        while (retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                Log.d(TAG, "开始第${retryCount + 1}次尝试发送消息: $message")

                // 步骤1: 点击输入框
                val inputNode = result.findNodesByIdAndClassName(INPUT_BOX_ID, "android.widget.EditText")
                if (inputNode == null) {
                    Log.e(TAG, "找不到输入框节点，尝试查找所有EditText节点")
                    // 打印所有可编辑节点信息用于调试
                    result.nodes.filter { it.editable }.forEach { node ->
                        Log.d(TAG, "找到可编辑节点: id=${node.id}, className=${node.className}, text=${node.text}")
                    }
                    delay(500)
                    retryCount++
                    continue
                }

                Log.d(TAG, "点击输入框")
                inputNode.click()
                delay(600) // 等待输入框激活

                // 步骤2: 输入消息内容
                Log.d(TAG, "输入消息内容: $message")
                inputNode.input(message)
                delay(800) // 等待内容输入完成和发送按钮出现

                // 步骤3: 查找并点击发送按钮（重试机制）
                var sendButtonFound = false
                var buttonRetryCount = 0
                val maxButtonRetries = 5

                while (!sendButtonFound && buttonRetryCount < maxButtonRetries) {
                    val sendButton = result.findNodesByIdAndClassName(SEND_BUTTON_ID, "android.widget.Button")

                    if (sendButton != null) {
                        Log.d(TAG, "找到发送按钮，准备点击")
                        sendButton.click(false)
                        delay(300) // 等待发送完成
                        sendButtonFound = true
                        Log.d(TAG, "消息发送完成")
                        return true
                    } else {
                        Log.d(TAG, "发送按钮未出现，等待${buttonRetryCount + 1}/5")
                        // 打印所有按钮节点信息用于调试
                        result.nodes.filter { it.clickable && it.className.contains("Button") }.forEach { node ->
                            Log.d(TAG, "找到按钮节点: id=${node.id}, className=${node.className}, text=${node.text}")
                        }
                        delay(400)
                        buttonRetryCount++
                    }
                }

                if (!sendButtonFound) {
                    Log.e(TAG, "发送按钮始终未出现，本次尝试失败")
                }

            } catch (e: Exception) {
                Log.e(TAG, "第${retryCount + 1}次尝试发送时出现异常: ${e.message}")
            }

            retryCount++
            if (retryCount < MAX_RETRY_ATTEMPTS) {
                Log.d(TAG, "等待${retryCount * 500}ms后进行下一次重试")
                delay(retryCount * 500L) // 递增延迟
            }
        }

        Log.e(TAG, "所有重试都失败了，消息发送失败")
        return false
    }

    /**
     * 获取当前窗口的分析结果
     * @param service 无障碍服务实例
     * @return 分析结果，如果获取失败返回null
     */
    fun getCurrentWindowAnalyzeResult(service: FastAccessibilityService): AnalyzeSourceResult? {
        return try {
            // 获取当前活动窗口的根节点
            val rootNode = service.rootInActiveWindow
            if (rootNode != null) {
                Log.d(TAG, "成功获取当前窗口根节点")
                // 创建分析结果
                val result = AnalyzeSourceResult(arrayListOf())
                analyzeNode(rootNode, result.nodes)
                Log.d(TAG, "窗口分析完成，找到 ${result.nodes.size} 个节点")
                return result
            } else {
                Log.e(TAG, "无法获取当前窗口根节点")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取当前窗口分析结果失败: ${e.message}")
            null
        }
    }

    /**
     * 递归遍历结点的方法
     */
    private fun analyzeNode(
        node: android.view.accessibility.AccessibilityNodeInfo?,
        list: ArrayList<cn.coderpig.cp_fast_accessibility.NodeWrapper>
    ) {
        if (node == null) return
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        list.add(
            cn.coderpig.cp_fast_accessibility.NodeWrapper(
                text = node.text?.toString() ?: "",
                id = node.viewIdResourceName ?: "",
                bounds = bounds,
                className = node.className?.toString() ?: "",
                description = node.contentDescription?.toString() ?: "",
                clickable = node.isClickable,
                scrollable = node.isScrollable,
                editable = node.isEditable,
                nodeInfo = node
            )
        )
        if (node.childCount > 0) {
            for (index in 0 until node.childCount) {
                analyzeNode(node.getChild(index), list)
            }
        }
    }

    /**
     * 调试方法：打印当前窗口所有节点信息
     * @param service 无障碍服务实例
     */
    fun debugCurrentWindow(service: FastAccessibilityService) {
        val result = getCurrentWindowAnalyzeResult(service)
        if (result != null) {
            Log.d(TAG, "=== 当前窗口节点信息 ===")
            Log.d(TAG, "总共找到 ${result.nodes.size} 个节点")
            result.nodes.forEachIndexed { index, node ->
                Log.d(TAG, "节点$index: id=${node.id}, className=${node.className}, text=${node.text}, clickable=${node.clickable}, editable=${node.editable}")
            }
            Log.d(TAG, "=== 节点信息结束 ===")

            // 专门查找微信相关的输入框和发送按钮
            val editTexts = result.nodes.filter { it.editable }
            val buttons = result.nodes.filter { it.clickable && it.className.contains("Button") }

            Log.d(TAG, "找到 ${editTexts.size} 个可编辑节点:")
            editTexts.forEach { Log.d(TAG, "  EditText: id=${it.id}, text=${it.text}") }

            Log.d(TAG, "找到 ${buttons.size} 个按钮节点:")
            buttons.forEach { Log.d(TAG, "  Button: id=${it.id}, text=${it.text}") }
        } else {
            Log.e(TAG, "无法获取当前窗口信息")
        }
    }

    /**
     * 重置发送状态
     * 在某些异常情况下可能需要手动重置
     */
    fun resetSendingState() {
        isSendingMessage.set(false)
        Log.d(TAG, "发送状态已重置")
    }
}
