package com.example.mycolor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class CommunityActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community)

        // activity_community 레이아웃에서 TextView를 찾습니다.
        val textView: TextView = findViewById(R.id.communityTextView)
        textView.text = "Welcome to Community Activity"
    }

}
