package com.example.mycolor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

data class Post(
    val Body: String = "바디",
    val Date: Timestamp = Timestamp.now(),
    val Like: Int = -1,
    val Title: String = "타이틀",
    val Tone: String = "톤",
    val UID: String = "uid"
)

class CommunityFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private val postsList = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.postsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PostAdapter(postsList)
        recyclerView.adapter = adapter

        val writeBtn = view.findViewById<Button>(R.id.writeBtn)
        writeBtn.setOnClickListener {
            // 인텐트 글쓰기 액티비티
        }

        fetchPosts()
    }

    private fun fetchPosts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Posts")
            .orderBy("Date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val post = document.toObject(Post::class.java)
                    postsList.add(post)
                    Log.d("CommunityFragment", "Post fetched: $post")
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w("CommunityFragment", "Error getting documents: ", exception)
            }
    }

    class PostAdapter(private val posts: MutableList<Post>) :
        RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

        class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val uploadedDateTextView: TextView = view.findViewById(R.id.uploadedDateTextView)
            val uploaderTextView: TextView = view.findViewById(R.id.uploaderTextView)
            val titleTextView: TextView = view.findViewById(R.id.titleTextView)
            val uploaderToneTextView: TextView = view.findViewById(R.id.uploaderToneTextView)
            val likeTextView: TextView = view.findViewById(R.id.likeTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.community_item, parent, false)
            return PostViewHolder(view)
        }

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = posts[position]
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            holder.uploadedDateTextView.text = dateFormat.format(post.Date.toDate())
            holder.uploadedDateTextView.textSize = 18f
            holder.titleTextView.text = post.Title
            holder.uploaderToneTextView.text = post.Tone
            holder.likeTextView.text = post.Like.toString()

            val db = FirebaseFirestore.getInstance()
            db.collection("User").document(post.UID).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        holder.uploaderTextView.text = document.getString("name") ?: "익명"
                    } else {
                        holder.uploaderTextView.text = "익명"
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w("PostAdapter", "Error getting user document: ", exception)
                    holder.uploaderTextView.text = "익명"
                }
        }

        override fun getItemCount(): Int = posts.size
    }
}
