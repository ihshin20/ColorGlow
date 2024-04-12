package com.example.mycolor.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.mycolor.R

class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_page_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ImageView 로컬 변수에 할당
        val logoimageView = view.findViewById<ImageView>(R.id.logoimageView)
        logoimageView.setImageResource(R.drawable.logoimage)
        val baseimageView = view.findViewById<ImageView>(R.id.baseimageView)
        baseimageView.setImageResource(R.drawable.killcover)
        val lipimageView = view.findViewById<ImageView>(R.id.lipimageView)
        // 립 제품 이미지 뷰에 클릭 리스너를 설정
        lipimageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://clubclio.co.kr/shop/goodsView/0000002419"))
            startActivity(intent)
        }

    }
}