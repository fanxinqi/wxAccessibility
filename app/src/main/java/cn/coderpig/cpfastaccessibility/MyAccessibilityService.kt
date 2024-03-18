package cn.coderpig.cpfastaccessibility
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import cn.coderpig.cp_fast_accessibility.AnalyzeSourceResult
import cn.coderpig.cp_fast_accessibility.EventWrapper
import cn.coderpig.cp_fast_accessibility.FastAccessibilityService
import java.util.Timer
import java.util.TimerTask

/**
 * Author: 范新旗
 * Date: 2024-03-18
 * Desc:
 */
class MyAccessibilityService : FastAccessibilityService() {
    companion object {
        private const val TAG = "CpFastAccessibility"
        private var timer: Timer? = Timer()
        // 静态方法，用于模拟下拉刷新
        @JvmStatic
        fun performPullToRefresh() {
            val device = FastAccessibilityService.require

            // 获取屏幕尺寸
            val displayMetrics = device.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            // 计算下拉手势起始和结束位置
            val startX = screenWidth / 2
            val startY = screenHeight / 2
            val endX = startX
            val endY = startY + screenHeight / 4  // 下拉距离为屏幕高度的四分之一

            // 模拟手势下拉操作
            swipe(startX, startY, endX, endY, 500)
        }

        // 静态方法，用于执行手势滑动
        @JvmStatic
        fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long) {
            val path = Path()
            path.moveTo(startX.toFloat(), startY.toFloat())
            path.lineTo(endX.toFloat(), endY.toFloat())

            val gestureBuilder = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))

            FastAccessibilityService.require.dispatchGesture(gestureBuilder.build(), null, null)
        }
    }

    override val enableListenApp = true

    override fun analyzeCallBack(wrapper: EventWrapper?, result: AnalyzeSourceResult) {
        if (wrapper?.packageName == "com.tencent.mm" && wrapper?.className == "com.tencent.mm.plugin.webview.ui.tools.MMWebViewUI") {
            // 每隔一定时间执行任务
            // 每隔一定时间执行任务
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    // 在定时器中执行你的操作
                    performPullToRefresh()
                }
            }, 0, 5000) // 每隔5秒执行一次任务，第一次任务延迟0毫秒

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消定时器
        if (timer != null) {
//            timer.cancel()
            timer = null
        }
    }

}