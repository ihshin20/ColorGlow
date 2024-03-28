package com.example.mycolor.signup

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
                Toast.makeText(this, "정보를 모두 입력하세요", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Log.d("MAIN", email.text.toString())
            Log.d("MAIN", password.text.toString())

            auth.createUserWithEmailAndPassword(email.text.toString(), password.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(this, "성공", Toast.LENGTH_LONG).show()
                    } else {
                        // If sign in fails, display a message to the user.
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "사용가능한 아이디입니다", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "실패", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }

        val logoutBtn = findViewById<Button>(R.id.button)
        logoutBtn.setOnClickListener{
            Firebase.auth.signOut()
            Toast.makeText(this,"로그인 완료", Toast.LENGTH_LONG).show()
        }
    }
}