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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.StorageReference
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
        val resultTextView = findViewById<TextView>(R.id.detailResultTextView)
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        val infoTextView = findViewById<TextView>(R.id.infoTextView)
        val similarTextView = findViewById<TextView>(R.id.similarTextView)

        val baseProductTextViews = listOf(
            findViewById<TextView>(R.id.baseproductTextView_1),
            findViewById<TextView>(R.id.baseproductTextView_2),
            findViewById<TextView>(R.id.baseproductTextView_3)
        )
        val lipProductTextViews = listOf(
            findViewById<TextView>(R.id.lipproductTextView_1),
            findViewById<TextView>(R.id.lipproductTextView_2),
            findViewById<TextView>(R.id.lipproductTextView_3)
        )
        val eyeProductTextViews = listOf(
            findViewById<TextView>(R.id.eyeproductTextView_1),
            findViewById<TextView>(R.id.eyeproductTextView_2),
            findViewById<TextView>(R.id.eyeproductTextView_3)
        )


        val imageViewBest = findViewById<ImageView>(R.id.imageViewBest)
        val imageViewWorst = findViewById<ImageView>(R.id.imageViewWorst)
        val myImg: ImageView = findViewById(R.id.myImg)

        val result = intent.getStringExtra("result")
        var uid = intent.getStringExtra("uid")
        val flag = intent.getIntExtra("flag", 0)
        val imagePath = intent.getStringExtra("imgPath")

        // Intent에서 타임스탬프 값 가져오기
        val defaultTime = -1L // 기본값 설정
        val timestamp = intent.getLongExtra("timestamp", defaultTime)
        val receivedDate = Date(timestamp)

        val imageBitmap = BitmapFactory.decodeFile(imagePath)


        val storage = FirebaseStorage.getInstance("gs://colorglow-9e76e.appspot.com")
        val storageRef = storage.reference

        val resultLower = result?.lowercase(Locale.ROOT) ?: "default_value"
        val bestImagePath = "${resultLower}_best.jpg"
        val worstImagePath = "${resultLower}_worst.jpg"
        val bestImageRef = storageRef.child(bestImagePath)
        val worstImageRef = storageRef.child(worstImagePath)

        //베스트 팔레트,워스트 팔레트 이미지 불러오기
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


        if (flag == 1) {

            myImg.setImageBitmap(imageBitmap)

            fetchNowResult(
                uid, result, dateTextView, resultTextView, infoTextView, similarTextView,
                baseProductTextViews, lipProductTextViews, eyeProductTextViews, imageBitmap!!
            )
        } else {
            val dateFromIntent = intent.getStringExtra("date")
            val user = FirebaseAuth.getInstance().currentUser
            uid = user?.uid

            val date2String = receivedDate.toString()
            val storagePath = "UserImages/$uid/${date2String}.jpg"

            // Glide를 사용하여 ImageView에 이미지 로드
            FirebaseStorage.getInstance().reference.child(storagePath).downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(myImg)

                Log.d("FirebaseStorage", "Download URL: $uri")

            }.addOnFailureListener { exception ->
                // 이미지 로드 실패 처리

                Log.e("FirebaseStorage", "Error loading image", exception)
                myImg.setImageResource(R.drawable.joy_redvelvet) // 예를 들어 default_image
            }



            if (dateFromIntent != null) {
                if (result != null) {
                    fetchRecentResult(
                        uid,
                        receivedDate,
                        result,
                        myImg,
                        dateTextView,
                        resultTextView,
                        infoTextView,
                        similarTextView//,
                        //productTextView
                    )
                }
            } else {
                Log.w("IntentError", "Date information is missing in the intent")
            }
        }
    }

    fun fetchNowResult(
        uid: String?,
        result: String?,
        dateTextView: TextView,
        resultTextView: TextView,
        infoTextView: TextView,
        similarTextView: TextView,
        baseProductTextViews: List<TextView>,
        lipProductTextViews: List<TextView>,
        eyeProductTextViews: List<TextView>,
        myImg: Bitmap
    ) {
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

        fun updateProductInfo(productsMap: Map<String, Any>, productTextViews: List<TextView>) {
            productsMap.entries.forEachIndexed { index, entry ->
                val productInfo = entry.value as? Map<String, Any> ?: mapOf()
                val productName = productInfo["제품이름"] as? String ?: "제품명 정보 없음"
                val productBrand = productInfo["브랜드"] as? String ?: "브랜드 정보 없음"
                val productPrice = productInfo["가격"] as? String ?: "가격 정보 없음"

                if (index < productTextViews.size) {
                    productTextViews[index].text = "$productName\n$productBrand\n$productPrice"
                }
            }
        }

        fun getCategoryData(productDescriptions: List<Map<String, Any>>, category: String) =
            productDescriptions.find { it.containsKey(category) }
                ?.get(category) as? Map<String, Any> ?: mapOf()


        // Tone 정보 가져오기
        toneRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val description = document.getString("설명") ?: "Description not available"
                    val celebrities = document.get("비슷한 연예인") as? List<String> ?: listOf("No celebrities available")

                    // '제품설명' 배열에서 데이터 추출
                    val productDescriptions = document.get("제품설명") as? List<Map<String, Any>> ?: listOf()

                    // '베이스' 카테고리의 데이터를 찾습니다.
                    val baseMap = productDescriptions.find { it.containsKey("베이스") }?.get("베이스") as? Map<String, Any> ?: mapOf()

                    // '베이스' 카테고리의 각 제품 정보를 TextView에 표시
                    baseMap.let { products ->
                        baseProductTextViews.forEachIndexed { index, textView ->
                            val productKey = "베이스제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>

                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"

                            // 각 TextView에 설정할 텍스트를 생성합니다.
                            textView.text = "$productName\n$productBrand\n$productPrice"
                        }
                    }

                    // '립' 카테고리의 데이터를 찾습니다.
                    val lipMap = productDescriptions.find { it.containsKey("립") }?.get("립") as? Map<String, Any> ?: mapOf()

                    // '립' 카테고리의 각 제품 정보를 TextView에 표시
                    lipMap.let { products ->
                        lipProductTextViews.forEachIndexed { index, textView ->
                            val productKey = "립제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>

                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"

                            // 각 TextView에 설정할 텍스트를 생성합니다.
                            textView.text = "$productName\n$productBrand\n$productPrice"
                        }
                    }

                    // '아이' 카테고리의 데이터를 찾습니다.
                    val eyeMap = productDescriptions.find { it.containsKey("아이") }?.get("아이") as? Map<String, Any> ?: mapOf()

                    // '아이' 카테고리의 각 제품 정보를 TextView에 표시
                    eyeMap.let { products ->
                        eyeProductTextViews.forEachIndexed { index, textView ->
                            val productKey = "아이제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>

                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"

                            // 각 TextView에 설정할 텍스트를 생성합니다.
                            textView.text = "$productName\n$productBrand\n$productPrice"
                        }
                    }

                    // UI에 데이터를 표시합니다.
                    infoTextView.text = description
                    similarTextView.text = celebrities.joinToString(", ")

                } else {
                    Log.d("Firestore", "No Tone document found")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting Tone document: ", exception)
            }
    }

    // 카테고리별 이미지 불러오기
    private fun loadImages(storageRef: StorageReference, categoryPrefix: String, category: String, imageViews: List<ImageView>) {
        imageViews.forEachIndexed { index, imageView ->
            val imagePath = "products/$categoryPrefix/${categoryPrefix}_${category}_${index + 1}.jpg"
            val imageRef = storageRef.child(imagePath)
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this).load(uri).into(imageView)
            }.addOnFailureListener { exception ->
                Log.e("Storage", "Error loading image: $imagePath", exception)
            }
        }
    }
}


fun fetchRecentResult(uid: String?, date: Date, result:String, myImg: ImageView, dateTextView: TextView, resultTextView: TextView,
                      infoTextView: TextView, similarTextView: TextView) {
    if (uid == null || date == null) {
        Log.w("Firestore", "UID or Date is null")
        dateTextView.text = "UID or Date is missing"
        return
    }

    val db = FirebaseFirestore.getInstance()
    val diagnosticRef = db.collection("User").document(uid).collection("results")
    val toneRef = db.collection("Tone").document(result)

    diagnosticRef.whereEqualTo("date", date)
        .get()
        .addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                println("No documents found with the specified timestamp")
            } else {
                for (document in documents) {
                    val result = document.getString("result") ?: "No result available"
                    resultTextView.text = result

                    val formattedDate = date.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                    } ?: "No date available"
                    dateTextView.text = formattedDate

                }
            }
        }
        .addOnFailureListener { exception ->
            println("Error getting documents: $exception")
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