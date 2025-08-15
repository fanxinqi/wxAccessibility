# 状态机使用示例

## 基本概念

这个状态机系统设计用于管理无障碍服务中的多步骤操作，确保每个步骤只执行一次，并且可以根据条件自动推进到下一步。

## 核心组件

### 1. Step 接口
每个步骤都实现 `Step` 接口，包含：
- `condition()`: 检查步骤的触发条件
- `action()`: 执行步骤的操作
- `name`: 步骤名称（用于日志）

### 2. StateMachine 类
状态机管理步骤的执行：
- 维护当前步骤索引
- 处理事件回调
- 自动推进到下一步
- 提供状态查询功能

### 3. 预定义步骤类
- `FindAndClickContactStep`: 查找并点击联系人
- `ClickInputAndSendTextStep`: 点击输入框并输入文本
- `WaitForUIStep`: 等待特定界面出现
- `DelayStep`: 延迟等待
- `CustomStep`: 自定义步骤

## 使用示例

### 1. 基本使用
```kotlin
// 创建步骤列表
val steps = listOf(
    FindAndClickContactStep("联系人名称"),
    DelayStep(1000),
    ClickInputAndSendTextStep("要发送的消息")
)

// 创建状态机
val stateMachine = StateMachine(steps, "MyTag")

// 在事件回调中处理
override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
    stateMachine.processEvent(wrapper, result)
}
```

### 2. 使用工厂类
```kotlin
// 创建完整的微信发送流程
val steps = StepFactory.createWeChatSendMessageFlow("联系人", "消息内容")
val stateMachine = StateMachine(steps)

// 创建简化流程
val simpleSteps = StepFactory.createSimpleWeChatSendFlow("联系人", "消息")

// 创建动态消息流程
val dynamicSteps = StepFactory.createDynamicWeChatSendFlow("联系人") {
    getCurrentMessage() // 动态获取消息
}
```

### 3. 自定义步骤
```kotlin
val customStep = CustomStep(
    conditionFunction = { wrapper, result ->
        // 自定义条件逻辑
        result.findNodeByText("目标文本") != null
    },
    actionFunction = { wrapper, result ->
        // 自定义操作逻辑
        result.findNodeByText("目标文本")?.click()
    },
    name = "自定义步骤名称"
)
```

### 4. 状态管理
```kotlin
// 检查状态机是否完成
if (stateMachine.isCompleted()) {
    println("所有步骤已完成")
}

// 获取当前状态
val status = stateMachine.getStatus()
println("当前状态: $status")

// 重置状态机
stateMachine.reset()

// 获取当前步骤
val currentStep = stateMachine.getCurrentStep()
```

## 高级用法

### 1. 条件步骤
```kotlin
val conditionalStep = CustomStep(
    conditionFunction = { wrapper, result ->
        // 只在特定应用中执行
        wrapper?.packageName == "com.tencent.mm" &&
        result.findNodeByText("发送") != null
    },
    actionFunction = { wrapper, result ->
        result.findNodeByText("发送")?.click()
    },
    name = "条件发送"
)
```

### 2. 循环检测
```kotlin
val waitStep = WaitForUIStep(
    uiCheckFunction = { result ->
        // 等待加载完成
        result.findNodeByText("加载中") == null
    },
    name = "等待加载完成"
)
```

### 3. 错误处理
```kotlin
val safeStep = CustomStep(
    conditionFunction = { wrapper, result ->
        result.findNodeByText("目标") != null
    },
    actionFunction = { wrapper, result ->
        try {
            result.findNodeByText("目标")?.click()
        } catch (e: Exception) {
            Log.e("SafeStep", "操作失败", e)
        }
    },
    name = "安全操作步骤"
)
```

## 注意事项

1. **步骤设计**: 每个步骤应该是原子性的，只做一件事
2. **条件检查**: 条件函数应该尽可能精确，避免误触发
3. **错误处理**: 在 action 中添加适当的错误处理
4. **日志记录**: 充分利用日志来调试和监控执行过程
5. **性能考虑**: 避免在条件检查中执行耗时操作

## 调试技巧

1. 使用日志查看状态机状态：`stateMachine.getStatus()`
2. 检查当前步骤：`stateMachine.getCurrentStep()?.name`
3. 监控步骤执行：观察 `processEvent()` 的返回值
4. 使用延迟步骤来观察界面变化
