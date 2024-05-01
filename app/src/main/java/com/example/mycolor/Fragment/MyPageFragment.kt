
package com.example.mycolor.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.mycolor.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        val result = activity?.intent?.getStringExtra("result") ?: "Default Result"
        val resultLower = result.lowercase(Locale.ROOT)  // 소문자로 변환

        // 제품 정보 불러오기 함수 호출, resultLower 인자 전달
        fetchProductInfo(resultLower)
    }

    private fun fetchProductInfo(resultLower: String) {
        // Firebase Firestore 인스턴스 생성
        val db = FirebaseFirestore.getInstance()
        // 해당 result를 사용하여 Tone 컬렉션의 문서 참조
        val productRef = db.collection("Tone").document(resultLower)


// 베이스 제품 정보 가져오기 예시
        productRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val productName = document.getString("name") ?: "제품명 정보 없음"
                val productBrand = document.getString("brand") ?: "브랜드 정보 없음"
                val productPrice = document.getString("price") ?: "가격 정보 없음"
                view?.findViewById<TextView>(R.id.baseproductTextView_1)?.text = "$productName\n$productBrand\n$productPrice"
            }
        }


        // 립 제품 정보 가져오기
        productRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val productName = document.getString("name") ?: "제품명 정보 없음"
                val productBrand = document.getString("brand") ?: "브랜드 정보 없음"
                val productPrice = document.getString("price") ?: "가격 정보 없음"
                view?.findViewById<TextView>(R.id.lipproductTextView_1)?.text =
                    "$productName\n$productBrand\n$productPrice"
            }
        }

        // 아이 제품 정보 가져오기
        productRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val productName = document.getString("name") ?: "제품명 정보 없음"
                val productBrand = document.getString("brand") ?: "브랜드 정보 없음"
                val productPrice = document.getString("price") ?: "가격 정보 없음"
                view?.findViewById<TextView>(R.id.eyeproductTextView_1)?.text =
                    "$productName\n$productBrand\n$productPrice"
            }
        }
    }
}
