package cn.coderpig.cpfastaccessibility

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import cn.coderpig.cp_fast_accessibility.FastAccessibilityService
import cn.coderpig.cp_fast_accessibility.isAccessibilityEnable
import cn.coderpig.cp_fast_accessibility.requireAccessibility

class MainActivity : AppCompatActivity() {
    private lateinit var mServiceStatusIv: ImageView
    private lateinit var mServiceStatusTv: TextView
    private lateinit var mOpenTargetAppBt: Button
    private lateinit var callBackEditText: EditText
    private lateinit var callBackSaveButton: Button

    private val mClickListener = View.OnClickListener {
        when (it.id) {
            R.id.iv_service_status -> {
                if (isAccessibilityEnable) shortToast(getStringRes(R.string.service_is_enable_tips))
                else requireAccessibility()
            }
            R.id.bt_open_target_app -> startApp("com.tencent.mm", "com.tencent.mm.ui.LauncherUI", "未安装微信")
        }
    }

    private val callBackSaveClickListener = View.OnClickListener {
        when (it.id) {
            R.id.btn_confirm -> {
                val text = callBackEditText.text.toString()
                if (text.isNotEmpty()) {
                    Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "EditText is empty!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        mServiceStatusIv = findViewById(R.id.iv_service_status)
        mServiceStatusTv = findViewById(R.id.tv_service_status)
        mOpenTargetAppBt = findViewById(R.id.bt_open_target_app)

        mServiceStatusIv.setOnClickListener(mClickListener)
        mOpenTargetAppBt.setOnClickListener(mClickListener)

        callBackEditText = findViewById(R.id.et_input)
        callBackSaveButton = findViewById(R.id.btn_confirm)
        callBackSaveButton.setOnClickListener(callBackSaveClickListener)
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityEnable) {
            mServiceStatusIv.setImageDrawable(getDrawableRes(R.drawable.ic_service_enable))
            mServiceStatusTv.text = getStringRes(R.string.service_status_enable)
            mOpenTargetAppBt.visibility = View.VISIBLE
        } else {
            mServiceStatusIv.setImageDrawable(getDrawableRes(R.drawable.ic_service_disable))
            mServiceStatusTv.text = getStringRes(R.string.service_status_disable)
            mOpenTargetAppBt.visibility = View.GONE
        };
    }
}