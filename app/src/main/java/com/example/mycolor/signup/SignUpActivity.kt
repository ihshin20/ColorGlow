package com.example.mycolor.signup

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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mycolor.R
import com.example.mycolor.activity.NaviActivity
import com.example.mycolor.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.FirebaseAuthLegacyRegistrar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        auth = Firebase.auth

        val joinBtn = findViewById<Button>(R.id.button) // 가입 버튼 ID 수정
        joinBtn.setOnClickListener {
            val email = findViewById<EditText>(R.id.editTextText) // EditText로 변경하고 ID 수정
            val password = findViewById<EditText>(R.id.editTextTextPassword)

            if (email.text.toString().isEmpty() || password.text.toString().isEmpty()) {
                Toast.makeText(this, "정보를 모두 입력하세요.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

//            Log.d("MAIN", email.text.toString())
//            Log.d("MAIN", password.text.toString())

            auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(this, "성공", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()

                    } else {
                        // 아이디 중복될 경우
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "이미 등록된 아이디입니다.", Toast.LENGTH_LONG).show()
                        } else {
                            // 양식(이메일 아이디, 6자리 이상의 비밀번호) 미충족 시
                            Toast.makeText(this, "아이디와 비밀번호 양식을 확인하세요.", Toast.LENGTH_LONG).show()
                            Log.d("MAIN", task.exception.toString())
                        }
                    }
                }
        }

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