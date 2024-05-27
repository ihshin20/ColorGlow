package com.example.mycolor

import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
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
    val timestamp: Long = System.currentTimeMillis()
)


class CommunityFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var postEditText: EditText
    private lateinit var postButton: Button
    private lateinit var adapter: PostAdapter
    private var postsList = mutableListOf<Post>()

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

        postButton.setOnClickListener {
            val content = postEditText.text.toString()
            if (content.isNotEmpty()) {
                val newPost = Post(uid = FirebaseAuth.getInstance().currentUser?.uid ?: "", author = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous", content = content)
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.post_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.postTextView.text = posts[position].content
    }

    override fun getItemCount(): Int = posts.size
}
