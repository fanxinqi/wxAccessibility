package cn.coderpig.cpfastaccessibility

import android.util.Log
import cn.coderpig.cp_fast_accessibility.AnalyzeSourceResult
import cn.coderpig.cp_fast_accessibility.EventWrapper

/**
 * 步骤接口，定义每个步骤的条件判断和操作
 */
interface Step {
    /**
     * 检查当前步骤的触发条件是否满足
     * @param wrapper 事件包装器
     * @param result 分析结果
     * @return true表示条件满足，可以执行操作
     */
    fun condition(wrapper: EventWrapper?, result: AnalyzeSourceResult): Boolean
    
    /**
     * 执行当前步骤的操作
     * @param wrapper 事件包装器
     * @param result 分析结果
     */
    fun action(wrapper: EventWrapper?, result: AnalyzeSourceResult)
    
    /**
     * 步骤名称，用于日志记录
     */
    val name: String
}

/**
 * 状态机类，管理步骤的执行状态
 */
class StateMachine(
    private val steps: List<Step>,
    private val tag: String = "StateMachine"
) {
    private var currentStepIndex = 0
    
    /**
     * 获取当前步骤索引
     */
    fun getCurrentStepIndex(): Int = currentStepIndex
    
    /**
     * 获取当前步骤
     */
    fun getCurrentStep(): Step? {
        return if (currentStepIndex < steps.size) {
            steps[currentStepIndex]
        } else {
            null
        }
    }
    
    /**
     * 检查状态机是否已完成所有步骤
     */
    fun isCompleted(): Boolean = currentStepIndex >= steps.size
    
    /**
     * 重置状态机到初始状态
     */
    fun reset() {
        currentStepIndex = 0
        Log.d(tag, "状态机已重置")
    }
    
    /**
     * 处理无障碍事件回调
     * @param wrapper 事件包装器
     * @param result 分析结果
     * @return true表示有步骤被执行，false表示没有步骤被执行
     */
    fun processEvent(wrapper: EventWrapper?, result: AnalyzeSourceResult): Boolean {
        if (isCompleted()) {
            Log.d(tag, "所有步骤已完成")
            return false
        }
        
        val currentStep = getCurrentStep() ?: return false
        
        Log.d(tag, "检查步骤 ${currentStepIndex + 1}/${steps.size}: ${currentStep.name}")
        
        // 检查当前步骤的条件是否满足
        if (currentStep.condition(wrapper, result)) {
            Log.d(tag, "步骤 ${currentStepIndex + 1}: ${currentStep.name} - 条件满足，开始执行")
            
            try {
                // 执行当前步骤的操作
                currentStep.action(wrapper, result)
                
                // 推进到下一步
                currentStepIndex++
                
                Log.d(tag, "步骤 ${currentStepIndex}: ${currentStep.name} - 执行完成，推进到下一步")
                
                if (isCompleted()) {
                    Log.d(tag, "所有步骤执行完毕！")
                }
                
                return true
            } catch (e: Exception) {
                Log.e(tag, "步骤 ${currentStepIndex + 1}: ${currentStep.name} - 执行失败: ${e.message}", e)
                return false
            }
        } else {
            Log.d(tag, "步骤 ${currentStepIndex + 1}: ${currentStep.name} - 条件不满足，等待下次事件")
            return false
        }
    }
    
    /**
     * 获取状态机的状态信息
     */
    fun getStatus(): String {
        return if (isCompleted()) {
            "已完成 (${steps.size}/${steps.size})"
        } else {
            val currentStep = getCurrentStep()
            "进行中 (${currentStepIndex + 1}/${steps.size}) - ${currentStep?.name ?: "未知步骤"}"
        }
    }
}
