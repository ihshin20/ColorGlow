package com.example.mycolor.activity

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.mycolor.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import java.util.*

class WriteActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private lateinit var loadingDialog: Dialog

    private lateinit var attachImageButton: Button
    private lateinit var imageViewPreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupLoadingDialog()

        val seasonSpinner = findViewById<Spinner>(R.id.seasonSpinner)
        val toneRadioGroup = findViewById<RadioGroup>(R.id.toneRadioGroup)
        val writeTitleTextView = findViewById<EditText>(R.id.writeTitleTextView)
        val writeBodyTextView = findViewById<EditText>(R.id.writeBodyTextView)
        val saveBtn = findViewById<Button>(R.id.saveBtn)
        attachImageButton = findViewById(R.id.attachImageButton)
        imageViewPreview = findViewById(R.id.imageViewPreview)

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

        attachImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startForResult.launch(intent)
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

                showLoadingDialog()

                db.collection("Posts")
                    .add(post)
                    .addOnSuccessListener { documentReference ->
                        val postId = documentReference.id
                        if (selectedImageUri != null) {
                            uploadImageToStorage(postId, selectedImageUri!!) { imageUrl ->
                                db.collection("Posts").document(postId)
                                    .update("ImageUrl", imageUrl)
                                    .addOnSuccessListener {
                                        hideLoadingDialog()
                                        Toast.makeText(this, "게시글이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                        finish()  // 액티비티 종료
                                    }
                                    .addOnFailureListener { e ->
                                        hideLoadingDialog()
                                        Toast.makeText(this, "이미지 URL 업데이트에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            hideLoadingDialog()
                            Toast.makeText(this, "게시글이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                            finish()  // 액티비티 종료
                        }
                    }
                    .addOnFailureListener { e ->
                        hideLoadingDialog()
                        Toast.makeText(this, "저장에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        // 루트 레이아웃을 터치하면 키보드를 숨기도록 설정
        findViewById<View>(R.id.detailCommunityLayout).setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }
    }

    private fun setupLoadingDialog() {
        loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.loading_dialog)
        loadingDialog.setCancelable(false)
    }

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)

        val view = inflater.inflate(R.layout.loading_dialog, null)
        val textView = view.findViewById<TextView>(R.id.loadingProgressTextView)
        textView.text = "잠시만 기다려주세요."

        builder.setView(view)
        builder.setCancelable(false)

        loadingDialog = builder.create()
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog.dismiss()
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data
            imageViewPreview.setImageURI(selectedImageUri)
            imageViewPreview.visibility = android.view.View.VISIBLE
        }
    }

    private fun uploadImageToStorage(postId: String, imageUri: Uri, onSuccess: (String) -> Unit) {
        val storageRef = storage.reference.child("PostImages/$postId.jpg")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                hideLoadingDialog()
                Toast.makeText(this, "이미지 업로드에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }
}
