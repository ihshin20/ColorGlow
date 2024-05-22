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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mycolor.R

import com.example.mycolor.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Locale

class MyPageFragment : Fragment() {

    private lateinit var uid: String

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var baseProductTextViews: List<TextView>
    private lateinit var lipProductTextViews: List<TextView>
    private lateinit var eyeProductTextViews: List<TextView>


    private var baseUrls: MutableList<String> = mutableListOf()
    private var lipUrls: MutableList<String> = mutableListOf()
    private var eyeUrls: MutableList<String> = mutableListOf()

    private lateinit var defaultText1: TextView
    private lateinit var defaultText2: TextView
    private lateinit var defaultText3: TextView
    private lateinit var defaultText4: TextView
    private lateinit var defaultText5: TextView


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

        val nameTextView = view.findViewById<TextView>(R.id.customerName)
        val nameTextView2 = view.findViewById<TextView>(R.id.PersonalColorName)
        // ImageView 로컬 변수에 할당
        val logoimageView = view.findViewById<ImageView>(R.id.logoimageView)
        logoimageView.setImageResource(R.drawable.logoimage)

        defaultText1 = view.findViewById(R.id.yourcoloris)
        defaultText2 = view.findViewById(R.id.itis)
        defaultText3 = view.findViewById(R.id.recommendBase)
        defaultText4 = view.findViewById(R.id.recommendLip)
        defaultText5 = view.findViewById(R.id.recommendEye)



        showLoadingDialog()

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

        val db = FirebaseFirestore.getInstance()

        // User 컬렉션에서 해당 uid의 문서를 참조합니다.
        val userRef = db.collection("User").document(uid)

        userRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    // 문서가 존재하고, name 필드에 값이 있을 경우 그 값을 가져옴
                    val name = document.getString("name") ?: "" // name 필드가 null이면 "상상부기"를 사용
                    nameTextView.text = name

                    // results 컬렉션에서 최신 결과 가져오기
                    userRef.collection("results").orderBy("date", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.isEmpty) {
                                nameTextView2.text = "" // 결과가 없는 경우 기본값을 사용
                                Log.d("Firestore", "No documents found")
                            } else {
                                defaultText1.text = "님의 퍼스널 컬러는"
                                defaultText2.text = "입니다."
                                defaultText3.text = "추천 베이스"
                                defaultText4.text = "추천 립"
                                defaultText5.text = "추천 아이"
                                val resultDocument = documents.documents[0]
                                val result = resultDocument.getString("result") ?: ""
                                val replacedResult = result.replace("_", " ")
                                nameTextView2.text = replacedResult

                                Log.d("Firestore", "Personal Color: $result")
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w("Firestore", "Error getting documents: ", exception)
                        }

