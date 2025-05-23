package com.example.todoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter
    private lateinit var db: FirebaseFirestore
    private val tasks = mutableListOf<Task>()

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.todoRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(tasks,
            onDoneClick = { task -> markTaskDone(task) },
            onDeleteClick = { task -> deleteTask(task) }
        )
        recyclerView.adapter = adapter

        loadTasks()

        val addButton: Button = findViewById(R.id.addButton)
        addButton.setOnClickListener {
            showAddTaskDialog()
        }

        val logoutButton: Button = findViewById(R.id.btnLogout)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadTasks() {
        db.collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                tasks.clear()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    tasks.add(task)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun markTaskDone(task: Task) {
        val updatedTask = task.copy(done = true)
        db.collection("tasks").document(task.id)
            .set(updatedTask)
            .addOnSuccessListener {
                val index = tasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    tasks[index] = updatedTask
                    adapter.notifyItemChanged(index)
                }
            }
    }

    private fun deleteTask(task: Task) {
        db.collection("tasks").document(task.id)
            .delete()
            .addOnSuccessListener {
                val index = tasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    tasks.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
            }
    }

    private fun showAddTaskDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Nouvelle tâche")

        val input = EditText(this)
        input.hint = "Entrez le titre de la tâche"
        builder.setView(input)

        builder.setPositiveButton("Ajouter") { _, _ ->
            val title = input.text.toString()
            if (title.isNotEmpty()) {
                val newTask = Task(
                    id = db.collection("tasks").document().id,
                    title = title,
                    done = false
                )
                db.collection("tasks").document(newTask.id)
                    .set(newTask)
                    .addOnSuccessListener {
                        tasks.add(newTask)
                        adapter.notifyDataSetChanged()
                    }
            }
        }
        builder.setNegativeButton("Annuler", null)
        builder.show()
    }
}
