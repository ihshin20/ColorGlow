package com.example.mycolor.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.mycolor.signup.SignUpActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase




class LoginActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null

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
0
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

}
