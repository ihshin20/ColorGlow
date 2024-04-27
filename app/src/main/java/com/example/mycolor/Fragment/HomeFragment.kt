package com.example.mycolor.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment


import com.example.mycolor.R
import com.example.mycolor.activity.DetailResultActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull


import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream

import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface


// API Interface
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

@SuppressLint("ResourceType")
class HomeFragment : Fragment(R.id.homeFragment) {

    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView
    private lateinit var uid:String
    private var nowFlag = 0

    private lateinit var imageBitmap: Bitmap

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val REQUEST_GALLERY_ACCESS = 102

        // server IP 맞춰서 수정해야 함
        private const val BASE_URL = "http://34.22.71.204:3000/"
        //"http://13.125.218.109:3000/"
        private lateinit var apiService: ApiService

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



        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS) // 연결 타임아웃
            .readTimeout(120, TimeUnit.SECONDS) // 읽기 타임아웃
            .writeTimeout(120, TimeUnit.SECONDS) // 쓰기 타임아웃
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
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

//        val retrofit = Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()

        apiService = retrofit.create(ApiService::class.java)

        imageView = view.findViewById(R.id.imageViewTest)
        resultText = view.findViewById(R.id.testResult)
        resultText.text = "진단 ㄱㄱ"


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
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

//    private fun launchCamera() {
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        try {
//            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
//        } catch (e: Exception) {
//            Toast.makeText(context, "Error opening camera", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun launchCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(context, "Photo file could not be created, please try again", Toast.LENGTH_SHORT).show()
                null
            }

            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.mycolor.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            Toast.makeText(context, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }




    private lateinit var currentPhotoPath: String

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {

            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageFile = File(currentPhotoPath)
                    if (imageFile.exists()) {
                        imageBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

                        imageBitmap = rotateImageIfNeeded(currentPhotoPath, imageBitmap) // 회전 추가

                        imageView.setImageBitmap(imageBitmap)
                        uploadImageAndText(imageBitmap, uid)
                    }
                }
                REQUEST_GALLERY_ACCESS -> {
                    // Gallery
                    val selectedImageUri: Uri? = data?.data
                    selectedImageUri?.let {
                        imageView.setImageURI(it)
                        context?.contentResolver?.openInputStream(it)?.let { inputStream ->
                            imageBitmap = BitmapFactory.decodeStream(inputStream)
                            uploadImageAndText(imageBitmap, uid) // auth UID로 변경해야 함
                        }
                    }
                }
            }

            resultText.text = "진단 중"
        }


    }


    private fun rotateImageIfNeeded(photoPath: String, bitmap: Bitmap): Bitmap {
        val exif = try {
            ExifInterface(photoPath)
        } catch (e: IOException) {
            e.printStackTrace()
            return bitmap
        }
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun uploadImageAndText(bitmap: Bitmap, text: String) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), baos.toByteArray())
        val part = MultipartBody.Part.createFormData("image", "image.jpg", requestBody)
        val textPart = RequestBody.create("text/plain".toMediaTypeOrNull(), text)

        apiService.uploadImageAndText(part, textPart).enqueue(object : Callback<ServerResponse> {
            override fun onResponse(call: Call<ServerResponse>, response: Response<ServerResponse>) {
                if (response.isSuccessful) {
                    // 서버 응답 성공 처리
                    val serverResponse = response.body()


                    if(serverResponse?.pythonResult == "1face"){
                        Toast.makeText(
                            context,
                            "한 사람의 얼굴만 잘 나오는 사진으로 다시 진행해주세요.",
                            Toast.LENGTH_SHORT

                        ).show()
                    }else if(serverResponse?.pythonResult == "error"){
                        Toast.makeText(
                            context,
                            "네트워크 에러입니다. 나중에 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }else{
                        Toast.makeText(
                            context,
                            "진단 완료! 결과페이지에서 확인해주세요.",
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

                    resultText.text = serverResponse?.pythonResult

                }else {
                    // 서버 응답 오류 처리
                    Toast.makeText(context, "Upload failed: ${response.message()}", Toast.LENGTH_SHORT).show()
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

}