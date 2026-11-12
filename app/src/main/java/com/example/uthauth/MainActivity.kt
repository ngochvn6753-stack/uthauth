package com.example.uthauth

import android.os.Bundle
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.uthauth.ui.theme.UthauthTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.style.TextOverflow
import java.util.Calendar
import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }


        setContent {
            UthauthTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(Modifier.padding(innerPadding)) { AppNav() }
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: AuthViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTask by remember{
        mutableStateOf<Task?>(null)
    }
    val tasks = remember {mutableStateListOf<Task> ()}
    DisposableEffect(Unit) {
        val stop = TaskRepo.listen { list ->
            tasks.clear()
            tasks.addAll(list)
        }

        onDispose { stop() }
    }
    val start = if (vm.currentUser() != null) "home" else "login"

    NavHost(navController = nav, startDestination = start) {
        composable("login") {
            LoginScreen(vm) {
                nav.navigate("home") { popUpTo("login") { inclusive = true } }
            }
        }
        composable("home") {
            val user = vm.currentUser()

            HomeScreen(
                name = user?.displayName ?: "",
                onProfile = {
                    nav.navigate("profile")
                },
                onTaskClick = { task ->
                    selectedTask = task
                    nav.navigate("task_detail")
                }
            )
        }
        composable("profile") {
            val user = vm.currentUser()
            ProfileScreen(
                name = user?.displayName ?: "",
                email = user?.email ?: "",
                tasks = tasks,
                onBack = { nav.popBackStack() },
                onLogout = {
                    scope.launch {
                        vm.signOut()
                        try {
                            CredentialManager.create(context)
                                .clearCredentialState(ClearCredentialStateRequest())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        nav.navigate("login") { popUpTo("home") { inclusive = true } }
                    }
                }
            )
        }
        composable ("task_detail") {
            selectedTask?.let{ task ->
                TaskDetailScreen(
                    task = task,
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }
        }
    }



//    Button(
//        onClick = {
//            val calendar = java.util.Calendar.getInstance().apply {
//                add(java.util.Calendar.MINUTE, 2)
//            }
//
//            val date = java.text.SimpleDateFormat(
//                "dd/MM/yyyy",
//                java.util.Locale.getDefault()
//            ).format(calendar.time)
//
//            val time = java.text.SimpleDateFormat(
//                "HH:mm",
//                java.util.Locale.getDefault()
//            ).format(calendar.time)
//
//            TaskAlarmScheduler.schedule(
//                context = context,
//                taskId = "test-reminder",
//                title = "Thông báo thử từ UTH SmartTasks",
//                date = date,
//                time = time
//            )
//        }
//    ) {
//        Text("🔔 Thử thông báo")
//    }

}


//màn đăng ký gg
@Composable
fun LoginScreen(vm: AuthViewModel, onLoggedIn: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.default_web_client_id)

    LaunchedEffect(state) {
        if (state is AuthUiState.Success) {
            Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
            onLoggedIn()
        }
    }

    fun signIn() {
        //Toast.makeText(context, "Đã bấm nút", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()
                val result = CredentialManager.create(context).getCredential(context, request)
                val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                vm.signInWithIdToken(token)
            } catch (e: Exception) {
                vm.onError(e.message ?: "Đăng nhập lỗi")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
            .navigationBarsPadding()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(90.dp))
        val logoBlue = Color(0xFF3355FF)

        Box(
            modifier = Modifier
                .size(88.dp)
                .background(logoBlue, RoundedCornerShape(19.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(58.dp)) {
                val w = size.width
                val stroke = w * 0.082f

                val y1 = size.height * 0.34f
                val y2 = size.height * 0.68f

                val cx = w * 0.20f
                val r = w * 0.16f

                drawCircle(
                    color = Color.White,
                    radius = r,
                    center = Offset(cx, y1)
                )

                val tick = Path().apply {
                    moveTo(cx - r * 0.45f, y1)
                    lineTo(cx - r * 0.1f, y1 + r * 0.4f)
                    lineTo(cx + r * 0.5f, y1 - r * 0.35f)
                }
                drawPath(
                    path = tick,
                    color = logoBlue,
                    style = Stroke(
                        width = w * 0.055f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.50f, y1),
                    end = Offset(w * 0.92f, y1),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )

                drawCircle(
                    color = Color.White,
                    radius = w * 0.109f,
                    center = Offset(cx, y2),
                    style = Stroke(width = stroke)
                )
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.50f, y2),
                    end = Offset(w * 0.92f, y2),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(Modifier.height(25.dp))

        Text("Chào mừng đến với UTH Todo", fontSize = 32.sp, fontWeight = FontWeight.Bold,
             lineHeight = 40.sp)

        //Spacer(Modifier.height(14.dp))

        Text("Đăng nhập để lưu công việc và đồng bộ trên mọi thiết bị của bạn",
            fontSize = 14.sp,
            textAlign = TextAlign.Left)

        Spacer(Modifier.height(100.dp))

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {

            Button(
                onClick = { signIn() },
                modifier = Modifier.height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.Gray),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF),
                    contentColor = Color.Black
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Tiếp tục với Google", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                }
            }
            Spacer(Modifier.height(8.dp))


            Text("hoặc")
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { })
            {
                Text("Dùng thử không cần tài khoản", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(24.dp))
            if (state is AuthUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text((state as AuthUiState.Error).message, Modifier.padding(16.dp)) }
            }
        }
    }
}
data class Task(
    val title: String = "",
    val tag: String = "",
    val time: String = "",
    val date: String = "",
    val done: Boolean = false,
    val id: String = "",
    val completedAt: Long = 0L
)
@Composable
fun HomeTab(
    name: String,
    tasks: MutableList<Task>,
    onProfile: () -> Unit,
    onSeeAll: () -> Unit,
    onTaskClick: (Task) -> Unit
){
    val blue = Color(0xFF3355FF)
    val doneCount = tasks.count { it.done }
    val percent = if (tasks.isEmpty()) 0 else doneCount * 100 / tasks.size
    val message = when {
        percent >= 100 -> "Xong hết rồi, giỏi quá!"
        percent >= 50 -> "Đã hơn nửa chặng rồi, cố lên!"
        else -> "Bắt đầu thôi nào!"
    }
    var greeting by remember { mutableStateOf(AppConfig.greeting()) }
    LaunchedEffect(Unit) {
        AppConfig.fetch { greeting = AppConfig.greeting() }
    }
    val context = LocalContext.current
    val categories = listOf(
        Triple("🎓", "Học tập", 0),
        Triple("🏠", "Cuộc sống", 0),
        Triple("💼", "Làm việc", 0),
        Triple("💪", "Sức khỏe", 0),
        Triple("🎮", "Giải trí", 0),
        Triple("📌", "Khác", 0)
    )


    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 110.dp)
        ) {
            // Hàng đầu: avatar + lời chào
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFDCE4FF), CircleShape)
                        .clickable { onProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = blue)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Hôm nay", fontSize = 12.sp, color = Color.Gray)
                    Text("$greeting $name!", fontSize = 20.sp, fontWeight = FontWeight.Bold)                }
            }
            Spacer(Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = blue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tiến độ hôm nay", color = Color.White.copy(alpha = 0.8f))
                        Text(
                            "$doneCount/${tasks.size} việc",
                            color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
                        )
                        Text(message, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier.size(64.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                            strokeWidth = 6.dp
                        )
                        Text("$percent%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))


            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (emoji, category, _) ->
                            val count = tasks.count {
                                it.tag == category && !it.done
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(125.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    Color(0xFFE0E0E0)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 22.sp
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = category,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = "$count việc còn lại",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Việc hôm nay", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Xem tất cả", color = blue, fontSize = 13.sp, modifier = Modifier.clickable { onSeeAll() })
            }
            Spacer(Modifier.height(8.dp))

            for (task in tasks) {
                Box(
                    modifier = Modifier.clickable {
                        onTaskClick(task)
                    }
                ) {
                    TaskItem(task) {
                        TaskRepo.toggle(context, task)
                    }
                }
            }
            if (tasks.isEmpty()) {
                Text("Chưa có việc nào, bấm + để thêm", color = Color.Gray)
            }
        }

       // HomeBottomBar(Modifier.align(Alignment.BottomCenter))
    }
}
@Composable
fun HomeScreen(
    name: String,
    onProfile: () -> Unit,
    onTaskClick: (Task) -> Unit) {
    val tasks = remember { mutableStateListOf<Task>() }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val stop = TaskRepo.listen { list ->
            tasks.clear()
            tasks.addAll(list)
        }
        onDispose { stop() }
    }

    Box(Modifier.fillMaxSize()) {
        when (tab) {
            0 -> HomeTab(
                name = name,
                tasks = tasks,
                onProfile = onProfile,
                onSeeAll = { tab = 1 },
                onTaskClick = onTaskClick
            )            1 -> ListTab(
                tasks = tasks,
                onTaskClick = onTaskClick)
            2 -> NotifTab(tasks)
            3 -> SettingsTab(onProfile)
        }
        HomeBottomBar(
            selected = tab,
            onSelect = { tab = it },
            onAdd = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onAdd = { task ->
                TaskRepo.add(
                    context = context,
                    task = task
                )
                showAdd = false
            }
        )
    }
}

