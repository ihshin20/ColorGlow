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
    val comments: MutableList<Comment> = mutableListOf()
)

data class Comment(
    val uid: String = "",
    val author: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class CommunityFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var postEditText: EditText
    private lateinit var postButton: Button
    private lateinit var adapter: PostAdapter
    private var postsList = mutableListOf<Post>()
    private lateinit var auth: FirebaseAuth
    private var uid: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        val db = FirebaseFirestore.getInstance()
        val uid = post.uid
        if (uid.isNotEmpty()) {
            db.collection("Posts").document(uid)
                .collection("userPosts")
                .add(post)
                .addOnSuccessListener { documentReference ->
                    Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")
                    postEditText.text.clear()
                    postsList.add(post)
                    adapter.notifyItemInserted(postsList.size - 1)
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error adding document", e)
                }
        } else {
            Log.w("Firestore", "User UID is empty, cannot upload post.")
        }
    }

    private fun loadPosts() {
        val db = FirebaseFirestore.getInstance()
        val user = auth.currentUser
        if (user != null) {
            uid = user.uid
            db.collection("Posts").document(uid!!)
                .collection("userPosts")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val post = document.toObject(Post::class.java)
                        postsList.add(post)
                    }
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.w("Firestore", "Error getting documents.", exception)
                }
        }
    }
}


    class PostAdapter(private val posts: MutableList<Post>) :
        RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

        class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val postTextView: TextView = view.findViewById(R.id.postTextView)
            val commentEditText: EditText = view.findViewById(R.id.commentEditText)
            val commentButton: Button = view.findViewById(R.id.commentButton)
            val commentsRecyclerView: RecyclerView = view.findViewById(R.id.commentsRecyclerView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
            return PostViewHolder(view)
        }

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = posts[position]
            holder.postTextView.text = post.content

            val commentsAdapter = CommentAdapter(post.comments)
            holder.commentsRecyclerView.layoutManager =
                LinearLayoutManager(holder.commentsRecyclerView.context)
            holder.commentsRecyclerView.adapter = commentsAdapter

            holder.postTextView.setOnClickListener {
                // Handle the content TextView click event
                Toast.makeText(
                    holder.postTextView.context,
                    "Content clicked: ${post.content}",
                    Toast.LENGTH_SHORT
                ).show()
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
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
            return CommentViewHolder(view)
        }

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            holder.commentTextView.text = comments[position].content
        }

        override fun getItemCount(): Int = comments.size
    }


