package com.example.mycolor.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mycolor.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import java.io.ByteArrayOutputStream


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

        val imageView = findViewById<ImageView>(R.id.logoimageView1)
        imageView.setImageResource(R.drawable.logoimage)
        val uidTextView = findViewById<TextView>(R.id.detailUidTextView)
        val resultTextView = findViewById<TextView>(R.id.detailResultTextView)
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        val infoTextView = findViewById<TextView>(R.id.infoTextView)

        val similarTextView = findViewById<TextView>(R.id.similarTextView)
        val productTextview = findViewById<TextView>(R.id.productTextView)

        val imageViewBest = findViewById<ImageView>(R.id.imageViewBest)
        val imageViewWorst = findViewById<ImageView>(R.id.imageViewWorst)

        val myImg: ImageView = findViewById(R.id.myImg)


        val result = intent.getStringExtra("result")
        val uid = intent.getStringExtra("uid")
        val flag = intent.getIntExtra("flag", 0)

//        val imageBitmap = intent.extras?.getParcelable("imgPath") as Bitmap?
//        imageBitmap?.let {
//            myImg.setImageBitmap(it)
//        }

        val imagePath = intent.getStringExtra("imgPath")
        val imageBitmap = BitmapFactory.decodeFile(imagePath)
        myImg.setImageBitmap(imageBitmap)

        val storage = FirebaseStorage.getInstance("gs://colorglow-9e76e.appspot.com") // 스토리지 주소 설정
        val storageRef = storage.reference

        // 파일 경로 수정 (결과 이름을 기반으로 동적으로 생성)
        val resultLower = result?.lowercase(Locale.ROOT) ?: "default_value" // 결과 이름을 소문자로 변환
        val bestImagePath = "${resultLower}_best.jpg"
        val worstImagePath = "${resultLower}_worst.jpg"

        val bestImageRef = storageRef.child(bestImagePath)
        val worstImageRef = storageRef.child(worstImagePath)

        bestImageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this)
                .load(uri)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("GlideError", "Error loading image", e)
                        return false // false를 반환하면 Glide는 에러 플레이스홀더나 에러 이미지를 처리하지 않습니다.
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(imageViewBest)
        }.addOnFailureListener { exception ->
            Log.w("Storage", "Failed to load best image", exception)
        }

        worstImageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this)
                .load(uri)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("GlideError", "Error loading image", e)
                        return false // false를 반환하면 Glide는 에러 플레이스홀더나 에러 이미지를 처리하지 않습니다.
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }
                })
                .into(imageViewWorst)
        }.addOnFailureListener { exception ->
            Log.w("Storage", "Failed to load worst image", exception)
        }
        //resultTextView.text = result
        uidTextView.text = uid

        if(flag == 1){
            fetchNowResult(uid, result, dateTextView, resultTextView, infoTextView, similarTextView, productTextview, imageBitmap!!)
        }
        // else{} -> 방금 진단한거 아닌거 (result fragment에서 내 과거 진단 조회 시 실행할 부분 작성해야 함

        //fetchRecentResult(uid, dateTextView, resultTextView, infoTextView, similarTextView)

    }

    fun fetchNowResult(uid: String?, result: String?, dateTextView: TextView, resultTextView: TextView,
                       infoTextView: TextView, similarTextView: TextView, productTextView: TextView, myImg: Bitmap) {
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

                        uploadImageToFirestore(myImg, date.toString(), uid)

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

                    val words = description.split(" ")
                    val maxLineLength = 40  // 최대 줄 길이 설정
                    val stringBuilder = StringBuilder()
                    var currentLineLength = 0

                    for (word in words) {
                        // 단어 추가 전, 현재 줄의 길이와 단어 길이를 합쳐서 최대 길이를 초과하는지 확인
                        if (currentLineLength + word.length + 1 > maxLineLength) { // +1 for space
                            stringBuilder.append("\n")
                            currentLineLength = 0  // 줄 길이 리셋
                        }

                        stringBuilder.append(word).append(" ")
                        currentLineLength += word.length + 1

                        // 마침표 뒤에 줄바꿈 추가 (단어 처리 후에 적용)
                        if (word.endsWith(".")) {
                            stringBuilder.append("\n")
                            currentLineLength = 0
                        }
                    }

                    val processedText = stringBuilder.toString()

                    val celebrities = document.get("비슷한 연예인") as? List<String> ?: listOf("No celebrities available")
                    val celebrityText = celebrities.joinToString(", ")
                    val products = document.get("제품설명") as? List<String> ?: listOf("No products")
                    val productText = products.joinToString("\n")

                    // TextView 업데이트
                    infoTextView.text = processedText.trim()
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



    private fun uploadImageToFirestore(imageBitmap: Bitmap, uDate: String, uid:String) {
        // Firestore에 이미지를 저장하기 위해 ByteArrayOutputStream 사용
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        // Firebase Storage에 먼저 이미지를 업로드하고 URL을 받아 Firestore에 저장
        val storageRef = FirebaseStorage.getInstance().reference.child("UserImages/${uid}/${uDate}.jpg")
        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                saveImageUrlToFirestore(imageUrl)
            }
        }.addOnFailureListener {
            // 처리할 오류 로직 추가
        }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val imageInfo = hashMapOf("url" to imageUrl)

        db.collection("images").add(imageInfo)
            .addOnSuccessListener { documentReference ->
                // 데이터 저장 성공 시 처리
            }
            .addOnFailureListener { e ->
                // 데이터 저장 실패 시 처리
            }
    }


}