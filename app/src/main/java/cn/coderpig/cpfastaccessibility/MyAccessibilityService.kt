package cn.coderpig.cpfastaccessibility
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityNodeInfo
import cn.coderpig.cp_fast_accessibility.AnalyzeSourceResult
import cn.coderpig.cp_fast_accessibility.EventWrapper
import cn.coderpig.cp_fast_accessibility.FastAccessibilityService
import cn.coderpig.cp_fast_accessibility.click
import cn.coderpig.cp_fast_accessibility.findNodeByExpression
import cn.coderpig.cp_fast_accessibility.findNodeById
import cn.coderpig.cp_fast_accessibility.findNodesById
import cn.coderpig.cp_fast_accessibility.findNodesByIdAndClassName
import cn.coderpig.cp_fast_accessibility.input
import cn.coderpig.cp_fast_accessibility.sleep
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
        if (wrapper?.packageName == "com.tencent.mm" && wrapper.className=="com.tencent.mm.ui.LauncherUI") {
            var inputNode = result?.findNodesByIdAndClassName("com.tencent.mm:id/bkk", "android.widget.EditText")
            if (inputNode !=null) {
                inputNode.click();
                sleep(400);
                inputNode.input("公告，机器人发送");
                sleep(400);
                var sentButton=  result?.findNodesByIdAndClassName("com.tencent.mm:id/bql","android.widget.Button");
                if (sentButton != null) {
                    sentButton.click(false)
                }
            }
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