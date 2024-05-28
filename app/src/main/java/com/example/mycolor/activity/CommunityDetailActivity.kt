package com.example.mycolor.activity

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mycolor.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Locale

class CommunityDetailActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var detailLikeTextView: TextView
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private val commentsList = mutableListOf<Comment>()
    private lateinit var loadingDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_community_detail)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupLoadingDialog()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailCommunityLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val detailDateTextView = findViewById<TextView>(R.id.detailDateTextView)
        val detailNameTextView = findViewById<TextView>(R.id.detailNameTextView)
        val detailTitleTextView = findViewById<TextView>(R.id.detailTitleTextView)
        val detailBodyTextView = findViewById<TextView>(R.id.detailBodyTextView)
        val detailToneTextView = findViewById<TextView>(R.id.detailToneTextView)
        detailLikeTextView = findViewById(R.id.detailLikeTextView)
        val likeBtn = findViewById<Button>(R.id.likeBtn)
        val commentBtn = findViewById<Button>(R.id.CommentBtn)
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        val imgView = findViewById<ImageView>(R.id.detailCommunityImageView)

        showLoadingDialog()

        val documentId = intent.getStringExtra("DOCUMENT_ID") ?: return
        Log.d("CommunityDetailActivity", "Document ID: $documentId")

        // Firestore에서 해당 문서 ID로 데이터를 가져와서 화면에 표시
        db.collection("Posts").document(documentId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val post = document.data
                    post?.let {
                        detailTitleTextView.text = it["Title"] as String
                        detailBodyTextView.text = it["Body"] as String
                        detailToneTextView.text = it["Tone"] as String
                        detailLikeTextView.text = (it["Like"] as Long).toString()

                        val timestamp = it["Date"] as com.google.firebase.Timestamp
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        detailDateTextView.text = dateFormat.format(timestamp.toDate())

                        val uid = it["UID"] as String
                        // Firestore에서 UID로 사용자의 이름을 가져와서 설정
                        db.collection("User").document(uid).get()
                            .addOnSuccessListener { userDocument ->
                                if (userDocument != null && userDocument.exists()) {
                                    detailNameTextView.text = userDocument.getString("name") ?: "익명"
                                } else {
                                    detailNameTextView.text = "익명"
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.w("CommunityDetailActivity", "Error getting user document: ", exception)
                                detailNameTextView.text = "익명"
                            }

                        // Firebase Storage에서 이미지 로드
                        loadImageFromFirebase(documentId, imgView)
                    }
                } else {
                    Log.d("CommunityDetailActivity", "No such document")

                }
            }
            .addOnFailureListener { exception ->
                Log.w("CommunityDetailActivity", "Error getting document: ", exception)
                hideLoadingDialog()
            }

        likeBtn.setOnClickListener {
            handleLikeButtonClick(documentId)
        }

        commentBtn.setOnClickListener {
            showCommentDialog(documentId)
        }

        setupCommentsRecyclerView()
        fetchComments(documentId)
    }

    private fun setupLoadingDialog() {
        loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.loading_dialog)
        loadingDialog.setCancelable(false)
    }

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)

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
        loadingDialog.dismiss()
    }

    private fun setupCommentsRecyclerView() {
        commentsAdapter = CommentsAdapter(commentsList)
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.adapter = commentsAdapter
    }

    private fun fetchComments(documentId: String) {
        db.collection("Posts").document(documentId).collection("Comments")
            .orderBy("Date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("CommunityDetailActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    commentsList.clear()
                    for (document in snapshot.documents) {
                        val comment = document.toObject(Comment::class.java)
                        if (comment != null) {
                            commentsList.add(comment)
                        }
                    }
                    commentsAdapter.notifyDataSetChanged()

                }
            }
    }

    private fun loadImageFromFirebase(documentId: String, imageView: ImageView) {
        val storageRef = storage.reference.child("PostImages/$documentId.jpg")
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            Log.d("CommunityDetailActivity", "Image URL: $uri")
            Glide.with(this)
                .load(uri)
                .override(1000, 1000) // 여기에 이미지 크기 설정
                .fitCenter() // 원본 비율을 유지하면서 이미지가 뷰에 맞게 조정
                .into(imageView)
            imageView.visibility = View.VISIBLE // 이미지 로드 성공 시 visibility 변경
            hideLoadingDialog() // 이미지 로드 후 로딩 다이얼로그 숨김
        }.addOnFailureListener { exception ->
            Log.w("CommunityDetailActivity", "Error getting image: ", exception)
            hideLoadingDialog()
        }
    }


    private fun handleLikeButtonClick(documentId: String) {
        val uid = auth.currentUser?.uid ?: return

        // "Likes" 서브 컬렉션에서 현재 사용자의 UID가 있는지 확인
        val likeRef = db.collection("Posts").document(documentId).collection("Likes").document(uid)
        likeRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // 이미 좋아요를 누른 경우, 좋아요 취소
                    likeRef.delete()
                        .addOnSuccessListener {
                            // "Like" 필드의 값을 1 줄임
                            db.collection("Posts").document(documentId).update("Like", FieldValue.increment(-1))
                                .addOnSuccessListener {
                                    updateLikeCount(documentId)
                                    Toast.makeText(this, "좋아요 취소", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.w("CommunityDetailActivity", "Error removing like: ", e)
                        }
                } else {
                    // 좋아요를 누르지 않은 경우, 좋아요 추가
                    likeRef.set(hashMapOf("UID" to uid))
                        .addOnSuccessListener {
                            // "Like" 필드의 값을 1 증가시킴
                            db.collection("Posts").document(documentId).update("Like", FieldValue.increment(1))
                                .addOnSuccessListener {
                                    updateLikeCount(documentId)
                                    Toast.makeText(this, "좋아요", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.w("CommunityDetailActivity", "Error adding like: ", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("CommunityDetailActivity", "Error checking like: ", e)
            }
    }

    private fun updateLikeCount(documentId: String) {
        db.collection("Posts").document(documentId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val likeCount = document.getLong("Like") ?: 0
                    detailLikeTextView.text = likeCount.toString()
                }
            }
            .addOnFailureListener { e ->
                Log.w("CommunityDetailActivity", "Error getting like count: ", e)
            }
    }

    private fun showCommentDialog(documentId: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_comment, null)
        dialogBuilder.setView(dialogView)

        val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText)
        val submitButton = dialogView.findViewById<Button>(R.id.submitCommentButton)

        val alertDialog = dialogBuilder.create()
        submitButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()
            if (commentText.isNotEmpty()) {
                val comment = hashMapOf(
                    "Body" to commentText,
                    "Date" to FieldValue.serverTimestamp(),
                    "UID" to auth.currentUser?.uid
                )
                db.collection("Posts").document(documentId).collection("Comments").add(comment)
                    .addOnSuccessListener {
                        Toast.makeText(this, "댓글이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Log.w("CommunityDetailActivity", "Error adding comment: ", e)
                        Toast.makeText(this, "댓글 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }
}
