package com.example.mycolor.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        val infoTextView2 = findViewById<TextView>(R.id.infoTextView2)
        val infoTextView3 = findViewById<TextView>(R.id.infoTextView3)
        val infoTextView4 = findViewById<TextView>(R.id.infoTextView4)
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
                uid, result, dateTextView, resultTextView,
                infoTextView, infoTextView2, infoTextView3, infoTextView4, similarTextView,
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



            if (dateFromIntent != null && result != null) {
                fetchRecentResult(uid, receivedDate, result, myImg, dateTextView, resultTextView,
                    infoTextView, infoTextView2, infoTextView3, infoTextView4, similarTextView,
                    baseProductTextViews, lipProductTextViews, eyeProductTextViews)
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
        infoTextView2: TextView,
        infoTextView3: TextView,
        infoTextView4: TextView,
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
                        val replacedResult = result.replace("_", " ")
                        resultTextView.text = replacedResult
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
                    val description2 = document.getString("설명2") ?: "Description not available"
                    val description3 = document.getString("설명3") ?: "Description not available"
                    val description4 = document.getString("설명4") ?: "Description not available"
                    val celebrities = document.get("비슷한 연예인") as? List<String>
                        ?: listOf("No celebrities available")
                    val productDescriptions =
                        document.get("제품설명") as? List<Map<String, Any>> ?: listOf()

                    updateProductInfo(
                        getCategoryData(productDescriptions, "베이스"),
                        baseProductTextViews
                    )
                    updateProductInfo(
                        getCategoryData(productDescriptions, "립"),
                        lipProductTextViews
                    )
                    updateProductInfo(
                        getCategoryData(productDescriptions, "아이"),
                        eyeProductTextViews
                    )

                    infoTextView.text = description
                    infoTextView2.text = description2
                    infoTextView3.text = description3
                    infoTextView4.text = description4
                    similarTextView.text = celebrities.joinToString(", ")
                } else {
                    Log.d("Firestore", "No Tone document found")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting Tone document: ", exception)
            }


        // Firebase Storage 초기화 및 참조 설정
        val storageRef = FirebaseStorage.getInstance().reference

        // 인텐트에서 result 값을 받아와서 소문자로 변환
        val resultLower = intent.getStringExtra("result")?.lowercase(Locale.ROOT) ?: "default_value"

        // 각 카테고리별 이미지뷰 배열
        val baseImageViews = listOf(
            findViewById<ImageView>(R.id.baseimageView_1),
            findViewById<ImageView>(R.id.baseimageView_2),
            findViewById<ImageView>(R.id.baseimageView_3)
        )
        val lipImageViews = listOf(
            findViewById<ImageView>(R.id.lipimageView_1),
            findViewById<ImageView>(R.id.lipimageView_2),
            findViewById<ImageView>(R.id.lipimageView_3)
        )
        val eyeImageViews = listOf(
            findViewById<ImageView>(R.id.eyeimageView_1),
            findViewById<ImageView>(R.id.eyeimageView_2),
            findViewById<ImageView>(R.id.eyeimageView_3)
        )

        // 이미지 로드
        loadImages(storageRef, resultLower, "base", baseImageViews)
        loadImages(storageRef, resultLower, "lip", lipImageViews)
        loadImages(storageRef, resultLower, "eye", eyeImageViews)
    }

    // 카테고리별 이미지 불러오기
    private fun loadImages(
        storageRef: StorageReference,
        categoryPrefix: String,
        category: String,
        imageViews: List<ImageView>
    ) {
        imageViews.forEachIndexed { index, imageView ->
            val imagePath =
                "products/$categoryPrefix/${categoryPrefix}_${category}_${index + 1}.jpg"
            val imageRef = storageRef.child(imagePath)
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this).load(uri).into(imageView)
            }.addOnFailureListener { exception ->
                Log.e("Storage", "Error loading image: $imagePath", exception)
            }
        }
    }

    private fun fetchRecentResult(
        uid: String?,
        date: Date,
        result: String,
        myImg: ImageView,
        dateTextView: TextView,
        resultTextView: TextView,
        infoTextView: TextView,
        infoTextView2: TextView,
        infoTextView3: TextView,
        infoTextView4: TextView,
        similarTextView: TextView,
        baseProductTextViews: List<TextView>,
        lipProductTextViews: List<TextView>,
        eyeProductTextViews: List<TextView>
    ) {
        if (uid == null || date == null) {
            Log.w("Firestore", "UID or Date is missing")
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
                        val replacedResult = result.replace("_", " ")
                        resultTextView.text = replacedResult
                        val formattedDate = date.let {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(it)
                        } ?: "No date available"
                        dateTextView.text = formattedDate

                        // Firebase Storage 초기화 및 참조 설정
                        val storageRef = FirebaseStorage.getInstance().reference

                        // 이 부분에서 이미지 뷰를 선언하고 이미지 로딩 함수 호출
                        val baseImageViews = listOf(
                            findViewById<ImageView>(R.id.baseimageView_1),
                            findViewById<ImageView>(R.id.baseimageView_2),
                            findViewById<ImageView>(R.id.baseimageView_3)
                        )
                        val lipImageViews = listOf(
                            findViewById<ImageView>(R.id.lipimageView_1),
                            findViewById<ImageView>(R.id.lipimageView_2),
                            findViewById<ImageView>(R.id.lipimageView_3)
                        )
                        val eyeImageViews = listOf(
                            findViewById<ImageView>(R.id.eyeimageView_1),
                            findViewById<ImageView>(R.id.eyeimageView_2),
                            findViewById<ImageView>(R.id.eyeimageView_3)
                        )

                        val resultLower = result.lowercase(Locale.ROOT)
                        loadImages(storageRef, resultLower, "base", baseImageViews)
                        loadImages(storageRef, resultLower, "lip", lipImageViews)
                        loadImages(storageRef, resultLower, "eye", eyeImageViews)
                    }
                }
            }
            .addOnFailureListener { exception ->
                println("Error getting documents: $exception")
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

        toneRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val description = document.getString("설명") ?: "Description not available"
                    val description2 = document.getString("설명2") ?: "Description not available"
                    val description3 = document.getString("설명3") ?: "Description not available"
                    val description4 = document.getString("설명4") ?: "Description not available"
                    val celebrities = document.get("비슷한 연예인") as? List<String>
                        ?: listOf("No celebrities available")
                    val productDescriptions =
                        document.get("제품설명") as? List<Map<String, Any>> ?: listOf()

                    updateProductInfo(
                        getCategoryData(productDescriptions, "베이스"),
                        baseProductTextViews
                    )
                    updateProductInfo(
                        getCategoryData(productDescriptions, "립"),
                        lipProductTextViews
                    )
                    updateProductInfo(
                        getCategoryData(productDescriptions, "아이"),
                        eyeProductTextViews
                    )

                    infoTextView.text = description
                    infoTextView.text = description
                    infoTextView2.text = description2
                    infoTextView3.text = description3
                    infoTextView4.text = description4
                    similarTextView.text = celebrities.joinToString(", ")
                } else {
                    Log.d("Firestore", "No Tone document found")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting Tone document: ", exception)
            }
    }

    private fun uploadImageToFirestore(imageBitmap: Bitmap, uDate: String, uid: String) {
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val storageRef =
            FirebaseStorage.getInstance().reference.child("UserImages/${uid}/${uDate}.jpg")
        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                saveImageUrlToFirestore(uri.toString())
            }
        }.addOnFailureListener {
            Log.e("FirestoreUpload", "Failed to upload image", it)  // 오류 로깅 추가
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