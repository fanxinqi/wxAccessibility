package cn.coderpig.cpfastaccessibility

import android.util.Log
import cn.coderpig.cp_fast_accessibility.AnalyzeSourceResult
import cn.coderpig.cp_fast_accessibility.EventWrapper
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.findNodeByClassName
import cn.coderpig.cp_fast_accessibility.findNodeByLabel
import cn.coderpig.cp_fast_accessibility.input

/**
 * 查找并点击联系人的步骤
 */
class FindAndClickContactStep(
    private val contactName: String,
    override val name: String = "查找并点击联系人($contactName)"
) : Step {
    
    override fun condition(wrapper: EventWrapper?, result: AnalyzeSourceResult): Boolean {
        // 检查是否能找到指定的联系人
        val contact = result.findNodeByLabel(contactName)
        return contact != null
    }
    
    override fun action(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        val contact = result.findNodeByLabel(contactName)
        if (contact != null) {
            Log.d("FindAndClickContactStep", "点击联系人: $contactName")
            contact.click()
        }
    }
}

/**
 * 点击输入框并输入文本的步骤
 */
class ClickInputAndSendTextStep(
    private val text: String,
    override val name: String = "点击输入框并输入文本"
) : Step {
    
    override fun condition(wrapper: EventWrapper?, result: AnalyzeSourceResult): Boolean {
        // 检查是否能找到文本输入框
        val inputNode = result.findNodeByClassName("android.widget.EditText")
        return inputNode != null
    }
    
    override fun action(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        val inputNode = result.findNodeByClassName("android.widget.EditText")
        if (inputNode != null) {
            Log.d("ClickInputAndSendTextStep", "点击输入框并输入文本: $text")
            inputNode.click()
            inputNode.input(text)
        }
    }
}

/**
 * 等待特定界面出现的步骤
 */
class WaitForUIStep(
    private val uiCheckFunction: (AnalyzeSourceResult) -> Boolean,
    override val name: String = "等待界面出现"
) : Step {
    
    override fun condition(wrapper: EventWrapper?, result: AnalyzeSourceResult): Boolean {
        return uiCheckFunction(result)
    }
    
    override fun action(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        Log.d("WaitForUIStep", "目标界面已出现")
        // 等待步骤通常不需要执行具体操作，只是确认界面状态
    }
}

/**
 * 自定义步骤，允许完全自定义条件和操作
 */
class CustomStep(
    private val conditionFunction: (EventWrapper?, AnalyzeSourceResult) -> Boolean,
    private val actionFunction: (EventWrapper?, AnalyzeSourceResult) -> Unit,
    override val name: String
) : Step {
    
    override fun condition(wrapper: EventWrapper?, result: AnalyzeSourceResult): Boolean {
        return conditionFunction(wrapper, result)
    }
    
    override fun action(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        actionFunction(wrapper, result)
    }
}

/**
 * 延迟步骤，用于在某些操作之间添加等待时间
 */
class DelayStep(
    private val delayMs: Long,
    override val name: String = "延迟等待(${delayMs}ms)"
) : Step {
    
    private var startTime: Long = 0
    private var isStarted = false
    
    override fun condition(wrapper: EventWrapper?, result: AnalyzeSourceResult): Boolean {
        if (!isStarted) {
            startTime = System.currentTimeMillis()
            isStarted = true
            Log.d("DelayStep", "开始延迟等待 ${delayMs}ms")
            return false
        }
        
        val elapsed = System.currentTimeMillis() - startTime
        return elapsed >= delayMs
    }
    
    override fun action(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        Log.d("DelayStep", "延迟等待完成")
        // 重置状态以便下次使用
        isStarted = false
    }
}
