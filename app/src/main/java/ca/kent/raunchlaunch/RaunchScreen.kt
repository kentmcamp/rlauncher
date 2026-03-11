package ca.kent.raunchlaunch

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun RaunchScreen(viewModel: RaunchViewModel = viewModel()) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val currentItems = viewModel.currentItems
    val displayItems = remember(viewModel.scrollIndex, currentItems) {
        val maxVisible = 5
        currentItems.subList(
            viewModel.scrollIndex,
            (viewModel.scrollIndex + maxVisible).coerceAtMost(currentItems.size)
        )
    }

    // Temporary touch state for dragging
    var touchOffset by remember { mutableStateOf(Offset.Zero) }
    var centerPoint by remember { mutableStateOf(Offset.Zero) }

    // Auto-Scroll Logic
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
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (!viewModel.isEditMode) {
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
                                    viewModel.selectedSlotLeft = -1
                                    val normalized = (angle + 70).coerceIn(0f, 140f)
                                    viewModel.selectedSlotRight = (normalized / 140f * 6).roundToInt()
                                } else if (dx < -110) {
                                    viewModel.selectedSlotRight = -1
                                    val adjAngle = if (angle < 0) angle + 360 else angle
                                    val normalized = (220 - adjAngle).coerceIn(0f, 80f)
                                    val newSlotLeft = (normalized / 80f * (viewModel.directories.size - 1)).roundToInt()
                                    if (newSlotLeft != viewModel.selectedSlotLeft) {
                                        viewModel.selectedSlotLeft = newSlotLeft
                                        viewModel.switchDirectory(newSlotLeft)
                                    }
                                } else if (dx < -30 && viewModel.selectedSlotLeft == -1) {
                                    val adjAngle = if (angle < 0) angle + 360 else angle
                                    val normalized = (220 - adjAngle).coerceIn(0f, 80f)
                                    val newSlotLeft = (normalized / 80f * (viewModel.directories.size - 1)).roundToInt()
                                    viewModel.selectedSlotLeft = newSlotLeft
                                    viewModel.switchDirectory(newSlotLeft)
                                } else if (dx > -30 && dx < 45) {
                                    viewModel.selectedSlotRight = -1
                                }
                            } else {
                                viewModel.selectedSlotRight = -1
                                viewModel.selectedSlotLeft = -1
                            }
                        }
                    },
                    onDragEnd = {
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
        val triggerSize = 80.dp
        val startPadding = 130.dp

        // --- Trigger Circle ---
        Box(
            modifier = Modifier
                .padding(start = startPadding)
                .align(Alignment.CenterStart)
                .offset(y = triggerSize / 2)
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
                    isRight = true, title = viewModel.currentDir?.name?.replace("_", " ") ?: ""
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TechMenuItem(label = "UP", icon = Icons.Default.KeyboardArrowUp, isSelected = viewModel.selectedSlotRight == 0, isEnabled = viewModel.scrollIndex > 0)
                        displayItems.forEachIndexed { i, label ->
                            TechMenuItem(label = label, isSelected = viewModel.selectedSlotRight == i + 1)
                        }
                        TechMenuItem(label = "DOWN", icon = Icons.Default.KeyboardArrowDown, isSelected = viewModel.selectedSlotRight == 6, isEnabled = viewModel.scrollIndex < currentItems.size - 5)
                    }
                }

                TechPanel(
                    centerDpX = centerDpX, centerDpY = centerDpY,
                    isRight = false, title = "DIR",
                    panelWidth = 120.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                        viewModel.directories.forEachIndexed { i, label ->
                            TechMenuItem(
                                label = label.name.replace("_", " "),
                                isSelected = viewModel.selectedSlotLeft == i,
                                isRight = false,
                                isPrimary = i == viewModel.currentDirIndex,
                                itemWidth = 100.dp
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
                directories = viewModel.directories,
                onUpdate = { viewModel.updateDirectories(it) },
                onClose = { viewModel.isEditMode = false }
            )
        }

        if (viewModel.isMenuVisible) {
            LeanIndicator(centerPoint, touchOffset, density)
        }
    }
}

@Composable
fun ManagementOverlay(
    directories: List<Directory>,
    onUpdate: (List<Directory>) -> Unit,
    onClose: () -> Unit
) {
    var editingDirIndex by remember { mutableStateOf<Int?>(null) }
    var newDirName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117).copy(alpha = 0.98f)).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DIRECTORY_MANAGER", color = Color(0xFFF85149), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = newDirName,
                    onValueChange = { newDirName = it },
                    placeholder = { Text("NEW_DIR_NAME", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF161B22),
                        focusedContainerColor = Color(0xFF161B22),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newDirName.isNotBlank()) {
                            onUpdate(directories + Directory(newDirName.uppercase().replace(" ", "_"), emptyList()))
                            newDirName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                ) {
                    Icon(Icons.Default.Add, null)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(directories.size) { index ->
                    val dir = directories[index]
                    DirectoryEditItem(
                        directory = dir,
                        isLocked = dir.name == ALL_APPS_DIR,
                        onDelete = {
                            onUpdate(directories.toMutableList().apply { removeAt(index) })
                        },
                        onEditApps = {
                            editingDirIndex = index
                        }
                    )
                }
            }
        }

        if (editingDirIndex != null) {
            val index = editingDirIndex!!
            AppSelectionOverlay(
                directory = directories[index],
                onClose = { editingDirIndex = null },
                onUpdate = { updatedDir ->
                    val newList = directories.toMutableList()
                    newList[index] = updatedDir
                    onUpdate(newList)
                }
            )
        }
    }
}

