<!--
 * @Author: fanxinqi fanxinqi@taou.com
 * @Date: 2025-08-13 19:04:19
 * @LastEditors: fanxinqi fanxinqi@taou.com
 * @LastEditTime: 2025-08-13 19:04:23
 * @FilePath: /wxAccessibility/test.md
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
-->
sequenceDiagram
    participant User as 用户
    participant Service as 无障碍服务
    participant WeChat as 微信客户端
    
    User->>Service: 触发发送消息指令
    activate Service
    
    Service->>Service: 检查当前窗口状态
    alt 不在微信主界面
        Service->>WeChat: 启动微信应用
        Service-->>Service: 等待启动完成(3s)
    end
    
    Service->>WeChat: 检测当前窗口
    WeChat-->>Service: 返回窗口状态
    
    alt 在聊天列表界面
        Service->>WeChat: 查找目标联系人
        Service-->>Service: 等待加载(0.5s)
        Service->>WeChat: 点击目标联系人
        Service-->>Service: 等待聊天窗口打开(2s)
    end
    
    Service->>WeChat: 检测聊天窗口
    WeChat-->>Service: 返回聊天窗口状态
    
    loop 最多3次重试
        Service->>WeChat: 查找输入框
        alt 找到输入框
            Service->>WeChat: 聚焦输入框
            Service->>WeChat: 设置文本内容
            Service-->>Service: 等待输入完成(1s)
            break
        else 未找到
            Service-->>Service: 等待重试(1s)
        end
    end
    
    loop 最多3次重试
        Service->>WeChat: 查找发送按钮
        alt 找到发送按钮
            Service->>WeChat: 点击发送按钮
            Service-->>Service: 等待发送完成(1.5s)
            break
        else 未找到
            Service-->>Service: 等待重试(1s)
        end
    end
    
    Service->>WeChat: 验证消息发送成功
    WeChat-->>Service: 返回验证结果
    
    alt 发送成功
        Service->>User: 返回成功状态
    else 发送失败
        Service->>User: 返回错误信息
    end
    
    deactivate Service 