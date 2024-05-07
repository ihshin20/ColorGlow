package com.example.mycolor.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mycolor.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Locale

class MyPageFragment : Fragment() {

    private lateinit var uid: String

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var baseProductTextViews: List<TextView>
    private lateinit var lipProductTextViews: List<TextView>
    private lateinit var eyeProductTextViews: List<TextView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Toast.makeText(context, "뷰 생성 중...", Toast.LENGTH_SHORT).show()
        return inflater.inflate(R.layout.fragment_my_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Toast.makeText(context, "뷰 생성 완료", Toast.LENGTH_SHORT).show()

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        baseProductTextViews = listOf(
            view.findViewById(R.id.baseproductTextView_11),
            view.findViewById(R.id.baseproductTextView_22),
            view.findViewById(R.id.baseproductTextView_33)
        )
        lipProductTextViews = listOf(
            view.findViewById(R.id.lipproductTextView_11),
            view.findViewById(R.id.lipproductTextView_22),
            view.findViewById(R.id.lipproductTextView_33)
        )
        eyeProductTextViews = listOf(
            view.findViewById(R.id.eyeproductTextView_11),
            view.findViewById(R.id.eyeproductTextView_22),
            view.findViewById(R.id.eyeproductTextView_33)
        )

        val auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user != null) {
            uid = user.uid
            Log.d("FirebaseAuth", "User UID: $uid")
        } else {
            Log.d("FirebaseAuth", "No user is currently signed in.")
        }

        //기존코드
        val logoimageView = view.findViewById<ImageView>(R.id.logoimageView)
        logoimageView.setImageResource(R.drawable.logoimage)



        val logoutButton = view.findViewById<Button>(R.id.button3)
        logoutButton.setOnClickListener {
            Toast.makeText(activity, "앱을 종료합니다.", Toast.LENGTH_SHORT).show()
            logoutButton.postDelayed({
                activity?.finish()
            }, 1000)
        }

        fetchUserUid(uid) { result ->
            if (result.startsWith("Error") || result == "No documents found") {
                Toast.makeText(context, "Failed to load data: $result", Toast.LENGTH_LONG).show()
            } else {
                updateProductDetails(result)
            }
        }


        fetchPersonalColorInfo()
    }

    private fun fetchPersonalColorInfo() {
        val uid = firebaseAuth.currentUser?.uid
        Toast.makeText(context, "개인 색상 정보 확인 중...", Toast.LENGTH_SHORT).show()
        if (uid == null) {
            Toast.makeText(context, "로그인 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("User").document(uid).collection("results")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val colorResult = document.getString("result") ?: "Unknown"
                    Toast.makeText(context, "색상 결과: $colorResult", Toast.LENGTH_LONG).show()
                    updateUI(colorResult.toLowerCase(Locale.ROOT))
                } else {
                    Toast.makeText(context, "결과 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "정보를 불러오는 데 실패했습니다: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateUI(colorResult: String) {
        Toast.makeText(context, "UI 업데이트 중...", Toast.LENGTH_SHORT).show()
        val storageRef = FirebaseStorage.getInstance().reference
        val categories = listOf("base", "lip", "eye")
        categories.forEach { category ->
            val imageViews = getImageViewsByCategory(category)
            loadImages(storageRef, colorResult, category, imageViews)
        }
    }

    private fun getImageViewsByCategory(category: String): List<ImageView> {
        Toast.makeText(context, "$category 카테고리의 이미지 뷰 로드 중...", Toast.LENGTH_SHORT).show()
        return listOf(
            requireView().findViewById(resources.getIdentifier("${category}imageView_1", "id", context?.packageName)),
            requireView().findViewById(resources.getIdentifier("${category}imageView_2", "id", context?.packageName)),
            requireView().findViewById(resources.getIdentifier("${category}imageView_3", "id", context?.packageName))
        )
    }

    private fun loadImages(storageRef: StorageReference, categoryPrefix: String, category: String, imageViews: List<ImageView>) {
        imageViews.forEachIndexed { index, imageView ->
            val imagePath = "products/$categoryPrefix/${categoryPrefix}_${category}_${index + 1}.jpg"
            val imageRef = storageRef.child(imagePath)
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this).load(uri).into(imageView)
                Toast.makeText(context, "이미지 로드 성공: $imagePath", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Log.e("Storage", "Error loading image: $imagePath", exception)
                Toast.makeText(context, "이미지 로드 실패: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 추가된 함수들입니다.
    fun fetchUserUid(uid: String, callback: (String) -> Unit) {
        val diagnosticRef =
            FirebaseFirestore.getInstance().collection("User").document(uid).collection("results")
        diagnosticRef.orderBy("date", Query.Direction.DESCENDING).limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "No documents found")
                    callback("No documents found")
                } else {
                    var result: String? = null
                    for (document in documents) {
                        result = document.getString("result") ?: "No result found"
                    }
                    result?.let { callback(it) }
                    Toast.makeText(context, "사용자 UID 확인: $result", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
                callback("Error getting documents: ${exception.message}")
                Toast.makeText(context, "문서 가져오기 실패: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateProductDetails(result: String) {
        val toneRef = FirebaseFirestore.getInstance().collection("Tone").document(result)
        Log.d("FirestoreDebug", "Attempting to fetch document with ID: $result") // 문서 ID 로그 추가

        toneRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.d("FirestoreDebug", "No document found with ID: $result") // 문서가 없을 때 로그
                    Toast.makeText(context, "No data available for this product.", Toast.LENGTH_LONG).show()
                } else {
                    Log.d("FirestoreDebug", "Document successfully fetched with ID: $result") // 문서가 있을 때 로그
                    val productDescriptions = document.get("제품설명") as? List<Map<String, Any>> ?: listOf()
                    Log.d("FirestoreDebug", "Product descriptions found: $productDescriptions") // 제품 설명 로그

                    val baseMap = productDescriptions.find { it.containsKey("베이스") }?.get("베이스") as? Map<String, Any> ?: mapOf()
                    baseMap.let { products ->
                        baseProductTextViews.forEachIndexed { index, textView ->
                            val productKey = "베이스제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"
                            textView.text = "$productName\n$productBrand\n$productPrice"
                        }
                    }

                    val lipMap = productDescriptions.find { it.containsKey("립") }?.get("립") as? Map<String, Any> ?: mapOf()
                    lipMap.let { products ->
                        lipProductTextViews.forEachIndexed { index, textView ->
                            val productKey = "립제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"
                            textView.text = "$productName\n$productBrand\n$productPrice"
                        }
                    }

                    val eyeMap = productDescriptions.find { it.containsKey("아이") }?.get("아이") as? Map<String, Any> ?: mapOf()
                    eyeMap.let { products ->
                        eyeProductTextViews.forEachIndexed { index, textView ->
                            val productKey = "아이제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"
                            textView.text = "$productName\n$productBrand\n$productPrice"
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("FirestoreError", "Error fetching document with ID: $result", exception) // 오류 로그
                Toast.makeText(context, "Error loading details: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }
}
