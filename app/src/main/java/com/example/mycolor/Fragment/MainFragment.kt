package com.example.mycolor.Fragment


import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.mycolor.R


class MainFragment : Fragment() {
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)

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
        val imageView = view.findViewById<ImageView>(R.id.imageView)
        imageView.setImageResource(R.drawable.logoimage)


        val textView6 = view.findViewById<TextView>(R.id.textView6)
        applySpannableStringToTextView(textView6, "배색", 1.5f)

        val textView4 = view.findViewById<TextView>(R.id.textView4)
        applyColorSpanToTextView(textView4, "조화로운가", R.color.Hotpink)

        val textView11 = view.findViewById<TextView>(R.id.textView11)
        applySpannableStringToTextView(textView11, "배색 효과", 1.7f)

        val textView8 = view.findViewById<TextView>(R.id.textView8)
        applySpannableStringToTextView(textView8, "봄, 여름, 가을, 겨울", 1.5f)

        val textView = view.findViewById<TextView>(R.id.textView5)
        applyColorSpanToTextView(textView, "퍼스널 컬러", R.color.Hotpink)
    }

    private fun applySpannableStringToTextView(textView: TextView, keyword: String, sizeMultiplier: Float) {
        val fullText = textView.text.toString()
        val spannableString = SpannableString(fullText)
        var startIndex = fullText.indexOf(keyword)
        while (startIndex >= 0) {
            val endIndex = startIndex + keyword.length
            spannableString.setSpan(
                RelativeSizeSpan(sizeMultiplier),
                startIndex,
                endIndex,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
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
            spannableString.setSpan(
                ForegroundColorSpan(color),
                startIndex,
                endIndex,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            startIndex = fullText.indexOf(keyword, endIndex)
        }
        textView.text = spannableString
    }
}
