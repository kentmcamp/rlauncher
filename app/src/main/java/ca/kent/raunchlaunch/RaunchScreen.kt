package ca.kent.raunchlaunch

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun RaunchScreen(viewModel: RaunchViewModel = viewModel()) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val fanMenuDirectories = remember(viewModel.directories) {
        viewModel.directories.filter { it.name != PINNED_APPS_DIR }
    }

    val currentItems = viewModel.currentItems
    val displayItems = remember(viewModel.scrollIndex, currentItems) {
        val maxVisible = 5
        currentItems.subList(
            viewModel.scrollIndex,
            (viewModel.scrollIndex + maxVisible).coerceAtMost(currentItems.size)
        )
    }

    var touchOffset by remember { mutableStateOf(Offset.Zero) }
    var centerPoint by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(viewModel.selectedSlotRight) {
        if (viewModel.selectedSlotRight == 0 || viewModel.selectedSlotRight == 6) {
            delay(800)
            while (true) {
                if (viewModel.selectedSlotRight == 0) viewModel.scrollUp()
                else if (viewModel.selectedSlotRight == 6) viewModel.scrollDown()
                else break
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(300)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .pointerInput(centerPoint) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val dx = offset.x - centerPoint.x
                        val dy = offset.y - centerPoint.y
                        val distance = sqrt(dx * dx + dy * dy)
                        if (!viewModel.isEditMode && distance < 60.dp.toPx()) {
                            viewModel.isMenuVisible = true
                            touchOffset = offset
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { _, dragAmount ->
                        if (viewModel.isMenuVisible) {
                            touchOffset += dragAmount
                            val dx = touchOffset.x - centerPoint.x
                            val dy = touchOffset.y - centerPoint.y
                            val distance = sqrt(dx * dx + dy * dy)

                            if (distance > 25f) {
                                val angle = atan2(dy, dx) * (180 / PI).toFloat()
                                if (dx > 45) {
                                    if (viewModel.selectedSlotLeft != -1) {
                                        viewModel.selectedSlotLeft = -1
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    val normalized = (angle + 70).coerceIn(0f, 140f)
                                    val newSlotRight = (normalized / 140f * 6).roundToInt()
                                    if (newSlotRight != viewModel.selectedSlotRight) {
                                        viewModel.selectedSlotRight = newSlotRight
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                } else if (dx < -110) {
                                    if (viewModel.selectedSlotRight != -1) {
                                        viewModel.selectedSlotRight = -1
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    val adjAngle = if (angle < 0) angle + 360 else angle
                                    val normalized = (220 - adjAngle).coerceIn(0f, 80f)
                                    val slot = (normalized / 80f * (fanMenuDirectories.size - 1)).roundToInt()
                                    if (slot != viewModel.selectedSlotLeft) {
                                        viewModel.selectedSlotLeft = slot
                                        val targetDir = fanMenuDirectories.getOrNull(slot)
                                        val actualIndex = viewModel.directories.indexOf(targetDir)
                                        if (actualIndex != -1) {
                                            viewModel.switchDirectory(actualIndex)
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                } else if (dx < -30 && viewModel.selectedSlotLeft == -1) {
                                    val adjAngle = if (angle < 0) angle + 360 else angle
                                    val normalized = (220 - adjAngle).coerceIn(0f, 80f)
                                    val slot = (normalized / 80f * (fanMenuDirectories.size - 1)).roundToInt()
                                    viewModel.selectedSlotLeft = slot
                                    val targetDir = fanMenuDirectories.getOrNull(slot)
                                    val actualIndex = viewModel.directories.indexOf(targetDir)
                                    if (actualIndex != -1) {
                                        viewModel.switchDirectory(actualIndex)
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else if (dx > -30 && dx < 45) {
                                    if (viewModel.selectedSlotRight != -1) {
                                        viewModel.selectedSlotRight = -1
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            } else {
                                if (viewModel.selectedSlotRight != -1 || viewModel.selectedSlotLeft != -1) {
                                    viewModel.selectedSlotRight = -1
                                    viewModel.selectedSlotLeft = -1
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        if (viewModel.selectedSlotRight in 1..5) {
                            viewModel.launchSelectedApp()
                        }
                        viewModel.isMenuVisible = false
                        viewModel.selectedSlotRight = -1
                        viewModel.selectedSlotLeft = -1
                    },
                    onDragCancel = {
                        viewModel.isMenuVisible = false
                        viewModel.selectedSlotRight = -1
                        viewModel.selectedSlotLeft = -1
                    }
                )
            }
    ) {
        // --- Media Widget ---
        MediaWidget(
            modifier = Modifier.align(if (isLandscape) Alignment.TopEnd else Alignment.TopCenter)
        )

        // --- Pinned Apps Bar ---
        // Pushed 40dp to the right in landscape (Alignment.Center -> Offset(x=40.dp))
        PinnedAppsBar(
            viewModel = viewModel,
            modifier = Modifier
                .align(if (isLandscape) Alignment.Center else Alignment.BottomCenter)
                .then(if (isLandscape) Modifier.offset(x = 40.dp) else Modifier)
        )

        val triggerSize = 80.dp
        val startPadding = 105.dp

        // --- Trigger Circle ---
        Box(
            modifier = Modifier
                .padding(start = startPadding)
                .align(Alignment.CenterStart)
                .size(triggerSize)
                .onGloballyPositioned {
                    val pos = it.positionInRoot()
                    centerPoint = Offset(pos.x + it.size.width / 2, pos.y + it.size.height / 2)
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            viewModel.isEditMode = !viewModel.isEditMode
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (viewModel.isEditMode) Color(0xFFF85149).copy(alpha = 0.4f) else Color(0xFF58A6FF).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, if (viewModel.isEditMode) Color(0xFFF85149) else Color(0xFF58A6FF).copy(alpha = 0.5f), CircleShape)
        ) {
            if (viewModel.isEditMode) {
                Icon(
                    Icons.Default.Edit, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(24.dp).align(Alignment.Center)
                )
            }
        }

        // --- Main Launch Menu ---
        AnimatedVisibility(
            visible = viewModel.isMenuVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f)
        ) {
            val centerDpX = with(density) { centerPoint.x.toDp() }
            val centerDpY = with(density) { centerPoint.y.toDp() }

            Box(modifier = Modifier.fillMaxSize()) {
                TechPanel(
                    centerDpX = centerDpX, centerDpY = centerDpY,
                    isRight = true, title = viewModel.currentDir?.name?.replace("_", " ") ?: "",
                    panelWidth = 205.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TechMenuItem(label = "UP", icon = Icons.Default.KeyboardArrowUp, isSelected = viewModel.selectedSlotRight == 0, isEnabled = viewModel.scrollIndex > 0, itemWidth = 165.dp)
                        displayItems.forEachIndexed { i, app ->
                            TechMenuItem(label = app.label, iconDrawable = app.icon, isSelected = viewModel.selectedSlotRight == i + 1, itemWidth = 165.dp)
                        }
                        TechMenuItem(label = "DOWN", icon = Icons.Default.KeyboardArrowDown, isSelected = viewModel.selectedSlotRight == 6, isEnabled = viewModel.scrollIndex < currentItems.size - 5, itemWidth = 165.dp)
                    }
                }

                TechPanel(
                    centerDpX = centerDpX, centerDpY = centerDpY,
                    isRight = false, title = "DIR",
                    panelWidth = 95.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                        fanMenuDirectories.forEachIndexed { i, label ->
                            val actualIndex = viewModel.directories.indexOf(label)
                            TechMenuItem(
                                label = label.name.replace("_", " "),
                                isSelected = viewModel.selectedSlotLeft == i,
                                isRight = false,
                                isPrimary = actualIndex == viewModel.currentDirIndex,
                                itemWidth = 75.dp
                            )
                        }
                    }
                }
            }
        }

        // --- Management Overlay ---
        AnimatedVisibility(
            visible = viewModel.isEditMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            ManagementOverlay(
                viewModel = viewModel,
                onClose = { viewModel.isEditMode = false }
            )
        }

        if (viewModel.isMenuVisible) {
            LeanIndicator(centerPoint, touchOffset, density)
        }
    }
}

@Composable
fun PinnedAppsBar(viewModel: RaunchViewModel, modifier: Modifier) {
    val pinned = viewModel.pinnedApps
    if (pinned.isEmpty()) return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val canScrollBackward by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }

    Box(
        modifier = modifier
            .padding(16.dp)
            .wrapContentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161B22).copy(alpha = 0.8f))
            .border(1.dp, Color(0xFF58A6FF).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        if (isLandscape) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(130.dp)) {
                if (pinned.size > 4) {
                    Icon(
                        Icons.Default.KeyboardArrowUp, null, 
                        tint = if (canScrollBackward) Color.White else Color.DarkGray,
                        modifier = Modifier.size(24.dp).clickable(enabled = canScrollBackward) { 
                            scope.launch { listState.animateScrollToItem((listState.firstVisibleItemIndex - 2).coerceAtMost(0)) }
                        }
                    )
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.height(240.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = (pinned.size + 1) / 2
                    items(rows) { rowIndex ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val idx1 = rowIndex * 2
                            val idx2 = rowIndex * 2 + 1
                            PinnedIcon(pinned[idx1]) { viewModel.launchApp(pinned[idx1].packageName) }
                            if (idx2 < pinned.size) {
                                PinnedIcon(pinned[idx2]) { viewModel.launchApp(pinned[idx2].packageName) }
                            } else if (pinned.size > 1) {
                                Spacer(modifier = Modifier.width(54.dp))
                            }
                        }
                    }
                }

                if (pinned.size > 4) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, null, 
                        tint = if (canScrollForward) Color.White else Color.DarkGray,
                        modifier = Modifier.size(24.dp).clickable(enabled = canScrollForward) { 
                            scope.launch { listState.animateScrollToItem(listState.firstVisibleItemIndex + 1) }
                        }
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(64.dp)) {
                if (pinned.size > 5) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft, null,
                        tint = if (canScrollBackward) Color.White else Color.DarkGray,
                        modifier = Modifier.width(12.dp).fillMaxHeight().clickable(enabled = canScrollBackward) { 
                            scope.launch { listState.animateScrollToItem((listState.firstVisibleItemIndex - 1).coerceAtLeast(0)) }
                        }
                    )
                }
                
                LazyRow(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(pinned) { _, app ->
                        PinnedIcon(app) { viewModel.launchApp(app.packageName) }
                    }
                }

                if (pinned.size > 5) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                        tint = if (canScrollForward) Color.White else Color.DarkGray,
                        modifier = Modifier.width(12.dp).fillMaxHeight().clickable(enabled = canScrollForward) { 
                            scope.launch { listState.animateScrollToItem(listState.firstVisibleItemIndex + 1) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PinnedIcon(app: AppInfo, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(54.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(2.dp)
    ) {
        if (app.icon != null) Image(painter = rememberDrawablePainter(app.icon), contentDescription = null, modifier = Modifier.size(48.dp))
        Text(text = app.label.uppercase(), color = Color.White.copy(alpha = 0.9f), fontSize = 5.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun ManagementOverlay(viewModel: RaunchViewModel, onClose: () -> Unit) {
    var editingDirIndex by remember { mutableStateOf<Int?>(null) }
    var newDirName by remember { mutableStateOf("") }
    var showAbout by remember { mutableStateOf(false) }
    val sortedDirectories = remember(viewModel.directories) { viewModel.directories.sortedBy { if (it.name == PINNED_APPS_DIR) 0 else 1 } }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117).copy(alpha = 0.98f)).padding(16.dp)) {
        if (showAbout) AboutScreen(onBack = { showAbout = false })
        else {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DIRECTORY_MANAGER", color = Color(0xFFF85149), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showAbout = true }) { Icon(Icons.Default.Info, null, tint = Color(0xFF58A6FF)) }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(value = newDirName, onValueChange = { newDirName = it }, placeholder = { Text("NEW_DIR_NAME", color = Color.Gray) }, modifier = Modifier.weight(1f), colors = TextFieldDefaults.colors(unfocusedContainerColor = Color(0xFF161B22), focusedContainerColor = Color(0xFF161B22), unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { if (newDirName.isNotBlank()) { viewModel.updateDirectories(viewModel.directories + Directory(newDirName.uppercase().replace(" ", "_"), emptyList())); newDirName = "" } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))) { Icon(Icons.Default.Add, null) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(sortedDirectories) { _, dir ->
                        val indexInModel = viewModel.directories.indexOf(dir)
                        val isPinnedDir = dir.name == PINNED_APPS_DIR
                        DirectoryEditItem(directory = dir, isLocked = dir.name == ALL_APPS_DIR, isPinnedSection = isPinnedDir, canMoveUp = indexInModel > 0 && !isPinnedDir && dir.name != ALL_APPS_DIR, canMoveDown = indexInModel < viewModel.directories.size - 1 && !isPinnedDir, onMoveUp = { viewModel.moveDirectory(indexInModel, indexInModel - 1) }, onMoveDown = { viewModel.moveDirectory(indexInModel, indexInModel + 1) }, onDelete = { viewModel.updateDirectories(viewModel.directories.toMutableList().apply { removeAt(indexInModel) }) }, onEditApps = { editingDirIndex = indexInModel })
                    }
                }
            }
        }
        if (editingDirIndex != null) {
            val index = editingDirIndex!!
            AppSelectionOverlay(directory = viewModel.directories[index], installedApps = viewModel.installedApps, onClose = { editingDirIndex = null }, onUpdate = { updatedDir -> val newList = viewModel.directories.toMutableList(); newList[index] = updatedDir; viewModel.updateDirectories(newList) }, onMoveApp = { from, to -> viewModel.moveAppInDirectory(index, from, to) })
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("ABOUT_R_LAUNCHER", color = Color(0xFF58A6FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(40.dp))
        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF161B22), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp)).padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("DEVELOPED_BY", color = Color.Gray, fontSize = 12.sp)
                Text("KENT", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("SOURCE_CODE", color = Color.Gray, fontSize = 12.sp)
                Text("GITHUB.COM/KENTMCAMP/RLAUNCHER", color = Color(0xFF58A6FF), modifier = Modifier.clickable { uriHandler.openUri("https://github.com/kentmcamp/rlauncher") }, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(40.dp))
                Text("VERSION 1.0.0", color = Color(0xFF238636), fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun DirectoryEditItem(directory: Directory, isLocked: Boolean, isPinnedSection: Boolean = false, canMoveUp: Boolean, canMoveDown: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onDelete: () -> Unit, onEditApps: () -> Unit) {
    val borderColor = if (isPinnedSection) Color(0xFF238636) else Color(0xFF30363D)
    val titleColor = if (isPinnedSection) Color(0xFF238636) else Color.White
    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF161B22), RoundedCornerShape(8.dp)).border(1.dp, borderColor, RoundedCornerShape(8.dp)).padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) { Icon(Icons.Default.KeyboardArrowUp, null, tint = if (canMoveUp) Color.White else Color.DarkGray) }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) { Icon(Icons.Default.KeyboardArrowDown, null, tint = if (canMoveDown) Color.White else Color.DarkGray) }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(directory.name.replace("_", " "), color = titleColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (isLocked) Text("SYSTEM DEFAULT", color = Color(0xFF58A6FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                else if (isPinnedSection) Text("ALWAYS ON HOME SCREEN", color = Color(0xFF238636), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                else Text("${directory.packageNames.size} APPS", color = Color.Gray, fontSize = 12.sp)
            }
            if (!isLocked) {
                IconButton(onClick = onEditApps) { Icon(Icons.Default.Settings, null, tint = Color(0xFF58A6FF)) }
                if (!isPinnedSection) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color(0xFFF85149)) }
            } else Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.padding(12.dp).size(20.dp))
        }
    }
}

@Composable
fun AppSelectionOverlay(directory: Directory, installedApps: List<AppInfo>, onClose: () -> Unit, onUpdate: (Directory) -> Unit, onMoveApp: (Int, Int) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EDIT: ${directory.name}", color = Color(0xFF58A6FF), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("CURRENT ORDER (Drag/Move Here)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp)) }
                itemsIndexed(directory.packageNames) { index, pkg ->
                    val app = installedApps.find { it.packageName == pkg }
                    if (app != null) {
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF161B22), RoundedCornerShape(4.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (app.icon != null) { Image(painter = rememberDrawablePainter(drawable = app.icon), contentDescription = null, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(12.dp)) }
                            Text(app.label, color = Color.White, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onMoveApp(index, index - 1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, null, tint = if (index > 0) Color.White else Color.DarkGray) }
                            IconButton(onClick = { onMoveApp(index, index + 1) }, enabled = index < directory.packageNames.size - 1) { Icon(Icons.Default.KeyboardArrowDown, null, tint = if (index < directory.packageNames.size - 1) Color.White else Color.DarkGray) }
                            IconButton(onClick = { onUpdate(directory.copy(packageNames = directory.packageNames - pkg)) }) { Icon(Icons.Default.Delete, null, tint = Color(0xFFF85149)) }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)); Text("ADD MORE APPS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp)) }
                val unselectedApps = installedApps.filter { !directory.packageNames.contains(it.packageName) }
                itemsIndexed(unselectedApps) { _, app ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { onUpdate(directory.copy(packageNames = directory.packageNames + app.packageName)) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF238636), modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(12.dp))
                        if (app.icon != null) { Image(painter = rememberDrawablePainter(drawable = app.icon), contentDescription = null, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.width(12.dp)) }
                        Text(app.label, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun TechPanel(centerDpX: Dp, centerDpY: Dp, isRight: Boolean, title: String, panelWidth: Dp = 180.dp, content: @Composable () -> Unit) {
    val xOffset = if (isRight) centerDpX + 45.dp else centerDpX - 45.dp - panelWidth
    Box(modifier = Modifier.offset(x = xOffset, y = centerDpY - 180.dp)) {
        Canvas(modifier = Modifier.size(panelWidth, 360.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                if (isRight) { moveTo(0f, 120f); lineTo(15f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); lineTo(15f, size.height); lineTo(0f, size.height - 120f) }
                else { moveTo(size.width, 120f); lineTo(size.width - 15f, 0f); lineTo(0f, 0f); lineTo(0f, size.height); lineTo(size.width - 15f, size.height); lineTo(size.width, size.height - 120f) }
                close()
            }
            drawPath(path, Color(0xFF161B22), alpha = 0.9f)
            drawPath(path, Color(0xFF58A6FF).copy(alpha = 0.2f), style = Stroke(1.dp.toPx()))
        }
        Column(modifier = Modifier.padding(top = 15.dp, start = if (isRight) 25.dp else 10.dp, end = if (isRight) 10.dp else 25.dp)) {
            Text(title, color = Color(0xFF58A6FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(15.dp))
            content()
        }
    }
}

@Composable
fun TechMenuItem(label: String, icon: ImageVector? = null, iconDrawable: Drawable? = null, isSelected: Boolean, isRight: Boolean = true, isEnabled: Boolean = true, isPrimary: Boolean = false, itemWidth: Dp = 140.dp) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f)
    val alphaValue by animateFloatAsState(if (isEnabled) 1f else 0.3f)
    Box(modifier = Modifier.width(itemWidth).height(36.dp).scale(scale).clip(RoundedCornerShape(4.dp)).background(if (isSelected) Color(0xFF58A6FF).copy(alpha = 0.3f) else if (isPrimary) Color(0xFF58A6FF).copy(alpha = 0.15f) else Color.Transparent).border(0.5.dp, if (isSelected) Color(0xFF58A6FF) else if (isPrimary) Color(0xFF58A6FF).copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp), contentAlignment = if (isRight) Alignment.CenterStart else Alignment.CenterEnd) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(alphaValue)) {
            if (isRight) {
                if (iconDrawable != null) { Image(painter = rememberDrawablePainter(drawable = iconDrawable), contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)) }
                else if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)) }
            }
            Text(label, color = if (isPrimary || isSelected) Color.White else Color(0xFF8B949E), fontSize = 10.sp, fontWeight = if (isSelected || isPrimary) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
            if (!isRight && icon != null) { Spacer(modifier = Modifier.width(8.dp)); Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
fun LeanIndicator(center: Offset, touch: Offset, density: androidx.compose.ui.unit.Density) {
    val indicatorSize = 60.dp
    Box(modifier = Modifier.offset(x = (center.x).dp / density.density - (indicatorSize / 2) + (touch.x - center.x).dp / 1.2f, y = (center.y).dp / density.density - (indicatorSize / 2) + (touch.y - center.y).dp / 1.2f).size(indicatorSize)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFF85149), 3.dp.toPx())
            drawCircle(Color(0xFFF85149).copy(alpha = 0.2f), size.minDimension / 2, style = Stroke(1.dp.toPx()))
        }
    }
}
