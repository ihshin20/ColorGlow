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
import com.google.firebase.Timestamp
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

        val uidTextView = findViewById<TextView>(R.id.detailUidTextView)
        val resultTextView = findViewById<TextView>(R.id.detailResultTextView)
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        val infoTextView = findViewById<TextView>(R.id.infoTextView)
        val similarTextView = findViewById<TextView>(R.id.similarTextView)
        val productTextView = findViewById<TextView>(R.id.productTextView)
        val imageViewBest = findViewById<ImageView>(R.id.imageViewBest)
        val imageViewWorst = findViewById<ImageView>(R.id.imageViewWorst)
        val myImg: ImageView = findViewById(R.id.myImg)

        val result = intent.getStringExtra("result")
        val uid = intent.getStringExtra("uid")
        val flag = intent.getIntExtra("flag", 0)
        val imagePath = intent.getStringExtra("imgPath")
        val imageBitmap = BitmapFactory.decodeFile(imagePath)
        myImg.setImageBitmap(imageBitmap)

        val storage = FirebaseStorage.getInstance("gs://colorglow-9e76e.appspot.com")
        val storageRef = storage.reference

        val resultLower = result?.lowercase(Locale.ROOT) ?: "default_value"
        val bestImagePath = "${resultLower}_best.jpg"
        val worstImagePath = "${resultLower}_worst.jpg"
        val bestImageRef = storageRef.child(bestImagePath)
        val worstImageRef = storageRef.child(worstImagePath)

        bestImageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this)
                .load(uri)
                .into(imageViewBest)
        }.addOnFailureListener { exception ->
            Log.w("Storage", "Failed to load best image", exception)
        }

        worstImageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this)
                .load(uri)
                .into(imageViewWorst)
        }.addOnFailureListener { exception ->
            Log.w("Storage", "Failed to load worst image", exception)
        }

        uidTextView.text = uid

        if (flag == 1) {
            fetchNowResult(
                uid,
                result,
                dateTextView,
                resultTextView,
                infoTextView,
                similarTextView,
                productTextView,
                imageBitmap!!
            )
        } else {
            val dateFromIntent = intent.getStringExtra("date")
            if (dateFromIntent != null) {
                fetchRecentResult(
                    uid,
                    dateFromIntent,
                    dateTextView,
                    resultTextView,
                    infoTextView,
                    similarTextView,
                    productTextView
                )
            } else {
                Log.w("IntentError", "Date information is missing in the intent")
                // 필요한 경우 사용자에게 날짜 정보가 누락되었다는 것을 알리는 UI 업데이트를 수행
            }
        }
    }
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


fun fetchRecentResult(uid: String?, date: String?, dateTextView: TextView, resultTextView: TextView,
                      infoTextView: TextView, similarTextView: TextView, productTextView: TextView) {
    if (uid == null || date == null) {
        Log.w("Firestore", "UID or Date is null")
        dateTextView.text = "UID or Date is missing"
        return
    }

    val db = FirebaseFirestore.getInstance()
    val diagnosticRef = db.collection("User").document(uid).collection("results")

    // 날짜 형식 파싱
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val targetDate = sdf.parse(date)?.let {
        Timestamp(Date(it.time / 1000 * 1000)) // 밀리초 단위의 오차를 제거하기 위해 초 단위로 변환 후 다시 밀리초로 변환
    }

    diagnosticRef.whereEqualTo("date", targetDate).get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Log.d("Firestore", "No documents found for the specified date")
                dateTextView.text = "No results found for $date"
                resultTextView.text = "No results found"
                // 다른 TextView들도 업데이트
                infoTextView.text = ""
                similarTextView.text = ""
                productTextView.text = ""
            } else {
                // 여러 결과 중 첫 번째 결과만 표시
                val document = documents.documents.first() // 날짜가 같은 첫 번째 문서 가져오기
                val actualDate = document.getTimestamp("date")?.toDate()
                val formattedDate = actualDate?.let { sdf.format(it) } ?: "No date available"

                val result = document.getString("result") ?: "No result available"

                // TextView 업데이트
                dateTextView.text = formattedDate
                resultTextView.text = result

                // Tone 정보 가져오기
                val toneRef = db.collection("Tone").document(result)
                toneRef.get()
                    .addOnSuccessListener { toneDocument ->
                        if (toneDocument.exists()) {
                            val description = toneDocument.getString("description") ?: "Description not available"
                            val celebrities = toneDocument.get("similarCelebrities") as? List<String> ?: listOf("No celebrities available")
                            val celebrityText = celebrities.joinToString(", ")
                            val products = toneDocument.get("productDescription") as? List<String> ?: listOf("No products available")
                            val productText = products.joinToString("\n")

                            // TextView 업데이트
                            infoTextView.text = description
                            similarTextView.text = celebrityText
                            productTextView.text = productText
                        } else {
                            Log.d("Firestore", "No Tone document found")
                            infoTextView.text = "No description available"
                            similarTextView.text = ""
                            productTextView.text = ""
                        }
                    }
                    .addOnFailureListener { toneException ->
                        Log.w("Firestore", "Error getting Tone document: ", toneException)
                        infoTextView.text = "Failed to fetch description"
                    }
            }
        }
        .addOnFailureListener { diagnosticException ->
            Log.w("Firestore", "Error getting diagnostic results: ", diagnosticException)
            dateTextView.text = "Failed to fetch results"
            resultTextView.text = "Failed to fetch results"
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



