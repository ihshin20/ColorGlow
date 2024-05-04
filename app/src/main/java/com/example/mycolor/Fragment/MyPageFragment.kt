package com.example.mycolor.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.mycolor.R
import com.example.mycolor.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Locale

class MyPageFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    lateinit var uid:String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        val nameTextView = view.findViewById<TextView>(R.id.customerName)
        // ImageView 로컬 변수에 할당
        val logoimageView = view.findViewById<ImageView>(R.id.logoimageView)
        logoimageView.setImageResource(R.drawable.logoimage)

        val auth = FirebaseAuth.getInstance()

        // 현재 로그인한 사용자 정보 가져오기
        val user = auth.currentUser
        if (user != null) {
            // 사용자 UID 출력
            uid = user.uid
            Log.d("FirebaseAuth", "User UID: $uid")
            // 여기서 uid를 사용할 수 있습니다.
        } else {
            Log.d("FirebaseAuth", "No user is currently signed in.")
        }

        val db = FirebaseFirestore.getInstance()

        // User 컬렉션에서 해당 uid의 문서를 참조합니다.
        val userRef = db.collection("User").document(uid)

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    // 문서가 존재하고, name 필드에 값이 있을 경우 그 값을 가져옴
                    val name = document.getString("name") ?: "상상부기" // name 필드가 null이면 "상상부기"를 사용
                    nameTextView.text = name
                    Log.d("Firestore", "Name: $name")
                } else {
                    // 문서가 존재하지 않을 경우, 기본값을 설정
                    Log.d("Firestore", "No such document, setting default name to '상상부기'")
                    // 필요한 경우 여기서 기본값을 문서에 설정할 수 있습니다.
                }
            } else {
                Log.d("Firestore", "get failed with ", task.exception)
            }
        }

        // 로그아웃 버튼에 대한 참조 및 클릭 리스너 설정
        val logoutButton = view.findViewById<Button>(R.id.button3)
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(activity, "로그아웃", Toast.LENGTH_SHORT).show()

            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

        }
    }
}
