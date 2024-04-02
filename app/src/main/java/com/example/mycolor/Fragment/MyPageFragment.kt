package com.example.mycolor.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
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

        view.findViewById<ImageView>(R.id.imageView).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://clubclio.co.kr/shop/goodsView/0000002419")))
        }

        view.findViewById<ImageView>(R.id.Baseimage).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://clubclio.co.kr/shop/goodsView/0000005117")))
        }
    }
}

