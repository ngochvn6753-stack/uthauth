package com.example.uthauth

import android.util.Log

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper

object TaskRepo {
    private fun ref(): DatabaseReference? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        return FirebaseDatabase.getInstance()
            .getReference("users").child(uid).child("tasks")
    }

    fun add(context: Context, task: Task) {
        val taskRef = ref() ?: return
        val newRef = taskRef.push()
        val taskId = newRef.key ?: return

        val newTask = task.copy(id = taskId)

        newRef.setValue(newTask)
            .addOnSuccessListener {

                Firebase.analytics.logEvent(
                    "add_task",
                    Bundle().apply {
                        putString("tag", newTask.tag)
                    }
                )

                if (!newTask.done) {
                    TaskAlarmScheduler.schedule(
                        context = context.applicationContext,
                        taskId = taskId,
                        title = newTask.title,
                        date = newTask.date,
                        time = newTask.time
                    )
                }

                Log.d("TaskRepo", "Đã tạo công việc: ${newTask.title}")
            }
            .addOnFailureListener { e ->
                Log.e("TaskRepo", "Không thể tạo công việc", e)
            }
    }

    fun toggle(context: Context, task: Task) {
        if (task.id.isBlank()) return

        val newDone = !task.done

        val updates = mapOf<String, Any>(
            "done" to newDone,
            "completedAt" to if (newDone) {
                System.currentTimeMillis()
            } else {
                0L
            }
        )

        ref()?.child(task.id)?.updateChildren(updates)
            ?.addOnSuccessListener {
                if (newDone) {
                    TaskAlarmScheduler.cancel(context, task.id)
                } else {
                    TaskAlarmScheduler.schedule(
                        context = context,
                        taskId = task.id,
                        title = task.title,
                        date = task.date,
                        time = task.time
                    )
                }

                Firebase.analytics.logEvent(
                    "toggle_task",
                    Bundle().apply {
                        putBoolean("done", newDone)
                    }
                )
            }
            ?.addOnFailureListener { e ->
                Log.e("TaskRepo", "Lỗi cập nhật trạng thái", e)
            }
    }


    fun delete(context: Context, task: Task) {
        if (task.id.isBlank()) return

        ref()?.child(task.id)?.removeValue()
            ?.addOnSuccessListener {
                TaskAlarmScheduler.cancel(context, task.id)
                Log.d("TaskRepo", "Đã xóa: ${task.title}")
            }
            ?.addOnFailureListener { e ->
                Log.e("TaskRepo", "Lỗi xóa công việc", e)
            }
    }


    fun update(
        context: Context,
        task: Task,
        newTitle: String,
        newTag: String,
        newTime: String,
        newDate: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (task.id.isBlank()) {
            onError("Không tìm thấy ID công việc")
            return
        }

        val taskRef = ref()

        if (taskRef == null) {
            onError("Bạn chưa đăng nhập")
            return
        }

        val updates = mapOf<String, Any>(
            "title" to newTitle,
            "tag" to newTag,
            "time" to newTime,
            "date" to newDate
        )

        taskRef.child(task.id)
            .updateChildren(updates)
            .addOnSuccessListener {
                TaskAlarmScheduler.cancel(context, task.id)

                if (!task.done) {
                    TaskAlarmScheduler.schedule(
                        context = context,
                        taskId = task.id,
                        title = newTitle,
                        date = newDate,
                        time = newTime
                    )
                }

                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Cập nhật thất bại")
            }
    }



    fun listen(onChange: (List<Task>) -> Unit): () -> Unit {
        val r = ref() ?: return {}
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    child.getValue(Task::class.java)?.copy(id = child.key ?: "")
                }.sortedBy { it.time }
                onChange(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskRepo", "Lỗi: ${error.message}")
            }
        }
        r.addValueEventListener(listener)
        return { r.removeEventListener(listener) }
    }

    fun addSubTask(taskId: String, title: String) {
        if (taskId.isBlank() || title.isBlank()) return

        val taskRef = ref() ?: return
        val subRef = taskRef.child(taskId)
            .child("subtasks")
            .push()

        val id = subRef.key ?: return

        subRef.setValue(
            SubTask(
                id = id,
                title = title.trim(),
                done = false
            )
        )
    }

    fun toggleSubTask(taskId: String, subTask: SubTask) {
        if (taskId.isBlank() || subTask.id.isBlank()) return

        ref()?.child(taskId)
            ?.child("subtasks")
            ?.child(subTask.id)
            ?.child("done")
            ?.setValue(!subTask.done)
    }

    fun listenSubTasks(
        taskId: String,
        onChange: (List<SubTask>) -> Unit
    ): () -> Unit {
        if (taskId.isBlank()) return {}

        val subRef = ref()
            ?.child(taskId)
            ?.child("subtasks")
            ?: return {}

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { child ->
                    child.getValue(SubTask::class.java)
                        ?.copy(id = child.key ?: "")
                }
                onChange(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskRepo", "Lỗi tải việc nhỏ", error.toException())
            }
        }

        subRef.addValueEventListener(listener)

        return {
            subRef.removeEventListener(listener)
        }
    }
    fun saveNote(
        taskId: String,
        note: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val r = ref()
        if (r == null || taskId.isBlank()) {
            onError("Không tìm thấy công việc hoặc tài khoản")
            return
        }

        r.child(taskId).child("note")
            .setValue(note)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener {
                onError(it.message ?: "Không lưu được ghi chú")
            }
    }

    fun listenNote(
        taskId: String,
        onChange: (String) -> Unit
    ): () -> Unit {
        val r = ref()?.child(taskId)?.child("note")
            ?: return {}

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(snapshot.getValue(String::class.java) ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaskRepo", "Lỗi đọc ghi chú: ${error.message}")
            }
        }

        r.addValueEventListener(listener)

        return { r.removeEventListener(listener) }
    }


}