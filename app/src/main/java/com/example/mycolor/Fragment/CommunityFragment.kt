package com.example.mycolor

import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class Post(
    val uid: String = "",
    val author: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val comments: MutableList<Comment> = mutableListOf(),
    var likes: Int = 0  // 추가된 필드
)

data class Comment(
    val uid: String = "",
    val author: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var likes: Int = 0  // 추가된 필드
)

class CommunityFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var postEditText: EditText
    private lateinit var postButton: Button
    private lateinit var adapter: PostAdapter
    private var postsList = mutableListOf<Post>()
    private lateinit var auth: FirebaseAuth
    private var uid: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        postEditText = view.findViewById(R.id.postEditText)
        postButton = view.findViewById(R.id.postButton)

        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PostAdapter(postsList)
        recyclerView.adapter = adapter

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            uid = user.uid
            Log.d("FirebaseAuth", "User UID: $uid")
        } else {
            Log.d("FirebaseAuth", "No user is currently signed in.")
        }

        postButton.setOnClickListener {
            val content = postEditText.text.toString()
            if (content.isNotEmpty()) {
                val newPost = Post(
                    uid = uid ?: "",
                    author = auth.currentUser?.displayName ?: "Anonymous",
                    content = content
                )
                uploadPost(newPost)
            }
        }

        loadPosts()
    }

    private fun uploadPost(post: Post) {
        FirebaseFirestore.getInstance().collection("Posts").add(post)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Post uploaded successfully", Toast.LENGTH_SHORT).show()
                    postEditText.setText("")  // Clear the input box after posting
                } else {
                    Toast.makeText(context, "Failed to upload post", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadPosts() {
        FirebaseFirestore.getInstance().collection("Posts").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                postsList.clear()
                snapshots?.documents?.forEach { document ->
                    document.toObject(Post::class.java)?.let { post ->
                        postsList.add(post)
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }
}

class PostAdapter(private val posts: MutableList<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postTextView: TextView = view.findViewById(R.id.postTextView)
        val likeButton: Button = view.findViewById(R.id.likeButton)
        val likeCountTextView: TextView = view.findViewById(R.id.likeCountTextView)
        val commentEditText: EditText = view.findViewById(R.id.commentEditText)
        val commentButton: Button = view.findViewById(R.id.commentButton)
        val commentsRecyclerView: RecyclerView = view.findViewById(R.id.commentsRecyclerView)

        init {
            // RecyclerView의 레이아웃 매니저를 여기서 설정합니다.
            commentsRecyclerView.layoutManager = LinearLayoutManager(view.context)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.postTextView.text = post.content
        holder.likeCountTextView.text = "${post.likes} Likes"

        val commentsAdapter = CommentAdapter(post.comments)
        holder.commentsRecyclerView.adapter = commentsAdapter

        holder.likeButton.setOnClickListener {
            post.likes++
            updateLikes(post)
            holder.likeCountTextView.text = "${post.likes} Likes"
        }

        holder.commentButton.setOnClickListener {
            val commentContent = holder.commentEditText.text.toString()
            if (commentContent.isNotEmpty()) {
                val newComment = Comment(
                    uid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    author = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous",
                    content = commentContent
                )
                addCommentToPost(post, newComment)
                holder.commentEditText.setText("")  // Clear the input box after posting
            }
        }
    }

    override fun getItemCount(): Int = posts.size

    private fun updateLikes(post: Post) {
        val postRef = FirebaseFirestore.getInstance().collection("Posts").document(post.uid)
        postRef.update("likes", post.likes)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Handle error
                }
            }
    }

    private fun addCommentToPost(post: Post, comment: Comment) {
        val postRef = FirebaseFirestore.getInstance().collection("Posts").document(post.uid)
        post.comments.add(comment)
        postRef.update("comments", post.comments)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    notifyDataSetChanged()
                } else {
                    // Handle error
                }
            }
    }
}

class CommentAdapter(private val comments: MutableList<Comment>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val commentTextView: TextView = view.findViewById(R.id.commentTextView)
        val commentLikeButton: Button = view.findViewById(R.id.commentLikeButton)
        val commentLikeCountTextView: TextView = view.findViewById(R.id.commentLikeCountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.commentTextView.text = comment.content
        holder.commentLikeCountTextView.text = "${comment.likes} Likes"

        holder.commentLikeButton.setOnClickListener {
            comment.likes++
            updateCommentLikes(comment)
            holder.commentLikeCountTextView.text = "${comment.likes} Likes"
        }
    }

    override fun getItemCount(): Int = comments.size

    private fun updateCommentLikes(comment: Comment) {
        val postRef = FirebaseFirestore.getInstance().collection("Posts")
        // assuming you have a way to get the parent post id or reference
        // you can add additional code here to properly reference and update the likes of the comment
    }
}


