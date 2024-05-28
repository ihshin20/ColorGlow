package com.example.mycolor.activity

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycolor.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

data class Comment(
    val Body: String = "",
    val Date: com.google.firebase.Timestamp? = null,
    val UID: String = ""
)

class CommentsAdapter(private val commentsList: List<Comment>) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val detailCommentDateTextView: TextView = view.findViewById(R.id.DetailCommentDateTextView)
        val detailCommentNameTextView: TextView = view.findViewById(R.id.detailCommentNameTextView)
        val detailCommentTextView: TextView = view.findViewById(R.id.detailCommentTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]

        // Date 처리
        comment.Date?.let {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            holder.detailCommentDateTextView.text = dateFormat.format(it.toDate())
        } ?: run {
            holder.detailCommentDateTextView.text = "Unknown Date"
        }

        // Body 처리
        holder.detailCommentTextView.text = comment.Body

        // UID를 통해 사용자 이름 가져오기
        val db = FirebaseFirestore.getInstance()
        db.collection("User").document(comment.UID).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    holder.detailCommentNameTextView.text = document.getString("name") ?: "익명"
                } else {
                    holder.detailCommentNameTextView.text = "익명"
                }
            }
            .addOnFailureListener { exception ->
                Log.w("CommentsAdapter", "Error getting user document: ", exception)
                holder.detailCommentNameTextView.text = "익명"
            }
    }

    override fun getItemCount(): Int {
        return commentsList.size
    }
}
