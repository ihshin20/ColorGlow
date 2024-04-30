
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
import android.widget.Toast
import com.example.mycolor.R

class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_page, container, false)
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
        val eyeimageView = view.findViewById<ImageView>(R.id.eyeimageView)
        eyeimageView.setImageResource(R.drawable.proeyepalette)

        // 로그아웃 버튼에 대한 참조 및 클릭 리스너 설정
        val logoutButton = view.findViewById<Button>(R.id.button3)
        logoutButton.setOnClickListener {
            // 여기에서 로그아웃 처리 로직을 추가하세요.

            // 예를 들어, 사용자 데이터를 지우거나 서버에 로그아웃 요청을 보낼 수 있습니다.

            // 앱을 종료합니다 메시지 표시
            Toast.makeText(activity, "앱을 종료합니다.", Toast.LENGTH_SHORT).show()

            // Toast 메시지 표시 후 약간의 딜레이를 주고 앱 종료
            logoutButton.postDelayed({
                activity?.finish()
            }, 1000)
        }
    }
}