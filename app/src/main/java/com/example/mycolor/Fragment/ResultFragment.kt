package com.example.mycolor.Fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mycolor.R
import com.example.mycolor.activity.DetailResultActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class ResultFragment : Fragment() {
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var resultsAdapter: ResultsAdapter

    interface OnItemClickListener {
        fun onItemClick(result: DiagnosticResult)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView)
        setupRecyclerView()
        fetchAllDiagnosticResults()
    }

    private fun setupRecyclerView() {
        //resultsAdapter = ResultsAdapter()
        resultsAdapter = ResultsAdapter(object : OnItemClickListener {
            override fun onItemClick(result: DiagnosticResult) {
                val intent = Intent(context, DetailResultActivity::class.java).apply {
                    putExtra("result", result.description)
                    putExtra("date", result.date)
                    val timestamp = result.timestamp.time // Date 객체에서 밀리초로 시간 가져오기
                    putExtra("timestamp", timestamp)
                }
                startActivity(intent)
            }
        })
        resultsRecyclerView.layoutManager = LinearLayoutManager(context)
        resultsRecyclerView.adapter = resultsAdapter
    }


    fun fetchAllDiagnosticResults() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            context?.let {
                Toast.makeText(it, "User not logged in.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val db = FirebaseFirestore.getInstance()
        val diagnosticRef = db.collection("User").document(userId).collection("results")

        diagnosticRef.orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "No diagnostic results found.", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val results = documents.mapNotNull { document ->
                        val actualDate = document.getTimestamp("date")?.toDate()
                        if (actualDate != null) {
                            val formattedDate =
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(
                                    actualDate
                                )
                            val result = document.getString("result") ?: "No result available"
                            DiagnosticResult(formattedDate, result, actualDate)
                        } else {
                            null // Skip this entry if the date is null
                        }
                    }
                    if (results.isNotEmpty()) {
                        resultsAdapter.updateResults(results)
                    } else {
                        Toast.makeText(context, "No valid entries found.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Error loading results: ${exception.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    inner class ResultsAdapter(private val listener: OnItemClickListener) : RecyclerView.Adapter<ResultsAdapter.ResultViewHolder>() {
        private var resultsList: MutableList<DiagnosticResult> = mutableListOf()

        fun updateResults(results: List<DiagnosticResult>) {
            resultsList.clear()
            resultsList.addAll(results)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_diagnostic_result, parent, false)
            return ResultViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
            holder.bind(resultsList[position])
        }

        override fun getItemCount(): Int = resultsList.size

        inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView) ?: throw IllegalStateException("Date TextView must not be null")
            private val resultTextView: TextView = itemView.findViewById(R.id.resultTextView) ?: throw IllegalStateException("Result TextView must not be null")
            private val resultImage: ImageView = itemView.findViewById(R.id.resultImage)
            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(resultsList[position])
                    }
                }
            }


            fun bind(result: DiagnosticResult) {
                dateTextView.text = result.date
                resultTextView.text = result.description

                loadImage(result.timestamp, resultImage, itemView.context)

            }
        }
    }


    data class DiagnosticResult(
        val date: String,
        val description: String,
        val timestamp: Date // This is now guaranteed to be non-null
    )

    fun loadImage(date: Date, imageView: ImageView, context: Context) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val imagePath = "UserImages/$userId/$date.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child(imagePath)

        // Glide를 사용하여 ImageView에 이미지 로드
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(context)
                .load(uri)
                .into(imageView)
        }.addOnFailureListener { exception ->
            // 이미지 로드 실패 처리
            Log.e("FirebaseStorage", "Error loading image", exception)
            //imageView.setImageResource(R.drawable.joy_redvelvet) // 예를 들어 default_image
        }
    }

}


