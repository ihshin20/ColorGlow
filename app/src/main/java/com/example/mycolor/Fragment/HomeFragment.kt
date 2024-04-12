package com.example.mycolor.Fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager

import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mycolor.R


import com.google.android.material.floatingactionbutton.FloatingActionButton

// 서버 응답 받는 양식
data class ServerResponse(
    val message: String,
    val textData: String,
    val pythonResult: String
)

class HomeFragment : Fragment(R.id.homeFragment) {

    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val REQUEST_GALLERY_ACCESS = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Handling fragment arguments if necessary
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ImageView에 대한 참조
        val imageView2 = view.findViewById<ImageView>(R.id.imageView2)
        imageView2.setImageResource(R.drawable.logoimage)
        val pccsimageView = view.findViewById<ImageView>(R.id.pccsimageView)
        pccsimageView.setImageResource(R.drawable.pccs)
        val twofaceimageView = view.findViewById<ImageView>(R.id.twofaceimageView)
        twofaceimageView.setImageResource(R.drawable.twofaceimage)
        val grassimageView = view.findViewById<ImageView>(R.id.grassimageView)
        grassimageView.setImageResource(R.drawable.grassimage)
        val shoppingimageView = view.findViewById<ImageView>(R.id.shoppingimageView)
        shoppingimageView.setImageResource(R.drawable.shoppingimage)
        val imageView = view.findViewById<ImageView>(R.id.lipimageView)
        imageView.setImageResource(R.drawable.logoimage)


        view.findViewById<FloatingActionButton>(R.id.fab_camera).setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCameraPermission()
            }
        }
        view.findViewById<FloatingActionButton>(R.id.fab_gallery).setOnClickListener {
            openGallery()
        }
        //텍스트뷰 부분 글자크기,글자색상 편집 편집
        val textView6 = view.findViewById<TextView>(R.id.textView6)
        applySpannableStringToTextView(textView6, "배색", 1.5f)

        val textView4 = view.findViewById<TextView>(R.id.textView4)
        applyColorSpanToTextView(textView4, "조화로운가", R.color.Hotpink)

        val textView11 = view.findViewById<TextView>(R.id.textView11)
        applySpannableStringToTextView(textView11, "배색 효과", 1.7f)

        val textView8 = view.findViewById<TextView>(R.id.textView8)
        applySpannableStringToTextView(textView8, "봄, 여름, 가을, 겨울", 1.5f)


        val textView = view.findViewById<TextView>(R.id.itis)
        applyColorSpanToTextView(textView, "퍼스널 컬러",R.color.Hotpink)


    }
    private fun applySpannableStringToTextView(textView: TextView, keyword: String, sizeMultiplier: Float) {
        val fullText = textView.text.toString()
        val spannableString = SpannableString(fullText)
        var startIndex = fullText.indexOf(keyword)
        while (startIndex >= 0) {
            val endIndex = startIndex + keyword.length
            spannableString.setSpan(RelativeSizeSpan(sizeMultiplier), startIndex, endIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            startIndex = fullText.indexOf(keyword, endIndex)
        }
        textView.text = spannableString
    }

    private fun applyColorSpanToTextView(textView: TextView, keyword: String, colorResId: Int) {
        val fullText = textView.text.toString()
        val spannableString = SpannableString(fullText)
        var startIndex = fullText.indexOf(keyword)
        while (startIndex >= 0) {
            val endIndex = startIndex + keyword.length
            val color = ContextCompat.getColor(requireContext(), colorResId)
            spannableString.setSpan(ForegroundColorSpan(color), startIndex, endIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            startIndex = fullText.indexOf(keyword, endIndex)
        }
        textView.text = spannableString
    }





    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_GALLERY_ACCESS)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

}