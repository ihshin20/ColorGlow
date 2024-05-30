package com.example.mycolor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycolor.activity.CommunityDetailActivity
import com.example.mycolor.activity.WriteActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

data class Post(
    val Body: String = "",
    val Date: Timestamp = Timestamp.now(),
    val Like: Int = 0,
    val Title: String = "",
    val Tone: String = "",
    val UID: String = "",
    var documentId: String = "" // 문서 ID 추가
)

class CommunityFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private val postsList = mutableListOf<Post>()
    private lateinit var radioGroup: RadioGroup
    private lateinit var loadingDialog: AlertDialog
    private var selectedToneFilter: String? = null

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
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PostAdapter(postsList) { documentId ->
            val intent = Intent(context, CommunityDetailActivity::class.java)
            intent.putExtra("DOCUMENT_ID", documentId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        val writeBtn = view.findViewById<Button>(R.id.writeBtn)
        writeBtn.setOnClickListener {
            val intent = Intent(context, WriteActivity::class.java)
            startActivity(intent)
        }

        radioGroup = view.findViewById(R.id.radioGroup)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedToneFilter = when (checkedId) {
                R.id.radioAll -> null
                R.id.radioSpring -> "Spring"
                R.id.radioSummer -> "Summer"
                R.id.radioAutumn -> "Autumn"
                R.id.radioWinter -> "Winter"
                else -> null
            }
            fetchPosts(selectedToneFilter)
        }

        showLoadingDialog()  // Show loading dialog when view is created
        fetchPosts(selectedToneFilter)  // Load the posts
    }

    override fun onResume() {
        super.onResume()
        fetchPosts(selectedToneFilter)
    }

    private fun fetchPosts(toneFilter: String?) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Posts")
            .orderBy("Date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                postsList.clear()
                for (document in documents) {
                    val post = document.toObject(Post::class.java).apply {
                        documentId = document.id // 문서 ID 저장
                    }
                    if (toneFilter == null || post.Tone.contains(toneFilter, ignoreCase = true)) {
                        postsList.add(post)
                        Log.d("CommunityFragment", "Post fetched: $post")
                    }
                }
                adapter.notifyDataSetChanged()
                hideLoadingDialog()
            }
            .addOnFailureListener { exception ->
                Log.w("CommunityFragment", "Error getting documents: ", exception)
                hideLoadingDialog()
            }
    }

    class PostAdapter(
        private val posts: MutableList<Post>,
        private val itemClickListener: (String) -> Unit
    ) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

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
            val formattedDate = dateFormat.format(post.Date.toDate())
            holder.uploadedDateTextView.text = formattedDate.toString()
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

            holder.itemView.setOnClickListener {
                itemClickListener(post.documentId)
            }
        }

        override fun getItemCount(): Int = posts.size
    }

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.loading_dialog, null)
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