                    Log.d("Firestore", "Name: $name")
                } else {
                    // 문서가 존재하지 않을 경우, 기본값을 설정
                    Log.d("Firestore", "No such document, setting default name to '상상부기'")
                }
            } else {
                Log.d("Firestore", "get failed with ", task.exception)
            }
        }


        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 로그아웃 버튼에 대한 참조 및 클릭 리스너 설정
        val logoutButton = view.findViewById<Button>(R.id.button3)
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(activity, "로그아웃합니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

        }

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

        fetchUserUid(uid) { result ->
            if (result.startsWith("Error") || result == "No documents found") {
                Toast.makeText(context, "진단 이력이 없습니다.", Toast.LENGTH_LONG).show()
            } else {
                updateProductDetails(result)
            }
        }

        fetchPersonalColorInfo()
    }


    private fun fetchPersonalColorInfo() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        firestore.collection("User").document(uid).collection("results")
            .orderBy("date", Query.Direction.DESCENDING).limit(1)
            .get(Source.SERVER)  // 캐시를 무시하고 서버에서 데이터를 직접 가져옴
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    println("No recent color results found.")
                } else {
                    val document = documents.documents.first()
                    val colorResult = document.getString("result") ?: "default"
                    updateUI(colorResult.toLowerCase())
                }
            }
            .addOnFailureListener { e ->
                println("Error fetching personal color info: ${e.message}")
            }
    }


    private fun updateUI(colorResult: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val categories = listOf("base", "lip", "eye")
        categories.forEach { category ->
            val imageViews = getImageViewsByCategory(category)
            val urls = when (category) {
                "base" -> baseUrls
                "lip" -> lipUrls
                "eye" -> eyeUrls
                else -> mutableListOf<String>("none")  // Fallback empty list, should not be used
            }


            loadImages(storageRef, colorResult, category, imageViews, urls){
                hideLoadingDialog()
            }
        }
    }


    private fun getImageViewsByCategory(category: String): List<ImageView> {
        // 현재 뷰가 null인지 확인하고, null이 아니면 진행
        val currentView = view ?: return emptyList()  // view가 null이면 빈 리스트를 반환

        return listOf(
            currentView.findViewById(
                resources.getIdentifier(
                    "${category}imageView_1",
                    "id",
                    context?.packageName
                )
            ),
            currentView.findViewById(
                resources.getIdentifier(
                    "${category}imageView_2",
                    "id",
                    context?.packageName
                )
            ),
            currentView.findViewById(
                resources.getIdentifier(
                    "${category}imageView_3",
                    "id",
                    context?.packageName
                )
            )
        )
    }



    private fun loadImages(
        storageRef: StorageReference,
        categoryPrefix: String,
        category: String,
        imageViews: List<ImageView>,
        urls: List<String>,
        onComplete: () -> Unit // 콜백 추가
    ) {
        val totalImages = imageViews.size
        var loadedImages = 0

        imageViews.forEachIndexed { index, imageView ->
            val imagePath =
                "products/$categoryPrefix/${categoryPrefix}_${category}_${index + 1}.jpg"
            val imageRef = storageRef.child(imagePath)
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                if (isAdded) {
                    Glide.with(this).load(uri).into(imageView)
                }

                imageView.setOnClickListener {
                    if (urls[index] != "none") {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urls[index]))
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "해당 제품 홈페이지가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                loadedImages++
                if (loadedImages == totalImages) {
                    onComplete()
                }
            }.addOnFailureListener { exception ->
                Log.e("Storage", "Error loading image: $imagePath", exception)

                loadedImages++
                if (loadedImages == totalImages) {
                    onComplete()
                }
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

                    defaultText1.text = "님은 아직 진단 이력이 없습니다."
                    hideLoadingDialog()

                    callback("No documents found")
                } else {
                    var result: String? = null
                    for (document in documents) {
                        result = document.getString("result") ?: "No result found"
                    }
                    result?.let { callback(it) }
                    //Toast.makeText(context, "사용자 UID 확인: $result", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents: ", exception)
                callback("Error getting documents: ${exception.message}")
                //Toast.makeText(context, "문서 가져오기 실패: ${exception.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateProductDetails(result: String) {
        val currentView = view ?: return

        val toneRef = FirebaseFirestore.getInstance().collection("Tone").document(result)
        Log.d("FirestoreDebug", "Attempting to fetch document with ID: $result") // 문서 ID 로그 추가

        toneRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.d("FirestoreDebug", "No document found with ID: $result") // 문서가 없을 때 로그
                    Toast.makeText(context,"네트워크 에러입니다.",Toast.LENGTH_LONG).show()
                } else {
                    Log.d(
                        "FirestoreDebug",
                        "Document successfully fetched with ID: $result"
                    ) // 문서가 있을 때 로그
                    val productDescriptions =
                        document.get("제품설명") as? List<Map<String, Any>> ?: listOf()
                    Log.d(
                        "FirestoreDebug",
                        "Product descriptions found: $productDescriptions"
                    ) // 제품 설명 로그

                    // Initialize TextViews for base products
                    val baseProductNameTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.baseproductTextView_1),
                        currentView.findViewById<TextView>(R.id.baseproductTextView_2),
                        currentView.findViewById<TextView>(R.id.baseproductTextView_3)
                    )

                    val baseProductBrandTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.baseproductTextView_11),
                        currentView.findViewById<TextView>(R.id.baseproductTextView_22),
                        currentView.findViewById<TextView>(R.id.baseproductTextView_33)
                    )

                    val baseProductPriceTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.baseproductTextView_111),
                        currentView.findViewById<TextView>(R.id.baseproductTextView_222),
                        currentView.findViewById<TextView>(R.id.baseproductTextView_333)
                    )

                    // Initialize TextViews for other product categories
                    val lipProductNameTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.lipproductTextView_1),
                        currentView.findViewById<TextView>(R.id.lipproductTextView_2),
                        currentView.findViewById<TextView>(R.id.lipproductTextView_3)
                    )

                    val lipProductBrandTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.lipproductTextView_11),
                        currentView.findViewById<TextView>(R.id.lipproductTextView_22),
                        currentView.findViewById<TextView>(R.id.lipproductTextView_33)
                    )

                    val lipProductPriceTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.lipproductTextView_111),
                        currentView.findViewById<TextView>(R.id.lipproductTextView_222),
                        currentView.findViewById<TextView>(R.id.lipproductTextView_333)
                    )

                    // Initialize TextViews for eye products
                    val eyeProductNameTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_1),
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_2),
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_3)
                    )

                    val eyeProductBrandTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_11),
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_22),
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_33)
                    )

                    val eyeProductPriceTextViews = listOf(
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_111),
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_222),
                        currentView.findViewById<TextView>(R.id.eyeproductTextView_333)
                    )



                    val baseMap = productDescriptions.find { it.containsKey("베이스") }
                        ?.get("베이스") as? Map<String, Any> ?: mapOf()
                    Log.d("FirestoreDebug", "Base products found: $baseMap") // Base products 로그

                    baseMap.let { products ->
                        baseProductNameTextViews.forEachIndexed { index, textView ->
                            val productKey = "베이스제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            textView.text = productName
                            Log.d(
                                "FirestoreDebug",
                                "Set product name for $productKey: $productName"
                            ) // 제품명 설정 로그
                        }

                        baseProductBrandTextViews.forEachIndexed { index, textView ->
                            val productKey = "베이스제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            textView.text = productBrand
                            Log.d(
                                "FirestoreDebug",
                                "Set product brand for $productKey: $productBrand"
                            ) // 브랜드 설정 로그
                        }

                        baseProductPriceTextViews.forEachIndexed { index, textView ->
                            val productKey = "베이스제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"
                            textView.text = productPrice
                            Log.d(
                                "FirestoreDebug",
                                "Set product price for $productKey: $productPrice"
                            ) // 가격 설정 로그
                        }

                        products.forEach { (productKey, productInfo) ->
                            val productUrl =
                                (productInfo as? Map<String, Any>)?.get("url") as? String ?: "none"
                            baseUrls.add(productUrl)
                            Log.d(
                                "FirestoreDebug",
                                "Added URL for $productKey: $productUrl"
                            ) // URL 추가 로그
                        }
                    }

                    val lipMap = productDescriptions.find { it.containsKey("립") }
                        ?.get("립") as? Map<String, Any> ?: mapOf()
                    Log.d("FirestoreDebug", "Lip products found: $lipMap") // Lip products 로그

                    lipMap.let { products ->
                        lipProductNameTextViews.forEachIndexed { index, textView ->
                            val productKey = "립제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            textView.text = productName
                            Log.d(
                                "FirestoreDebug",
                                "Set product name for $productKey: $productName"
                            ) // 제품명 설정 로그
                        }

                        lipProductBrandTextViews.forEachIndexed { index, textView ->
                            val productKey = "립제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            textView.text = productBrand
                            Log.d(
                                "FirestoreDebug",
                                "Set product brand for $productKey: $productBrand"
                            ) // 브랜드 설정 로그
                        }

                        lipProductPriceTextViews.forEachIndexed { index, textView ->
                            val productKey = "립제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"
                            textView.text = productPrice
                            Log.d(
                                "FirestoreDebug",
                                "Set product price for $productKey: $productPrice"
                            ) // 가격 설정 로그
                        }

                        products.forEach { (productKey, productInfo) ->
                            val productUrl =
                                (productInfo as? Map<String, Any>)?.get("url") as? String ?: "none"
                            lipUrls.add(productUrl)
                            Log.d(
                                "FirestoreDebug",
                                "Added URL for $productKey: $productUrl"
                            ) // URL 추가 로그
                        }
                    }

                    val eyeMap = productDescriptions.find { it.containsKey("아이") }
                        ?.get("아이") as? Map<String, Any> ?: mapOf()
                    Log.d("FirestoreDebug", "Eye products found: $eyeMap") // Eye products 로그

                    eyeMap.let { products ->
                        eyeProductNameTextViews.forEachIndexed { index, textView ->
                            val productKey = "아이제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productName = productInfo?.get("제품이름") as? String ?: "제품명 정보 없음"
                            textView.text = productName
                            Log.d(
                                "FirestoreDebug",
                                "Set product name for $productKey: $productName"
                            ) // 제품명 설정 로그
                        }

                        eyeProductBrandTextViews.forEachIndexed { index, textView ->
                            val productKey = "아이제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productBrand = productInfo?.get("브랜드") as? String ?: "브랜드 정보 없음"
                            textView.text = productBrand
                            Log.d(
                                "FirestoreDebug",
                                "Set product brand for $productKey: $productBrand"
                            ) // 브랜드 설정 로그
                        }

                        eyeProductPriceTextViews.forEachIndexed { index, textView ->
                            val productKey = "아이제품${index + 1}"
                            val productInfo = products[productKey] as? Map<String, Any>
                            val productPrice = productInfo?.get("가격") as? String ?: "가격 정보 없음"
                            textView.text = productPrice
                            Log.d(
                                "FirestoreDebug",
                                "Set product price for $productKey: $productPrice"
                            ) // 가격 설정 로그
                        }

                        products.forEach { (productKey, productInfo) ->
                            val productUrl =
                                (productInfo as? Map<String, Any>)?.get("url") as? String ?: "none"
                            eyeUrls.add(productUrl)
                            Log.d(
                                "FirestoreDebug",
                                "Added URL for $productKey: $productUrl"
                            ) // URL 추가 로그
                        }
                    }
                }
            }

            .addOnFailureListener { exception ->
                Log.w(
                    "FirestoreError",
                    "Error fetching document with ID: $result",
                    exception
                ) // 오류 로그
                Toast.makeText(
                    context,
                    "네트워크 에러입니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private lateinit var loadingDialog: AlertDialog

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())

        val view = inflater.inflate(R.layout.loading_dialog, null)

        // TextView의 텍스트를 변경합니다
        val textView = view.findViewById<TextView>(R.id.loadingProgressTextView)
        textView.text = "잠시만 기다려주세요."

        builder.setView(view)
        builder.setCancelable(false)

        loadingDialog = builder.create()
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }


}