@Composable
fun DirectoryEditItem(
    directory: Directory,
    isLocked: Boolean,
    onDelete: () -> Unit,
    onEditApps: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF161B22), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(directory.name.replace("_", " "), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (isLocked) {
                    Text("SYSTEM DEFAULT", color = Color(0xFF58A6FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("${directory.apps.size} APPS", color = Color.Gray, fontSize = 12.sp)
                }
            }
            if (!isLocked) {
                IconButton(onClick = onEditApps) {
                    Icon(Icons.Default.Settings, null, tint = Color(0xFF58A6FF))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFF85149))
                }
            } else {
                Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.padding(12.dp).size(20.dp))
            }
        }
    }
}

@Composable
fun AppSelectionOverlay(
    directory: Directory,
    onClose: () -> Unit,
    onUpdate: (Directory) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("EDIT: ${directory.name}", color = Color(0xFF58A6FF), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mockAvailableApps) { app ->
                    val isSelected = directory.apps.contains(app)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newApps = if (isSelected) {
                                    directory.apps - app
                                } else {
                                    directory.apps + app
                                }
                                onUpdate(directory.copy(apps = newApps))
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF58A6FF))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(app, color = if (isSelected) Color.White else Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun TechPanel(
    centerDpX: Dp,
    centerDpY: Dp,
    isRight: Boolean,
    title: String,
    panelWidth: Dp = 180.dp,
    content: @Composable () -> Unit
) {
    val xOffset = if (isRight) centerDpX + 45.dp else centerDpX - 45.dp - panelWidth
    
    Box(modifier = Modifier.offset(x = xOffset, y = centerDpY - 240.dp)) {
        Canvas(modifier = Modifier.size(panelWidth, 360.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                if (isRight) {
                    moveTo(0f, 120f); lineTo(15f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); lineTo(15f, size.height); lineTo(0f, size.height - 120f)
                } else {
                    moveTo(size.width, 120f); lineTo(size.width - 15f, 0f); lineTo(0f, 0f); lineTo(0f, size.height); lineTo(size.width - 15f, size.height); lineTo(size.width, size.height - 120f)
                }
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
fun TechMenuItem(
    label: String,
    icon: ImageVector? = null,
    isSelected: Boolean,
    isRight: Boolean = true,
    isEnabled: Boolean = true,
    isPrimary: Boolean = false,
    itemWidth: Dp = 140.dp
) {
    val scale by animateFloatAsState(if (isSelected) 1.05f else 1f)
    val alphaValue by animateFloatAsState(if (isEnabled) 1f else 0.3f)
    
    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(36.dp)
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) Color(0xFF58A6FF).copy(alpha = 0.3f) 
                else if (isPrimary) Color(0xFF58A6FF).copy(alpha = 0.15f) 
                else Color.Transparent
            )
            .border(
                0.5.dp, 
                if (isSelected) Color(0xFF58A6FF) 
                else if (isPrimary) Color(0xFF58A6FF).copy(alpha = 0.4f) 
                else Color.Transparent, 
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp),
        contentAlignment = if (isRight) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(alphaValue)) {
            if (isRight && icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            if (isRight && icon != null) Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                label, color = if (isPrimary || isSelected) Color.White else Color(0xFF8B949E),
                fontSize = 10.sp, fontWeight = if (isSelected || isPrimary) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )

            if (!isRight && icon != null) Spacer(modifier = Modifier.width(8.dp))
            if (!isRight && icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun LeanIndicator(center: Offset, touch: Offset, density: androidx.compose.ui.unit.Density) {
    val indicatorSize = 60.dp
    Box(
        modifier = Modifier
            .offset(
                x = (center.x).dp / density.density - (indicatorSize / 2) + (touch.x - center.x).dp / 1.2f,
                y = (center.y).dp / density.density - (indicatorSize / 2) + (touch.y - center.y).dp / 1.2f
            )
            .size(indicatorSize)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(Color(0xFFF85149), 3.dp.toPx())
            drawCircle(Color(0xFFF85149).copy(alpha = 0.2f), size.minDimension / 2, style = Stroke(1.dp.toPx()))
        }
    }
}
