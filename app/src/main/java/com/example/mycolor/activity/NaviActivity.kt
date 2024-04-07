package com.example.mycolor.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.example.mycolor.Fragment.HomeFragment
import com.example.mycolor.Fragment.MyPageFragment
import com.example.mycolor.Fragment.ResultFragment
import com.example.mycolor.R
import com.example.mycolor.databinding.ActivityNaviBinding


private const val TAG_HOME = "home_fragment"
private const val TAG_RESULT = "result_fragment"
private const val TAG_MY_PAGE = "my_page_fragment"

class NaviActivity : AppCompatActivity() {

    private lateinit var binding : ActivityNaviBinding

    private var backPressedTime: Long = 0
    private lateinit var backToast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNaviBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // setFragment -> replaceFragment
        binding.navigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.HomeFragment -> replaceFragment(TAG_HOME)
                R.id.ResultFragment -> replaceFragment(TAG_RESULT)
                R.id.MyPageFragment-> replaceFragment(TAG_MY_PAGE)
            }
            true
        }

        // 백버튼 두 번 누르면 종료
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    backToast.cancel()
                    finish() // 앱 종료
                } else {
                    backToast = Toast.makeText(baseContext, "뒤로 가기 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT)
                    backToast.show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)


    }


    // setFragment(hide and show방식) -> replace 방식으로 변경
    private fun replaceFragment(tag: String) {
        val manager: FragmentManager = supportFragmentManager
        val fragTransaction = manager.beginTransaction()

        var frag = manager.findFragmentByTag(tag)
        if (frag == null) {
            frag = when(tag) {
                TAG_HOME -> HomeFragment()
                TAG_RESULT -> ResultFragment()
                TAG_MY_PAGE -> MyPageFragment()
                else -> return
            }
        }

        fragTransaction.replace(R.id.FragmentContainer, frag, tag)
        fragTransaction.commit()
    }



//    private fun setFragment(tag: String, fragment: Fragment) {
//        val manager: FragmentManager = supportFragmentManager
//        val fragTransaction = manager.beginTransaction()
//
//        if (manager.findFragmentByTag(tag) == null){
//            fragTransaction.add(R.id.nav_host_fragment, fragment, tag)
//        }
//
//        val home = manager.findFragmentByTag(TAG_HOME)
//        val result = manager.findFragmentByTag(TAG_RESULT)
//        val myPage = manager.findFragmentByTag(TAG_MY_PAGE)
//
//        if (home != null){
//            fragTransaction.hide(home)
//        }
//
//        if (result != null){
//            fragTransaction.hide(result)
//        }
//
//        if (myPage != null) {
//            fragTransaction.hide(myPage)
//        }
//
//        if (tag == TAG_HOME) {
//            if (home!=null){
//                fragTransaction.show(home)
//            }
//        }
//        else if (tag == TAG_RESULT) {
//            if (result != null) {
//                fragTransaction.show(result)
//            }
//        }
//
//        else if (tag == TAG_MY_PAGE){
//            if (myPage != null){
//                fragTransaction.show(myPage)
//            }
//        }
//
//        fragTransaction.commitAllowingStateLoss()
//    }
}


