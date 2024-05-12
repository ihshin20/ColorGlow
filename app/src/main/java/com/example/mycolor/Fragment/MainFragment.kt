package com.example.mycolor.Fragment


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.example.mycolor.R
import com.example.mycolor.activity.DetailResultActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


interface ApiService {
    @Multipart
    @POST("upload")
    fun uploadImageAndText(
        @Part image: MultipartBody.Part,
        @Part("text") text: RequestBody
    ): Call<ServerResponse>
}

// 서버 응답 받는 양식
data class ServerResponse(
    val message: String,
    val textData: String,
    val pythonResult: String
)

class MainFragment : Fragment() {
    private lateinit var navController: NavController

    private lateinit var uid: String
    private var nowFlag = 0
    private lateinit var imageBitmap: Bitmap
    private lateinit var currentPhotoPath: String

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val REQUEST_GALLERY_ACCESS = 102

        // server IP 맞춰서 수정해야 함
        private const val BASE_URL = "http://34.22.71.204:3000/"

        //"http://13.125.218.109:3000/"
        private lateinit var apiService: ApiService

    }

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

        val logoimageView1 = view.findViewById<ImageView>(R.id.logoimageView1)
        logoimageView1.setImageResource(R.drawable.logoimage)
        val pccsimageView = view.findViewById<ImageView>(R.id.pccsimageView)
        pccsimageView.setImageResource(R.drawable.pccs)
        val twofaceimageView = view.findViewById<ImageView>(R.id.twofaceimageView)
        twofaceimageView.setImageResource(R.drawable.twofaceimage)
        val grassimageView = view.findViewById<ImageView>(R.id.grassimageView)
        grassimageView.setImageResource(R.drawable.grassimage)
        val shoppingimageView = view.findViewById<ImageView>(R.id.shoppingimageView)
        shoppingimageView.setImageResource(R.drawable.shoppingimage)
        val logotextimage = view.findViewById<ImageView>(R.id.logotextimage)
        logotextimage.setImageResource(R.drawable.logotextimage)


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

        val personalColorInfoButton = view.findViewById<Button>(R.id.btnShowPersonalColorInfo)
        personalColorInfoButton.setOnClickListener {
            showPersonalColorPopup()
        }


        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS) // 연결 타임아웃
            .readTimeout(120, TimeUnit.SECONDS) // 읽기 타임아웃
            .writeTimeout(120, TimeUnit.SECONDS) // 쓰기 타임아웃
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(MainFragment.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient) // OkHttpClient 설정
            .build()

        // Firebase 인증 객체 생성
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

        apiService = retrofit.create(ApiService::class.java)


        // "검사하기" 버튼 클릭 이벤트 설정
        val checkButton = view.findViewById<ImageButton>(R.id.Callbutton)
        checkButton.setOnClickListener {
            // Bottom Sheet Dialog 보여주기
            showBottomSheetDialog()
        }
    }

    private fun applySpannableStringToTextView(
        textView: TextView,
        keyword: String,
        sizeMultiplier: Float
    ) {
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

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        with(bottomSheetDialog) {
            setContentView(R.layout.bottom_sheet_dialog)
            setCanceledOnTouchOutside(true) // 바깥 영역 터치시 다이얼로그 닫기 활성화
            show()

            val cameraBtn = findViewById<Button>(R.id.cameraBtn)
            val galBtn = findViewById<Button>(R.id.galleryBtn)

            cameraBtn?.setOnClickListener {
                when {
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        val photoFile: File? = try {
                            createImageFile()
                        } catch (ex: IOException) {
                            null
                        }
                        photoFile?.also {
                            val photoURI = FileProvider.getUriForFile(
                                requireContext(),
                                "com.example.mycolor.fileprovider",
                                it
                            )
                            currentPhotoPath = it.absolutePath
                            takePictureLauncher.launch(photoURI)
                        }
                    }

                    else -> {
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                dismiss()
            }

            galBtn?.setOnClickListener {
                pickImageLauncher.launch("image/*")
                dismiss()
            }


        }
    }


    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // 권한이 승인되었으므로 카메라를 바로 실행합니다.
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI = FileProvider.getUriForFile(
                        requireContext(),
                        "com.example.mycolor.fileprovider",
                        it
                    )
                    currentPhotoPath = it.absolutePath
                    takePictureLauncher.launch(photoURI)  // 카메라를 직접 실행
                }
            } else {
                Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                // Image was captured and saved to fileUri
                val imageFile = File(currentPhotoPath)
                if (imageFile.exists()) {
                    imageBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    imageBitmap = rotateImageIfNeeded(currentPhotoPath, imageBitmap) // 회전 추가
                    uploadImageAndText(imageBitmap, uid)
                }
            } else {
                Toast.makeText(context, "Failed to take picture", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val inputStream = context?.contentResolver?.openInputStream(uri)
                imageBitmap = BitmapFactory.decodeStream(inputStream)
                uploadImageAndText(imageBitmap, uid)
            }
        }


    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).also {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = it.absolutePath
        }
    }


    private fun rotateImageIfNeeded(photoPath: String, bitmap: Bitmap): Bitmap {
        val exif = try {
            ExifInterface(photoPath)
        } catch (e: IOException) {
            e.printStackTrace()
            return bitmap
        }
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    private lateinit var loadingDialog: AlertDialog

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(context)
        val inflater = layoutInflater
        builder.setView(inflater.inflate(R.layout.loading_dialog, null))
        builder.setCancelable(false)

        loadingDialog = builder.create()
        loadingDialog.show()
    }

    private fun hideLoadingDialog() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun uploadImageAndText(bitmap: Bitmap, text: String) {

        showLoadingDialog() // 로딩 다이얼로그 표시

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), baos.toByteArray())
        val part = MultipartBody.Part.createFormData("image", "image.jpg", requestBody)
        val textPart = RequestBody.create("text/plain".toMediaTypeOrNull(), text)

        apiService.uploadImageAndText(part, textPart).enqueue(object : Callback<ServerResponse> {
            override fun onResponse(
                call: Call<ServerResponse>,
                response: Response<ServerResponse>
            ) {
                hideLoadingDialog()
                if (response.isSuccessful) {
                    // 서버 응답 성공 처리
                    val serverResponse = response.body()


                    if (serverResponse?.pythonResult == "1face") {
                        Toast.makeText(
                            context,
                            "한 사람의 얼굴만 잘 나오는 사진으로 다시 진행해주세요.",
                            Toast.LENGTH_SHORT

                        ).show()
                    } else if (serverResponse?.pythonResult == "error") {
                        Toast.makeText(
                            context,
                            "네트워크 에러입니다. 나중에 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            "진단 완료! 상세 진단 결과 페이지로 이동합니다.",
                            Toast.LENGTH_SHORT


                        ).show()

                        addResult(uid, serverResponse?.pythonResult)

                    }


                    // 서버 응답 출력
                    Log.d(
                        "ServerResponse",
                        "Text Data: ${serverResponse?.textData}, Python Result: ${serverResponse?.pythonResult}"
                    )
                    // 즉, serverResponse?.textData -> UID, serverResponse?.pythonResult -> 진단결과 톤(ex. Bright_Sprint)

                    Toast.makeText(context, serverResponse?.pythonResult, Toast.LENGTH_SHORT).show()
                    //resultText.text = serverResponse?.pythonResult

                } else {
                    // 서버 응답 오류 처리
                    Toast.makeText(
                        context,
                        "Upload failed: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ServerResponse>, t: Throwable) {
                Toast.makeText(context, "Upload failed: ${t.message}", Toast.LENGTH_SHORT).show()

                Log.d("UploadError", t.message ?: "Error message is null")

            }
        })
    }

    fun addResult(uid: String, result: String?) {
        val db = FirebaseFirestore.getInstance()
        val userResults = db.collection("User").document(uid).collection("results")

        val date = Date()

        val diagnosticData = hashMapOf(
            "date" to date,
            "result" to result
        )

        userResults.add(diagnosticData)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "DocumentSnapshot written with ID: ${documentReference.id}")

                val intent = Intent(context, DetailResultActivity::class.java)
                intent.putExtra("result", result)
                intent.putExtra("uid", uid)

                val imagePath = saveImageToFile(requireContext(), imageBitmap, "temp.png")

                //val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, 200, 200, true)
                intent.putExtra("imgPath", imagePath)

                nowFlag = 1
                intent.putExtra("flag", nowFlag)

                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
            }
    }

    private fun saveImageToFile(context: Context, imageBitmap: Bitmap, filename: String): String? {
        val file = File(context.filesDir, filename)
        return try {
            FileOutputStream(file).use { out ->
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // PNG 형식으로 압축 없이 저장
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun showPersonalColorPopup() {
        val builder = AlertDialog.Builder(requireContext())
        builder.apply {
            setTitle("퍼스널 컬러에 대한 설명")
            setView(R.layout.dialog_personal_color)  // Ensure you have a layout file named dialog_personal_color.xml
            setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

}


