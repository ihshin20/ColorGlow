package com.example.mycolor.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import com.example.mycolor.R
import com.example.mycolor.login.LoginActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = Runnable {
        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_splash)

        handler.postDelayed(runnable, DURATION)
    }

    companion object {
        private const val DURATION: Long = 2000 // 3초 후 LoginActivity로 이동
    }

    override fun onBackPressed() {
        // 스플래시 화면에서 뒤로 가기 버튼을 눌렀을 때 아무 작업도 하지 않음
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable) // 메모리 누수 방지
        super.onDestroy()
    }
}