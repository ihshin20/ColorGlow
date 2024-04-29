package com.example.mycolor.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycolor.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ResultFragment : Fragment() {
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var resultsAdapter: ResultsAdapter

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
        resultsAdapter = ResultsAdapter()
        resultsRecyclerView.layoutManager = LinearLayoutManager(context)
        resultsRecyclerView.adapter = resultsAdapter
    }


    fun fetchAllDiagnosticResults() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
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

    inner class ResultsAdapter : RecyclerView.Adapter<ResultsAdapter.ResultViewHolder>() {
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

            fun bind(result: DiagnosticResult) {
                dateTextView.text = result.date
                resultTextView.text = result.description
            }
        }
    }


    data class DiagnosticResult(
        val date: String,
        val description: String,
        val timestamp: Date // This is now guaranteed to be non-null
    )
}