@Composable
fun ListTab(
    tasks: MutableList<Task>,
    onTaskClick: (Task) -> Unit
) {
    var filter by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf("Tất cả")
    }
    val context = LocalContext.current
    val options = listOf("Tất cả", "Chưa xong", "Đã xong")
    val categories = listOf("Tất cả",
        "Học tập",
        "Cuộc sống",
        "Làm việc",
        "Sức khỏe",
        "Giải trí",
        "Khác")

    val shown = tasks.filter { task ->
        val matchStatus = when (filter) {
            1 -> !task.done
            2 -> task.done
            else -> true
        }

        val matchSearch = task.title.contains(
            search,
            ignoreCase = true
        ) || task.tag.contains(
            search,
            ignoreCase = true
        ) || task.time.contains(
            search,
            ignoreCase = true
        )

        val matchCategory =
            selectedCategory == "Tất cả" ||
                    task.tag == selectedCategory

        matchStatus && matchSearch && matchCategory
    }

    val pendingTasks = shown.filter { !it.done }
    val completedTasks = shown.filter { it.done }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 24.dp,
                bottom = 110.dp
            )
    ) {
        Text(
            text = "Danh sách công việc",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Tìm công việc...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                Chip(
                    category,
                    selectedCategory == category
                ) {
                    selectedCategory = category
                }
            }
        }


        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { index, label ->
                Chip(label, filter == index) {
                    filter = index
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Nhóm CẦN LÀM
        if (filter != 2) {
            Text(
                text = "CẦN LÀM (${pendingTasks.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(Modifier.height(12.dp))

            if (pendingTasks.isEmpty()) {
                Text(
                    text = "Không có công việc cần làm",
                    color = Color.Gray
                )
            } else {
                pendingTasks.forEach { task ->
                    Box(
                        modifier = Modifier.clickable {
                            onTaskClick(task)
                        }
                    ) {
                        TaskItem(task) {
                            TaskRepo.toggle(context, task)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Nhóm ĐÃ XONG
        if (filter != 1) {
            Text(
                text = "ĐÃ XONG (${completedTasks.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Spacer(Modifier.height(12.dp))

            if (completedTasks.isEmpty()) {
                Text(
                    text = "Chưa có công việc hoàn thành",
                    color = Color.Gray
                )
            } else {
                completedTasks.forEach { task ->
                    Box(
                        modifier = Modifier.clickable {
                            onTaskClick(task)
                        }
                    ) {
                        TaskItem(task) {
                            TaskRepo.toggle(context, task)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun NotifTab(tasks: List<Task>) {
    val pending = tasks.filter { !it.done }
    val calendar = Calendar.getInstance()
    val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 +
            calendar.get(Calendar.MINUTE)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = 24.dp,
                bottom = 110.dp
            )
    ) {
        Text(
            text = "Thông báo",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        if (pending.isEmpty()) {
            Text(
                text = "Không có công việc cần nhắc",
                color = Color.Gray
            )
        }

        pending.forEach { task ->

            val formatter = SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }

            val taskDateTime = try {
                if (task.date.isNotBlank()) {
                    formatter.parse("${task.date} ${task.time}")
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            val isOverdue = taskDateTime != null &&
                    taskDateTime.before(Date())



            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    Color(0xFFE0E0E0)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOverdue) "⏰" else "🔔",
                        fontSize = 22.sp
                    )

                    Spacer(Modifier.width(12.dp))

                    Column {

                        Text(
                            text = when {
                                taskDateTime == null ->
                                    "Chưa tới hạn: ${task.title}"
                                isOverdue ->
                                    "Quá hạn: ${task.title}"
                                else ->
                                    "Sắp tới: ${task.title}"
                            },
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "${task.date.ifBlank { "Chưa có ngày" }} · ${task.time} · ${task.tag}",                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(onProfile: () -> Unit) {
    var remind by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Cài đặt", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().clickable { onProfile() }
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Thông tin cá nhân", Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Nhắc việc hằng ngày", Modifier.weight(1f))
                Switch(checked = remind, onCheckedChange = { remind = it })
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { throw RuntimeException("Test Crashlytics") }) {
            Text("Test crash")
        }
    }
}


@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (Task) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    val context = LocalContext.current

    var date by remember {
        mutableStateOf(
            java.text.SimpleDateFormat(
                "dd/MM/yyyy",
                java.util.Locale.getDefault()
            ).format(java.util.Date())
        )
    }
    var tag by remember { mutableStateOf("Học tập") }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm việc mới") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên việc") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Thời gian") },


                    trailingIcon = {
                        IconButton(
                            onClick = { showTimePicker = true }
                        ) {
                            Text(
                                text = "🕒",
                                fontSize = 22.sp
                            )
                        }
                    },


                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
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
                                        date = "%02d/%02d/%04d".format(
                                            day, month + 1, year
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

                Spacer(Modifier.height(12.dp))



                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Học tập",
                        "Cuộc sống",
                        "Làm việc",
                        "Sức khỏe",
                        "Giải trí",
                        "Khác"
                    ).chunked(3).forEach { rowItems ->

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { category ->
                                Chip(
                                    label = category,
                                    selected = tag == category,
                                    onClick = {
                                        tag = category
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },

                confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onAdd(
                        Task(
                            title = title.trim(),
                            tag = tag,
                            time = time,
                            date = date,
                            done = false
                        )
                    )
                }
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )

    if (showTimePicker) {
        TimeWheelPicker(
            initialTime = time,
            onDismiss = {
                showTimePicker = false
            },
            onConfirm = { selectedTime ->
                time = selectedTime
                showTimePicker = false
            }
        )
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Color(0xFF3355FF) else Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, color = if (selected) Color.White else Color.Black)
    }
}
@Composable
fun TaskItem(task: Task, onToggle: () -> Unit) {
    val blue = Color(0xFF3355FF)
    val isStudy = task.tag == "Học tập"
    val chipBg = if (isStudy) Color(0xFFE3E9FF) else Color(0xFFFFE8D1)
    val chipText = if (isStudy) blue else Color(0xFFE65100)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ô tick hình tròn
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (task.done) blue else Color.White, CircleShape)
                    .border(1.5.dp, if (task.done) blue else Color(0xFFBDBDBD), CircleShape)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (task.done) {
                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) Color.Gray else Color.Black
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .background(chipBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(task.tag, fontSize = 11.sp, color = chipText, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(task.time, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun HomeBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarItem(Icons.Default.Home, "Trang chủ", selected == 0) { onSelect(0) }
                BarItem(Icons.AutoMirrored.Filled.List, "Danh sách", selected == 1) { onSelect(1) }
                Spacer(Modifier.width(56.dp))
                BarItem(Icons.Default.Notifications, "Thông báo", selected == 2) { onSelect(2) }
                BarItem(Icons.Default.Settings, "Cài đặt", selected == 3) { onSelect(3) }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            containerColor = Color(0xFF3355FF),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm việc")
        }
    }
}

@Composable
fun BarItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) Color(0xFF3355FF) else Color.Gray
    Column(
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Text(label, fontSize = 11.sp, color = color)
    }
}


@Composable
fun ProfileScreen(
    name: String,
    email: String,
    tasks: List<Task>,
    onBack: () -> Unit,
    onLogout: () -> Unit) {
    val blue = Color(0xFF3355FF)
    val red =  Color(0xFFE53935)

    val words = name.trim().split(" ").filter {it.isNotEmpty()}
    val initials = when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1)
        else -> words.first().take(1) + words.last().take(1)
    }.uppercase()

    val totalTasks = tasks.size
    val completedTasks = tasks.count {it.done}

    val completionRate = if( totalTasks == 0){
        0
    }else{
        completedTasks * 100 / totalTasks
    }
    val days = listOf("T2","T3","T4","T5","T6","T7","CN")
    val values = listOf(3,5,2,4,6,1,2)
    val maxValue = values.max()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()){
            Text(
                "Cá nhân",
                color = blue,
                modifier = Modifier.align (Alignment.CenterStart).clickable{onBack()}
            )
            Text(
                "Hồ sơ",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.size(88.dp).background(Color(0xFFDCE4FF), CircleShape),
            contentAlignment = Alignment.Center){
            Text(initials, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = blue)
        }
        Spacer(Modifier.height(12.dp))
        Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(email, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                Modifier.weight(1f),
                "$completedTasks",
                "Hoàn thành")
            StatCard(
                Modifier.weight(1f),
                "$totalTasks",
                "Tổng việc")
            StatCard(
                Modifier.weight(1f),
                "$completionRate%",
                "Tỉ lệ")
        }
        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ){
            Column(Modifier.padding(16.dp)) {
                Text("Tuần này", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ){
                    for ((i,day) in days.withIndex()){
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .width(20.dp)
                                    .height((90 * values[i] / maxValue).dp)
                                    .background(blue, RoundedCornerShape(6.dp))
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(day, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, red),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = red,
                contentColor = Color.White)
        ) {
            Text("Đăng xuất", fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
fun StatCard(modifier: Modifier, value: String, label: String){
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun TimeWheelPicker(
    initialTime: String = "08:00",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")

    var hour by remember(initialTime) {
        mutableIntStateOf(
            (parts.getOrNull(0)?.toIntOrNull() ?: 8)
                .coerceIn(0, 23)
        )
    }

    var minute by remember(initialTime) {
        mutableIntStateOf(
            (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                .coerceIn(0, 59)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn thời gian") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "%02d:%02d".format(hour, minute),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3355FF)
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    TimeWheelColumn(
                        values = (0..23).toList(),
                        selected = hour,
                        onSelect = { hour = it },
                        modifier = Modifier.weight(1f)
                    )

                    Text(":", fontSize = 28.sp)

                    TimeWheelColumn(
                        values = (0..59).toList(),
                        selected = minute,
                        onSelect = { minute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        "%02d:%02d".format(hour, minute)
                    )
                }
            ) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun TimeWheelColumn(
    values: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 48.dp

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selected
    )

    val flingBehavior =
        androidx.compose.foundation.gestures.snapping
            .rememberSnapFlingBehavior(
                lazyListState = listState
            )

    LaunchedEffect(listState, values) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val center = (
                    layout.viewportStartOffset +
                            layout.viewportEndOffset
                    ) / 2

            layout.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(
                    item.offset + item.size / 2 - center
                )
            }?.index
        }.collect { index ->
            if (index != null) {
                values.getOrNull(index)?.let(onSelect)
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(180.dp),
        contentPadding = PaddingValues(vertical = 66.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(values) { index, value ->
            val active = value == selected

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(
                        if (active) Color(0xFFE8EDFF)
                        else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        onSelect(value)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "%02d".format(value),
                    fontSize = if (active) 24.sp else 19.sp,
                    fontWeight = if (active)
                        FontWeight.Bold
                    else
                        FontWeight.Normal,
                    color = if (active)
                        Color(0xFF3355FF)
                    else
                        Color.Gray
                )
            }
        }
    }
}
