package com.example.mycolor.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mycolor.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class WriteActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val seasonSpinner = findViewById<Spinner>(R.id.seasonSpinner)
        val toneRadioGroup = findViewById<RadioGroup>(R.id.toneRadioGroup)
        val writeTitleTextView = findViewById<EditText>(R.id.writeTitleTextView)
        val writeBodyTextView = findViewById<EditText>(R.id.writeBodyTextView)
        val saveBtn = findViewById<Button>(R.id.saveBtn)

        val content = intent.getStringExtra("postContent")

        // 계절 데이터 설정
        val seasons = arrayOf("Spring", "Summer", "Autumn", "Winter")
        val seasonAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seasons)
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        seasonSpinner.adapter = seasonAdapter

        // 계절별 세부 톤 매핑
        val toneMap = mapOf(
            "Spring" to arrayOf("Bright Spring", "Light Spring", "True Spring"),
            "Summer" to arrayOf("Light Summer", "Soft Summer", "True Summer"),
            "Autumn" to arrayOf("Dark Autumn", "Soft Autumn", "True Autumn"),
            "Winter" to arrayOf("Dark Winter", "Bright Winter", "True Winter")
        )

        // 스피너 선택 변경 이벤트 처리
        seasonSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                toneRadioGroup.removeAllViews()  // 라디오 그룹 초기화
                toneMap[seasons[position]]?.forEach { tone ->
                    val radioButton = RadioButton(this@WriteActivity)
                    radioButton.text = tone
                    toneRadioGroup.addView(radioButton)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {
                toneRadioGroup.removeAllViews()  // 선택 해제 시 라디오 버튼 제거
            }
        }

        saveBtn.setOnClickListener {
            val title = writeTitleTextView.text.toString().trim()
            val body = writeBodyTextView.text.toString().trim()
            val selectedToneRadioButtonId = toneRadioGroup.checkedRadioButtonId

            if (title.isEmpty() || body.isEmpty() || selectedToneRadioButtonId == -1) {
                Toast.makeText(this, "모든 필드를 채워주세요.", Toast.LENGTH_SHORT).show()
            } else {
                val selectedRadioButton = findViewById<RadioButton>(selectedToneRadioButtonId)
                val tone = selectedRadioButton.text.toString()
                val uid = auth.currentUser?.uid ?: "unknown"
                val post = hashMapOf(
                    "Body" to body,
                    "Date" to Timestamp.now(),
                    "Like" to 0,
                    "Title" to title,
                    "Tone" to tone,
                    "UID" to uid
                )

                db.collection("Posts")
                    .add(post)
                    .addOnSuccessListener {
                        Toast.makeText(this, "게시글이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        finish()  // 액티비티 종료
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "저장에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
