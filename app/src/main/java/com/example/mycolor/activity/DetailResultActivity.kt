package com.example.mycolor.activity

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mycolor.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class DetailResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail_result)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val uidTextView = findViewById<TextView>(R.id.detailUidTextView)
        val resultTextView = findViewById<TextView>(R.id.detailResultTextView)
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        val infoTextView = findViewById<TextView>(R.id.infoTextView)
        val similarTextView = findViewById<TextView>(R.id.similarTextView)
        val productTextview = findViewById<TextView>(R.id.productTextView)

        val result = intent.getStringExtra("result")
        val uid = intent.getStringExtra("uid")
        val flag = intent.getIntExtra("flag", 0)

        resultTextView.text = result
        uidTextView.text = uid

        if(flag == 1){
            fetchNowResult(uid, result, dateTextView, resultTextView, infoTextView, similarTextView, productTextview)
        }
        // else{} -> 방금 진단한거 아닌거 (result fragment에서 내 과거 진단 조회 시 실행할 부분 작성해야 함

        //fetchRecentResult(uid, dateTextView, resultTextView, infoTextView, similarTextView)

    }

    fun fetchNowResult(uid: String?, result: String?, dateTextView: TextView, resultTextView: TextView,
                       infoTextView: TextView, similarTextView: TextView, productTextView: TextView) {
        if (uid == null || result == null) {
            Log.w("Firestore", "UID or Result is null")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val diagnosticRef = db.collection("User").document(uid).collection("results")
        val toneRef = db.collection("Tone").document(result)

        // 최근 결과 가져오기
        diagnosticRef.orderBy("date", Query.Direction.DESCENDING).limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "No documents found")
                } else {
                    for (document in documents) {
                        val date = document.getDate("date")  // Firestore의 Timestamp를 Date 객체로 변환
                        val formattedDate = date?.let {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it)
                        } ?: "No date available"

                        // TextView 업데이트
                        dateTextView.text = formattedDate
                        resultTextView.text = result
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }

        // Tone 정보 가져오기
        toneRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val description = document.getString("설명") ?: "Description not available"
                    val celebrities = document.get("비슷한 연예인") as? List<String> ?: listOf("No celebrities available")
                    val celebrityText = celebrities.joinToString(", ")
                    val products = document.get("제품설명") as? List<String> ?: listOf("No products")
                    val productText = products.joinToString("\n")

                    // TextView 업데이트
                    infoTextView.text = description
                    similarTextView.text = celebrityText
                    productTextView.text = productText
                } else {
                    Log.d("Firestore", "No Tone document found")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting Tone document: ", exception)
            }
    }

}