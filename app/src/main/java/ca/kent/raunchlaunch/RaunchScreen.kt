package ca.kent.raunchlaunch

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun RaunchScreen() {
    // --- State ---
    var isMenuVisible by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf(Offset.Zero) }
    var centerPoint by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Directories
    val directories = listOf("FAVORITES", "ALL_APPS", "WORK", "MEDIA", "SYSTEM")
    var currentDirIndex by remember { mutableIntStateOf(0) }

    // Mock Apps
    val directoryContent = remember {
        mapOf(
            "FAVORITES" to listOf("TERMINAL", "FILES", "BROWSER", "MEDIA", "SYSTEM"),
            "ALL_APPS" to listOf("CALCULATOR", "CAMERA", "CLOCK", "CONTACTS", "EMAIL", "MAPS", "NOTES", "PHONE"),
            "WORK" to listOf("SLACK", "JIRA", "OUTLOOK", "TEAMS", "DOCS"),
            "MEDIA" to listOf("SPOTIFY", "YOUTUBE", "NETFLIX", "PHOTOS", "VLC"),
            "SYSTEM" to listOf("SETTINGS", "LOGCAT", "SHELL", "STORAGE", "CPU")
        )
    }

    val currentItems = directoryContent[directories[currentDirIndex]] ?: emptyList()

    // Scrolling & Selection
    var scrollIndex by remember { mutableIntStateOf(0) }
    val maxVisible = 5
    val displayItems = remember(scrollIndex, currentItems) {
        currentItems.subList(scrollIndex, (scrollIndex + maxVisible).coerceAtMost(currentItems.size))
    }

    var selectedSlotRight by remember { mutableIntStateOf(-1) }
    var selectedSlotLeft by remember { mutableIntStateOf(-1) }
    
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Auto-Scroll Logic
    LaunchedEffect(selectedSlotRight) {
        if (selectedSlotRight == 0 || selectedSlotRight == 6) {
            delay(800)
            while (true) {
                if (selectedSlotRight == 0 && scrollIndex > 0) {
                    scrollIndex--
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else if (selectedSlotRight == 6 && scrollIndex < currentItems.size - maxVisible) {
                    scrollIndex++
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else break
                delay(300)
            }
        }
    }

    // Directory Switch Logic
    LaunchedEffect(selectedSlotLeft) {
        if (selectedSlotLeft in directories.indices && selectedSlotLeft != currentDirIndex) {
            currentDirIndex = selectedSlotLeft
            scrollIndex = 0
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .onGloballyPositioned { containerSize = it.size }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (!isEditMode) {
                            isMenuVisible = true
                            touchOffset = offset
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { _, dragAmount ->
                        if (isMenuVisible) {
                            touchOffset += dragAmount
                            val dx = touchOffset.x - centerPoint.x
                            val dy = touchOffset.y - centerPoint.y
                            val distance = sqrt(dx * dx + dy * dy)
                            
                            if (distance > 25f) {
                                val angle = atan2(dy, dx) * (180 / PI).toFloat()
                                if (dx > 45) { // Confirmed App Selection Zone
                                    selectedSlotLeft = -1 
                                    val normalized = (angle + 70).coerceIn(0f, 140f)
                                    selectedSlotRight = (normalized / 140f * 6).roundToInt()
                                } else if (dx < -110) { // Confirmed Directory Selection Zone (Push to select)
                                    selectedSlotRight = -1
                                    val adjAngle = if (angle < 0) angle + 360 else angle
                                    val normalized = (220 - adjAngle).coerceIn(0f, 80f)
                                    selectedSlotLeft = (normalized / 80f * (directories.size - 1)).roundToInt()
                                } else if (dx < -30 && selectedSlotLeft == -1) {
                                    // Initial "hover" when moving out from center
                                    val adjAngle = if (angle < 0) angle + 360 else angle
                                    val normalized = (220 - adjAngle).coerceIn(0f, 80f)
                                    selectedSlotLeft = (normalized / 80f * (directories.size - 1)).roundToInt()
                                } else if (dx > -30 && dx < 45) {
                                    // Central "Safe" Transition Zone: Latch the current side's selection
                                    selectedSlotRight = -1
                                    // selectedSlotLeft stays as-is (LATCHED) until we cross +45
                                }
                            } else {
                                selectedSlotRight = -1
                                selectedSlotLeft = -1
                            }
                        }
                    },
                    onDragEnd = { isMenuVisible = false; selectedSlotRight = -1; selectedSlotLeft = -1 },
                    onDragCancel = { isMenuVisible = false; selectedSlotRight = -1; selectedSlotLeft = -1 }
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
                            isEditMode = !isEditMode
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isEditMode) Color(0xFFF85149).copy(alpha = 0.4f) else Color(0xFF58A6FF).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, if (isEditMode) Color(0xFFF85149) else Color(0xFF58A6FF).copy(alpha = 0.5f), CircleShape)
        ) {
            if (isEditMode) {
                Icon(
                    Icons.Default.Edit, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(24.dp).align(Alignment.Center)
                )
            }
        }

        // --- Main Launch Menu ---
        AnimatedVisibility(
            visible = isMenuVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f)
        ) {
            val centerDpX = with(density) { centerPoint.x.toDp() }
            val centerDpY = with(density) { centerPoint.y.toDp() }

            Box(modifier = Modifier.fillMaxSize()) {
                
                // --- Right Panel (Apps) ---
                TechPanel(
                    centerDpX = centerDpX, centerDpY = centerDpY,
                    isRight = true, title = directories[currentDirIndex]
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TechMenuItem(label = "UP", icon = Icons.Default.KeyboardArrowUp, isSelected = selectedSlotRight == 0, isEnabled = scrollIndex > 0)
                        displayItems.forEachIndexed { i, label ->
                            TechMenuItem(label = label, isSelected = selectedSlotRight == i + 1)
                        }
                        TechMenuItem(label = "DOWN", icon = Icons.Default.KeyboardArrowDown, isSelected = selectedSlotRight == 6, isEnabled = scrollIndex < currentItems.size - maxVisible)
                    }
                }

                // --- Left Panel (Directories) ---
                TechPanel(
                    centerDpX = centerDpX, centerDpY = centerDpY,
                    isRight = false, title = "DIR",
                    panelWidth = 120.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                        directories.forEachIndexed { i, label ->
                            val isActive = i == currentDirIndex
                            TechMenuItem(
                                label = label,
                                isSelected = selectedSlotLeft == i,
                                isRight = false,
                                isPrimary = isActive,
                                itemWidth = 100.dp
                            )
                        }
                    }
                }
            }
        }

        // --- Management Overlay ---
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            ManagementOverlay(onClose = { isEditMode = false })
        }
        
        // --- Lean Indicator ---
        if (isMenuVisible) {
            LeanIndicator(centerPoint, touchOffset, density)
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
    
    Box(modifier = Modifier.offset(x = xOffset, y = centerDpY - 180.dp)) {
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
fun ManagementOverlay(onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117).copy(alpha = 0.95f)).padding(32.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("DIRECTORY MANAGER", color = Color(0xFFF85149), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.pointerInput(Unit) { detectTapGestures { onClose() } })
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Edit directories, rename them, or drag apps to reorder.", color = Color(0xFF8B949E), fontSize = 14.sp)
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

@Preview
@Composable
fun PreviewRaunchScreen() {
    RaunchScreen()
}
