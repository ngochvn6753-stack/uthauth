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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                onProfile = { nav.navigate("profile") }
            )
        }
        composable("profile") {
            val user = vm.currentUser()
            ProfileScreen(
                name = user?.displayName ?: "",
                email = user?.email ?: "",
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
    }
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



data class Task(val title: String, val tag: String, val time: String, val done: Boolean)
@Composable
fun HomeTab(name: String, tasks: MutableList<Task>, onProfile: () -> Unit, onSeeAll: () -> Unit) {
    val blue = Color(0xFF3355FF)
//    val tasks = remember {
//        mutableStateListOf(
//            Task("Tập thể dục 30 phút", "Cuộc sống", "06:30", true),
//            Task("Nộp bài tập Lập trình thiết bị di động", "Học tập", "09:00", true),
//            Task("Ôn tập chương 3", "Học tập", "14:00", false),
//            Task("Đi chợ", "Cuộc sống", "17:30", false)
//        )
//    }

    val doneCount = tasks.count { it.done }
    val percent = if (tasks.isEmpty()) 0 else doneCount * 100 / tasks.size
    val message = when {
        percent >= 100 -> "Xong hết rồi, giỏi quá!"
        percent >= 50 -> "Đã hơn nửa chặng rồi, cố lên!"
        else -> "Bắt đầu thôi nào!"
    }

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
                    Text("Chào $name!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (tag in listOf("Học tập", "Cuộc sống")) {
                    val isStudy = tag == "Học tập"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isStudy) Color(0xFFE3E9FF) else Color(0xFFFFE8D1),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isStudy) "🎓" else "🏠", fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(tag, fontWeight = FontWeight.Bold)
                            Text(
                                "${tasks.count { it.tag == tag && !it.done }} việc còn lại",
                                fontSize = 12.sp, color = Color.Gray
                            )
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

            for ((index, task) in tasks.withIndex()) {
                TaskItem(task) { tasks[index] = task.copy(done = !task.done) }
            }
        }

       // HomeBottomBar(Modifier.align(Alignment.BottomCenter))
    }
}
@Composable
fun HomeScreen(name: String, onProfile: () -> Unit) {
    val tasks = remember {
        mutableStateListOf(
            Task("Tập thể dục 30 phút", "Cuộc sống", "06:30", true),
            Task("Nộp bài tập Lập trình thiết bị di động", "Học tập", "09:00", true),
            Task("Ôn tập chương 3", "Học tập", "14:00", false),
            Task("Đi chợ", "Cuộc sống", "17:30", false)
        )
    }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when (tab) {
            0 -> HomeTab(name, tasks, onProfile, onSeeAll = { tab = 1 })
            1 -> ListTab(tasks)
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
            onAdd = { tasks.add(it); showAdd = false }
        )
    }
}
@Composable
fun ListTab(tasks: MutableList<Task>) {
    var filter by remember { mutableIntStateOf(0) }
    val options = listOf("Tất cả", "Chưa xong", "Đã xong")
    val shown = tasks.withIndex().filter { (_, t) ->
        filter == 0 || (filter == 1 && !t.done) || (filter == 2 && t.done)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 110.dp)
    ) {
        Text("Danh sách việc", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((i, label) in options.withIndex()) {
                Chip(label, filter == i) { filter = i }
            }
        }
        Spacer(Modifier.height(12.dp))

        for ((index, task) in shown) {
            TaskItem(task) { tasks[index] = task.copy(done = !task.done) }
        }
        if (shown.isEmpty()) Text("Không có việc nào", color = Color.Gray)
    }
}

@Composable
fun NotifTab(tasks: List<Task>) {
    val pending = tasks.filter { !it.done }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 110.dp)
    ) {
        Text("Thông báo", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        for (task in pending) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Sắp tới: ${task.title}")
                        Text("Lúc ${task.time} · ${task.tag}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
        if (pending.isEmpty()) Text("Không có thông báo mới", color = Color.Gray)
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
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (Task) -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    var tag by remember { mutableStateOf("Học tập") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm việc mới") },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Tên việc") }, singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = time, onValueChange = { time = it },
                    label = { Text("Giờ (ví dụ 08:00)") }, singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (t in listOf("Học tập", "Cuộc sống")) {
                        Chip(t, tag == t) { tag = t }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onAdd(Task(title.trim(), tag, time.trim(), false)) }
            ) { Text("Thêm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
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
fun ProfileScreen(name: String, email: String, onBack: () -> Unit, onLogout: () -> Unit) {
    val blue = Color(0xFF3355FF)
    val red =  Color(0xFFE53935)

    val words = name.trim().split(" ").filter {it.isNotEmpty()}
    val initials = when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1)
        else -> words.first().take(1) + words.last().take(1)
    }.uppercase()

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
            StatCard(Modifier.weight(1f), "2", "Hoàn thành")
            StatCard(Modifier.weight(1f), "4", "Tổng việc")
            StatCard(Modifier.weight(1f), "50%", "Tỉ lệ")
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
