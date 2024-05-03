
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
import com.example.mycolor.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Locale

class MyPageFragment : Fragment() {

    private lateinit var uid: String

    private lateinit var baseProductTextViews: List<TextView>
    private lateinit var lipProductTextViews: List<TextView>
    private lateinit var eyeProductTextViews: List<TextView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        baseProductTextViews = listOf(
            view.findViewById<TextView>(R.id.baseproductTextView_11),
            view.findViewById<TextView>(R.id.baseproductTextView_22),
            view.findViewById<TextView>(R.id.baseproductTextView_33)
        )
        lipProductTextViews = listOf(
            view.findViewById<TextView>(R.id.lipproductTextView_11),
            view.findViewById<TextView>(R.id.lipproductTextView_22),
            view.findViewById<TextView>(R.id.lipproductTextView_33)
        )
        eyeProductTextViews = listOf(
            view.findViewById<TextView>(R.id.eyeproductTextView_11),
            view.findViewById<TextView>(R.id.eyeproductTextView_22),
            view.findViewById<TextView>(R.id.eyeproductTextView_33)
        )

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


        // 기존 코드
        val logoimageView = view.findViewById<ImageView>(R.id.logoimageView)
        logoimageView.setImageResource(R.drawable.logoimage)
        val baseimageView = view.findViewById<ImageView>(R.id.baseimageView)
        baseimageView.setImageResource(R.drawable.killcover)
        val lipimageView = view.findViewById<ImageView>(R.id.lipimageView)
        lipimageView.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://clubclio.co.kr/shop/goodsView/0000002419")
            )
            startActivity(intent)
        }
        val eyeimageView = view.findViewById<ImageView>(R.id.eyeimageView)
        eyeimageView.setImageResource(R.drawable.proeyepalette)
        val logoutButton = view.findViewById<Button>(R.id.button3)
        logoutButton.setOnClickListener {
            Toast.makeText(activity, "앱을 종료합니다.", Toast.LENGTH_SHORT).show()
            logoutButton.postDelayed({
                activity?.finish()
            }, 1000)
        }





        // 제품 정보 불러오기 함수 호출, resultLower 인자 전달
        val res = fetchUserUid(uid)
        updateProductDetails(res)



    }


    //가장 최근 date에서 uid일치하는지 보기
    fun fetchUserUid(uid: String): String {

        lateinit var result:String
        // Access Firestore to fetch the most recent document
        val diagnosticRef = FirebaseFirestore.getInstance().collection("User").document(uid)
            .collection("results")
        diagnosticRef.orderBy("date", Query.Direction.DESCENDING).limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("Firestore", "No documents found")
                } else {
                    for (document in documents) {
                        result =
                            document.getString("results")!!
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
            }
        return result

    }

//파이어베이스에서 텍스트(베이스, 립, 아이) 정보 불러오기
    private fun updateProductDetails(res:String) {
        val toneRef = FirebaseFirestore.getInstance().collection("Tone").document(res)

        // toneRef = FirebaseFirestore.getInstance().document("result")
        toneRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {

                    // '제품설명' 배열에서 데이터 추출
                    val productDescriptions =
                        document.get("제품설명") as? List<Map<String, Any>> ?: listOf()

                    // '베이스' 카테고리의 데이터를 찾습니다.
                    val baseMap = productDescriptions.find { it.containsKey("베이스") }
                        ?.get("베이스") as? Map<String, Any> ?: mapOf()

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
                    val lipMap = productDescriptions.find { it.containsKey("립") }
                        ?.get("립") as? Map<String, Any> ?: mapOf()

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
                    val eyeMap = productDescriptions.find { it.containsKey("아이") }
                        ?.get("아이") as? Map<String, Any> ?: mapOf()

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


                } else {
                    Log.d("Firestore", "No Tone document found")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting Tone document: ", exception)
            }
    }
}