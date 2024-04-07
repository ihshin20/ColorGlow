package com.example.mycolor.Fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mycolor.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

class HomeFragment : Fragment(R.id.homeFragment) {

    private lateinit var imageView: ImageView

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        private const val REQUEST_GALLERY_ACCESS = 102

        // server IP 맞춰서 수정해야 함
        private const val BASE_URL = "http://13.125.218.109:3000/"
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

        apiService = retrofit.create(ApiService::class.java)

//        val retrofit = Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()

        apiService = retrofit.create(ApiService::class.java)

        imageView = view.findViewById(R.id.imageView)

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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    // Camera
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let {
                        imageView.setImageBitmap(it)
                        uploadImageAndText(it, "안녕")
                    }
                }
                REQUEST_GALLERY_ACCESS -> {
                    // Gallery
                    val selectedImageUri: Uri? = data?.data
                    selectedImageUri?.let {
                        imageView.setImageURI(it)
                        context?.contentResolver?.openInputStream(it)?.let { inputStream ->
                            val imageBitmap = BitmapFactory.decodeStream(inputStream)
                            uploadImageAndText(imageBitmap, "안녕")
                        }
                    }
                }
            }
        }
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
                    Toast.makeText(
                        context,
                        "Upload successful: ${serverResponse?.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 서버 응답에 포함된 추가 데이터 사용
                    Log.d(
                        "ServerResponse",
                        "Text Data: ${serverResponse?.textData}, Python Result: ${serverResponse?.pythonResult}"
                    )
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
}
