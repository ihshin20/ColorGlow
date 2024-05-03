package com.example.mycolor.Fragment

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import retrofit2.http.Query
import java.util.Locale

class MyPageFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

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

        val logoutButton = view.findViewById<Button>(R.id.button3)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(activity, "앱을 종료합니다.", Toast.LENGTH_SHORT).show()
            logoutButton.postDelayed({
                activity?.finish()
            }, 1000)
        }

        fetchPersonalColorInfo()
    }

    private fun fetchPersonalColorInfo() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(context, "로그인 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firestore에서 사용자 정보 및 최근 결과 가져오기
        firestore.collection("User").document(uid).collection("results")
            .orderBy("date", Query.Direction.DESCENDING).limit(1)  // 'date' 필드명 확인 필요
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {  // 여기 수정됨
                    val document = documents.documents.first()
                    val colorResult = document.getString("result") ?: "Unknown"
                    updateUI(colorResult.toLowerCase(Locale.ROOT))
                } else {
                    Toast.makeText(context, "결과 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(colorResult: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val categories = listOf("base", "lip", "eye")
        categories.forEach { category ->
            val imageViews = getImageViewsByCategory(category)
            val productTextViews = getProductTextViewsByCategory(category)
            loadImages(storageRef, colorResult, category, imageViews)
            loadProductDetails(category, productTextViews)
        }
    }

    private fun getImageViewsByCategory(category: String): List<ImageView> {
        return listOf(
            requireView().findViewById(resources.getIdentifier("${category}imageView_1", "id", context?.packageName)),
            requireView().findViewById(resources.getIdentifier("${category}imageView_2", "id", context?.packageName)),
            requireView().findViewById(resources.getIdentifier("${category}imageView_3", "id", context?.packageName))
        )
    }

    private fun getProductTextViewsByCategory(category: String): List<TextView> {
        return listOf(
            requireView().findViewById(resources.getIdentifier("${category}productTextView_1", "id", context?.packageName)),
            requireView().findViewById(resources.getIdentifier("${category}productTextView_2", "id", context?.packageName)),
            requireView().findViewById(resources.getIdentifier("${category}productTextView_3", "id", context?.packageName))
        )
    }

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

    private fun loadProductDetails(category: String, productTextViews: List<TextView>) {
        firestore.collection("ProductDetails").document(category)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val products = document.get("products") as? List<Map<String, Any>> ?: listOf()
                    updateProductInfo(products, productTextViews)
                } else {
                    Toast.makeText(context, "제품 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "제품 정보를 불러오는데 실패했습니다: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProductInfo(products: List<Map<String, Any>>, productTextViews: List<TextView>) {
        products.forEachIndexed { index, product ->
            if (index < productTextViews.size) {
                val productName = product["productName"] as? String ?: "제품명 정보 없음"
                val productBrand = product["brand"] as? String ?: "브랜드 정보 없음"
                val productPrice = product["price"] as? String ?: "가격 정보 없음"
                productTextViews[index].text = "$productName\n$productBrand\n$productPrice"
            }
        }
    }
}
