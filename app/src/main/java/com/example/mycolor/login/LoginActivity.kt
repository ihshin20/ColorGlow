
package com.example.mycolor.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mycolor.R
import com.example.mycolor.activity.NaviActivity
import com.example.mycolor.signup.SignUpActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase




class LoginActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null

    private var backPressedTime: Long = 0
    private lateinit var backToast: Toast

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = Firebase.auth
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button = findViewById<Button>(R.id.button) // 로그인 버튼
        val button2 = findViewById<Button>(R.id.button2) // 회원가입 버튼
        val editTextEmail = findViewById<EditText>(R.id.editTextText)
        val editTextPassword = findViewById<EditText>(R.id.editTextTextPassword)

        // 로그인 버튼 클릭 리스너
        button.setOnClickListener {
            signIn(editTextEmail.text.toString(), editTextPassword.text.toString())
        }

        // 회원가입 버튼 클릭 리스너
        button2.setOnClickListener {
            createAccount(editTextEmail.text.toString(), editTextPassword.text.toString())
        }

        // 백버튼 두 번 누르면 종료
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    backToast.cancel()
                    finish() // 앱 종료
                } else {
                    backToast = Toast.makeText(baseContext, "뒤로 가기 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                    backToast.show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)

    }

    private fun signIn(email: String, password: String) {
        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth?.signInWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // 로그인 성공
                        Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, NaviActivity::class.java)
                        startActivity(intent)

                        finish()
                        // 로그인 성공시 수행할 작업, 예를 들면 메인 화면으로 이동
                    } else {
                        // 로그인 실패
                        Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun createAccount(email: String, password: String) {
        val intent = Intent(this, SignUpActivity::class.java)
        startActivity(intent)
    }

    // 화면 터치시 키패드 숨기기
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view != null) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }
        return super.dispatchTouchEvent(event)
    }

}
