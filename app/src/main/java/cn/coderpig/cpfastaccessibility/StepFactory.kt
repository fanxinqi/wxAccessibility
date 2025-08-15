package cn.coderpig.cpfastaccessibility

import cn.coderpig.cp_fast_accessibility.AnalyzeSourceResult
import cn.coderpig.cp_fast_accessibility.EventWrapper
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.findNodeByClassName
import cn.coderpig.cp_fast_accessibility.findNodeByLabel
import cn.coderpig.cp_fast_accessibility.findNodeByText
import cn.coderpig.cp_fast_accessibility.input

/**
 * 步骤工厂类，提供常用的步骤组合
 */
object StepFactory {
    
    /**
     * 创建微信发送消息的完整流程
     * @param contactName 联系人名称
     * @param message 要发送的消息
     * @return 步骤列表
     */
    fun createWeChatSendMessageFlow(contactName: String, message: String): List<Step> {
        return listOf(
            // 第一步：查找并点击联系人
            FindAndClickContactStep(contactName),
            
            // 第二步：等待聊天界面加载
            DelayStep(1000, "等待聊天界面加载"),
            
            // 第三步：点击输入框并输入文本
            ClickInputAndSendTextStep(message),
            
            // 第四步：点击发送按钮
            CustomStep(
                conditionFunction = { _, result -> 
                    result.findNodeByText("发送") != null 
                },
                actionFunction = { _, result -> 
                    result.findNodeByText("发送")?.click()
                },
                name = "点击发送按钮"
            ),
            
            // 第五步：等待发送完成
            DelayStep(500, "等待发送完成")
        )
    }
    
    /**
     * 创建简化的微信发送消息流程（不包含发送按钮点击）
     * @param contactName 联系人名称
     * @param message 要发送的消息
     * @return 步骤列表
     */
    fun createSimpleWeChatSendFlow(contactName: String, message: String): List<Step> {
        return listOf(
            FindAndClickContactStep(contactName),
            DelayStep(1000, "等待聊天界面加载"),
            ClickInputAndSendTextStep(message)
        )
    }
    
    /**
     * 创建等待特定界面出现的步骤
     * @param uiIdentifier 界面标识（文本或类名）
     * @param stepName 步骤名称
     * @return 等待步骤
     */
    fun createWaitForUIStep(uiIdentifier: String, stepName: String = "等待界面出现"): Step {
        return WaitForUIStep(
            uiCheckFunction = { result ->
                result.findNodeByText(uiIdentifier) != null ||
                result.findNodeByClassName(uiIdentifier) != null
            },
            name = stepName
        )
    }
    
    /**
     * 创建动态消息发送流程，根据当前消息内容动态调整
     * @param contactName 联系人名称
     * @param messageProvider 消息提供器函数
     * @return 步骤列表
     */
    fun createDynamicWeChatSendFlow(
        contactName: String, 
        messageProvider: () -> String?
    ): List<Step> {
        return listOf(
            FindAndClickContactStep(contactName),
            DelayStep(1000, "等待聊天界面加载"),
            CustomStep(
                conditionFunction = { _, result ->
                    val message = messageProvider()
                    message != null && result.findNodeByClassName("android.widget.EditText") != null
                },
                actionFunction = { _, result ->
                    val message = messageProvider()
                    if (message != null) {
                        val inputNode = result.findNodeByClassName("android.widget.EditText")
                        inputNode?.let {
                            it.click()
                            it.input(message)
                        }
                    }
                },
                name = "输入动态消息内容"
            ),
            CustomStep(
                conditionFunction = { _, result ->
                    val message = messageProvider()
                    message != null && result.findNodeByLabel("发送") != null
                },
                actionFunction = { _, result ->
                    val message = messageProvider()
                    if (message != null) {
                        val inputNode = result.findNodeByLabel("发送")
                        inputNode?.click()
                    }
                },
                name = "输入动态消息内容"
            )
        )
    }
}
