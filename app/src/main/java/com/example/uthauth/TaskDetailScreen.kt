
package com.example.uthauth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
@Composable
fun TaskDetailScreen(
    task: Task,
    onBack: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var newTitle by remember(task.id) {
        mutableStateOf(task.title)
    }
    var newTag by remember(task.id) {
        mutableStateOf(task.tag)
    }
    var newTime by remember(task.id) {
        mutableStateOf(task.time)
    }
    var newDate by remember(task.id) {
        mutableStateOf(task.date)
    }

    val context = LocalContext.current
    var showTimePicker by remember {
        mutableStateOf(false)
    }

    var editError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Ghi chú tạm thời trên giao diện
    var note by remember(task.id) { mutableStateOf("") }
    var noteInput by remember(task.id) { mutableStateOf("") }
    var showNoteField by remember { mutableStateOf(false) }
    var isSavingNote by remember { mutableStateOf(false) }
    var noteError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(task.id) {
        val stop = TaskRepo.listenNote(task.id) {
            note = it
        }
        onDispose { stop() }
    }

    val blue = Color(0xFF3355FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            //.statusBarsPadding()
            // .navigationBarsPadding()
            .padding(20.dp)
    ) {

        // Thanh trên cùng
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Quay lại", color = blue)
            }

            Box {
                TextButton(onClick = { showMenu = true }) {
                    Text("⋮", fontSize = 26.sp, color = Color.DarkGray)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sửa công việc") },
                        onClick = {
                            showMenu = false
                            newTitle = task.title
                            newTag = task.tag
                            newTime = task.time
                            newDate = task.date
                            editError = null
                            showEditDialog = true
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Xóa công việc", color = Color.Red)
                        },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = task.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Danh mục: ${task.tag}",
            fontSize = 16.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Thời gian: ${task.time}",
            fontSize = 16.sp
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = "Ngày thực hiện: ${
                task.date.ifBlank { "Chưa đặt ngày" }
            }",
            fontSize = 16.sp
        )
        Spacer(Modifier.height(20.dp))

        Text(
            text = if (task.done)
                "Trạng thái: Đã hoàn thành"
            else
                "Trạng thái: Chưa hoàn thành",
            color = if (task.done) blue else Color.Gray
        )

        Spacer(Modifier.height(32.dp))

        // Ghi chú

        // Ghi chú
        HorizontalDivider()

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Ghi chú",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        if (note.isNotBlank() && !showNoteField) {
            Text(
                text = note,
                fontSize = 15.sp
            )

            TextButton(
                onClick = {
                    noteInput = note
                    showNoteField = true
                }
            ) {
                Text("Sửa ghi chú")
            }
        } else if (!showNoteField) {
            TextButton(
                onClick = {
                    noteInput = ""
                    showNoteField = true
                }
            ) {
                Text("+ Thêm ghi chú")
            }
        }

        if (showNoteField) {

            OutlinedTextField(
                value = noteInput,
                onValueChange = {
                    noteInput = it
                },
                label = {
                    Text("Nội dung ghi chú")
                },
                placeholder = {
                    Text("Nhập ghi chú của bạn...")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            noteError?.let {
                Text(it, color = Color.Red)
            }

            Row {
                TextButton(
                    enabled = !isSavingNote,
                    onClick = {
                        showNoteField = false
                        noteError = null
                    }
                ) {
                    Text("Hủy")
                }

                Button(
                    enabled = !isSavingNote,
                    onClick = {
                        isSavingNote = true
                        noteError = null

                        TaskRepo.saveNote(
                            taskId = task.id,
                            note = noteInput.trim(),
                            onSuccess = {
                                note = noteInput.trim()
                                isSavingNote = false
                                showNoteField = false
                            },
                            onError = {
                                noteError = it
                                isSavingNote = false
                            }
                        )
                    }
                ) {
                    Text(
                        if (isSavingNote) "Đang lưu..."
                        else "Lưu ghi chú"
                    )
                }
            }
        }
        // Xác nhận xóa
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteDialog = false
                },
                title = { Text("Xóa công việc?") },
                text = {
                    Text("Bạn có chắc muốn xóa công việc này không?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            TaskRepo.delete(context,task)
                            showDeleteDialog = false
                            onBack()
                        }
                    ) {
                        Text("Xóa", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text("Hủy")
                    }
                }
            )
        }

        // Chỉnh sửa công việc
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isSaving) showEditDialog = false
                },
                title = { Text("Sửa công việc") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Tên công việc") },
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        Text("Danh mục")

                        listOf("Học tập",
                            "Cuộc sống",
                            "Làm việc",
                            "Sức khỏe",
                            "Giải trí",
                            "Khác").forEach { tag ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = newTag == tag,
                                    onClick = { newTag = tag }
                                )
                                Text(tag)
                            }
                        }


                        OutlinedTextField(
                            value = newTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Thời gian") },

                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        showTimePicker = true
                                    }
                                ) {
                                    Text(
                                        text = "🕒",
                                        fontSize = 22.sp
                                    )
                                }
                            },

                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ngày thực hiện") },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val calendar = Calendar.getInstance()

                                        DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                newDate = "%02d/%02d/%04d".format(
                                                    day,
                                                    month + 1,
                                                    year
                                                )
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                ) {
                                    Text("📅", fontSize = 22.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isSaving &&
                                newTitle.isNotBlank() &&
                                newTime.isNotBlank(),
                        onClick = {
                            isSaving = true
                            editError = null

                            TaskRepo.update(
                                context = context,
                                task = task,
                                newTitle = newTitle.trim(),
                                newTag = newTag,
                                newTime = newTime.trim(),
                                newDate = newDate,
                                onSuccess = {
                                    isSaving = false
                                    showEditDialog = false
                                    onBack()
                                },
                                onError = { error ->
                                    isSaving = false
                                    editError = error
                                }
                            )
                        }
                    ) {
                        Text(if (isSaving) "Đang lưu..." else "Lưu")
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isSaving,
                        onClick = { showEditDialog = false }
                    ) {
                        Text("Hủy")
                    }
                }
            )
        }
    }

    if (showTimePicker) {
        TimeWheelPicker(
            initialTime = newTime,
            onDismiss = {
                showTimePicker = false
            },
            onConfirm = { selectedTime ->
                newTime = selectedTime
                showTimePicker = false
            }
        )
    }
}
