package com.adamfoerster.promisedland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.key
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.adamfoerster.promisedland.game.GeneralPlacementInfo
import com.adamfoerster.promisedland.game.HexagonData
import com.adamfoerster.promisedland.ui.playerColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import kotlin.math.*

enum class ZoomPreset(val scale: Float) {
    OUT(0.5f), 
    INTERMEDIATE(1.5f),
    IN(3.5f)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HexMap(
    modifier: Modifier = Modifier,
    turnKey: String = "",
    onHexSelected: (HexagonData) -> Unit,
    selectedHex: HexagonData?,
    onZoomCycleReady: ((() -> Unit) -> Unit)? = null,
    hexagons: Map<Pair<Int, Int>, HexagonData> = emptyMap(),
    generalPlacements: List<GeneralPlacementInfo> = emptyList(),
    reachableHexes: Set<Pair<Int, Int>> = emptySet(),
    scrollToHexTarget: Pair<Int, Int>? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val currentOnHexSelected by rememberUpdatedState(onHexSelected)
    val columns = 10
    val rows = 25
    val hexSize = 60f 

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val scale = remember { Animatable(0.5f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var currentZoomPreset by remember { mutableStateOf(ZoomPreset.OUT) }

    val mapWidth = sqrt(3f) * hexSize * (columns + 0.5f)
    val mapHeight = hexSize * (1.5f * rows + 0.5f)

    val xIcon = rememberVectorPainter(Icons.Default.Clear)
    val villageIcon = rememberVectorPainter(Icons.Default.Home)
    val cityIcon = rememberVectorPainter(Icons.Default.Place)

    val animatedPlacements = generalPlacements.map { p ->
        key(p.id) {
            val targetX = sqrt(3f) * hexSize * (p.hexCol + 0.5f * (p.hexRow % 2)) + (sqrt(3f) * hexSize / 2f)
            val targetY = 1.5f * hexSize * p.hexRow + hexSize
            val animOffset by animateOffsetAsState(
                targetValue = Offset(targetX, targetY),
                animationSpec = tween(700)
            )
            Triple(p, animOffset.x, animOffset.y)
        }
    }

    LaunchedEffect(containerSize) {
        if (containerSize.width > 0 && containerSize.height > 0) {
            val scaleX = containerSize.width.toFloat() / mapWidth
            val scaleY = containerSize.height.toFloat() / mapHeight
            val fitScale = minOf(scaleX, scaleY)
            if (currentZoomPreset == ZoomPreset.OUT) {
                scale.snapTo(fitScale)
                val offsetX = (containerSize.width - mapWidth * fitScale) / 2f
                val offsetY = (containerSize.height - mapHeight * fitScale) / 2f
                offset.snapTo(Offset(offsetX, offsetY))
            }
        }
    }

    LaunchedEffect(turnKey, containerSize) {
        if (turnKey.isNotEmpty() && containerSize.width > 0 && containerSize.height > 0) {
            val fitScale = minOf(
                containerSize.width.toFloat() / mapWidth,
                containerSize.height.toFloat() / mapHeight
            )
            val offsetX = (containerSize.width - mapWidth * fitScale) / 2f
            val offsetY = (containerSize.height - mapHeight * fitScale) / 2f
            currentZoomPreset = ZoomPreset.OUT
            launch { scale.animateTo(fitScale, tween(350)) }
            launch { offset.animateTo(Offset(offsetX, offsetY), tween(350)) }
        }
    }

    LaunchedEffect(scrollToHexTarget) {
        if (scrollToHexTarget != null && containerSize.width > 0 && containerSize.height > 0) {
            val targetHex = hexagons[scrollToHexTarget]
            if (targetHex != null) {
                val hexX = sqrt(3f) * hexSize * (targetHex.col + 0.5f * (targetHex.row % 2)) + (sqrt(3f) * hexSize / 2f)
                val hexY = 1.5f * hexSize * targetHex.row + hexSize
                val centerX = containerSize.width / 2f
                val centerY = containerSize.height / 2f
                val targetScale = ZoomPreset.IN.scale
                val targetOffset = Offset(centerX - hexX * targetScale, centerY - hexY * targetScale)
                launch { scale.animateTo(targetScale, tween(500)) }
                launch { offset.animateTo(targetOffset, tween(500)) }
                currentZoomPreset = ZoomPreset.IN
            }
        }
    }

    fun animateToHex(hex: HexagonData, targetScale: Float) {
        coroutineScope.launch {
            val hexX = sqrt(3f) * hexSize * (hex.col + 0.5f * (hex.row % 2)) + (sqrt(3f) * hexSize / 2f)
            val hexY = 1.5f * hexSize * hex.row + hexSize
            val centerX = containerSize.width / 2f
            val centerY = containerSize.height / 2f
            val targetOffset = Offset(centerX - hexX * targetScale, centerY - hexY * targetScale)
            launch { scale.animateTo(targetScale, tween(500)) }
            launch { offset.animateTo(targetOffset, tween(500)) }
        }
    }

    fun cycleZoom() {
        val nextPreset = when (currentZoomPreset) {
            ZoomPreset.OUT -> ZoomPreset.INTERMEDIATE
            ZoomPreset.INTERMEDIATE -> ZoomPreset.IN
            ZoomPreset.IN -> ZoomPreset.OUT
        }
        currentZoomPreset = nextPreset
        val fitScale = minOf(containerSize.width.toFloat() / mapWidth, containerSize.height.toFloat() / mapHeight)
        val targetScale = if (nextPreset == ZoomPreset.OUT) fitScale else nextPreset.scale

        coroutineScope.launch {
            if (nextPreset == ZoomPreset.OUT) {
                val offsetX = (containerSize.width - mapWidth * fitScale) / 2f
                val offsetY = (containerSize.height - mapHeight * fitScale) / 2f
                launch { scale.animateTo(targetScale, tween(500)) }
                launch { offset.animateTo(Offset(offsetX, offsetY), tween(500)) }
            } else if (selectedHex != null) {
                animateToHex(selectedHex, targetScale)
            } else {
                launch { scale.animateTo(targetScale, tween(500)) }
            }
        }
    }

    LaunchedEffect(onZoomCycleReady) {
        onZoomCycleReady?.invoke(::cycleZoom)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val normalizedX = (tapOffset.x - offset.value.x) / scale.value
                    val normalizedY = (tapOffset.y - offset.value.y) / scale.value
                    val clickedHex = findHexAt(normalizedX, normalizedY, hexSize)
                    if (clickedHex != null && clickedHex.col in 0 until columns && clickedHex.row in 0 until rows) {
                        val hexData = hexagons[clickedHex.col to clickedHex.row] ?: clickedHex
                        if (hexData.isActive) {
                            currentOnHexSelected(hexData)
                            animateToHex(hexData, ZoomPreset.IN.scale)
                            currentZoomPreset = ZoomPreset.IN
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    coroutineScope.launch {
                        val oldScale = scale.value
                        val newScale = (oldScale * zoom).coerceIn(0.1f, 10f)
                        scale.snapTo(newScale)
                        val newOffset = centroid - (centroid - offset.value) * (newScale / oldScale) + pan
                        offset.snapTo(newOffset)
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer {
            translationX = offset.value.x
            translationY = offset.value.y
            scaleX = scale.value
            scaleY = scale.value
            transformOrigin = TransformOrigin(0f, 0f)
        }) {
            MapBackground(mapWidth, mapHeight)

            Canvas(modifier = Modifier.size(mapWidth.dp, mapHeight.dp)) {
                for (r in 0 until rows) {
                    for (c in 0 until columns) {
                        val hexData = hexagons[c to r] ?: HexagonData(c, r)
                        val hexPath = createHexPath(c, r, hexSize)
                        
                        // Fill background color based on terrain
                        val terrainColor = when (hexData.terrain) {
                            "water" -> Color(0xFF0D47A1) // Dark Blue
                            "plain" -> Color(0xFF3E2723) // Dark Brown
                            "mountains" -> Color(0xFF616161) // Gray
                            "desert" -> Color(0xFFEDC9AF) // Sand color
                            else -> Color.Transparent
                        }
                        
                        if (terrainColor != Color.Transparent) {
                            drawPath(
                                path = hexPath,
                                color = terrainColor,
                                style = Fill
                            )
                        }

                        // Draw hex border
                        drawPath(
                            path = hexPath, 
                            color = if (hexData.isActive) Color.White.copy(alpha = 0.3f) else Color.Red.copy(alpha = 0.5f), 
                            style = Stroke(width = 1f)
                        )
                        
                        // Highlight selected
                        if (selectedHex?.col == c && selectedHex.row == r) {
                            drawPath(path = hexPath, color = Color.Green, style = Stroke(width = 4f))
                            drawPath(path = hexPath, color = Color.Green.copy(alpha = 0.2f), style = Fill)
                        }
                        
                        // Highlight reachable
                        if (reachableHexes.contains(c to r)) {
                            drawPath(path = hexPath, color = Color.Green, style = Stroke(width = 4f))
                            drawPath(path = hexPath, color = Color.Green.copy(alpha = 0.2f), style = Fill)
                        }

                        // Draw Icons
                        val centerX = sqrt(3f) * hexSize * (c + 0.5f * (r % 2)) + (sqrt(3f) * hexSize / 2f)
                        val centerY = 1.5f * hexSize * r + hexSize
                        val iconSize = 30f

                        if (!hexData.isActive) {
                            translate(centerX - iconSize / 2, centerY - iconSize / 2) {
                                with(xIcon) {
                                    draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Color.Red))
                                }
                            }
                        } else if (hexData.type == "village") {
                            translate(centerX - iconSize / 2, centerY - iconSize / 2) {
                                with(villageIcon) {
                                    draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Color.White))
                                }
                            }
                        } else if (hexData.type == "city") {
                            translate(centerX - iconSize / 2, centerY - iconSize / 2) {
                                with(cityIcon) {
                                    draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(Color.Yellow))
                                }
                            }
                        }
                    }
                }

                // Draw general icons
                val groupedByTarget = animatedPlacements.groupBy { it.first.hexCol to it.first.hexRow }
                animatedPlacements.forEach { (placement, centerX, centerY) ->
                    val targetGroup = groupedByTarget[placement.hexCol to placement.hexRow] ?: emptyList()
                    val idx = targetGroup.indexOfFirst { it.first.id == placement.id }.coerceAtLeast(0)
                    
                    val generalIconSize = 20f
                    val iconSize = 30f // From inner loop
                    val playerColor = playerColors.find { it.first == placement.playerColor }?.second ?: Color.White
                    
                    val offsetX = if (idx == 0) -generalIconSize * 0.6f else generalIconSize * 0.6f
                    val offsetY = iconSize * 0.5f
                    val gx = centerX + offsetX - generalIconSize / 2
                    val gy = centerY + offsetY - generalIconSize / 2
                    
                    val shieldPath = Path().apply {
                        val sx = gx + generalIconSize / 2
                        val sy = gy
                        moveTo(sx - generalIconSize * 0.4f, sy)
                        lineTo(sx + generalIconSize * 0.4f, sy)
                        lineTo(sx + generalIconSize * 0.4f, sy + generalIconSize * 0.5f)
                        lineTo(sx, sy + generalIconSize)
                        lineTo(sx - generalIconSize * 0.4f, sy + generalIconSize * 0.5f)
                        close()
                    }
                    drawPath(path = shieldPath, color = playerColor, style = Fill)
                    drawPath(path = shieldPath, color = Color.White, style = Stroke(width = 1.5f))
                }
            }
        }

    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MapBackground(width: Float, height: Float) {
    Box(modifier = Modifier.size(width.dp, height.dp).background(Color(0xFF242424))) {
        Image(
            painter = painterResource("map_background.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

fun createHexPath(col: Int, row: Int, size: Float): Path {
    val x = sqrt(3f) * size * (col + 0.5f * (row % 2)) + (sqrt(3f) * size / 2f)
    val y = 1.5f * size * row + size
    return Path().apply {
        for (i in 0 until 6) {
            val angleDeg = 60f * i - 30f
            val angleRad = angleDeg * PI / 180f
            val px = x + size * cos(angleRad).toFloat()
            val py = y + size * sin(angleRad).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
}

fun findHexAt(x: Float, y: Float, size: Float): HexagonData? {
    val startX = x - (sqrt(3f) * size / 2f)
    val startY = y - size
    val q = (sqrt(3f) / 3f * startX - 1f / 3f * startY) / size
    val r = (2f / 3f * startY) / size
    return cubeToOffset(cubeRound(q, r, -q - r))
}

fun cubeRound(q: Float, r: Float, s: Float): Triple<Int, Int, Int> {
    var rq = round(q).toInt()
    var rr = round(r).toInt()
    var rs = round(s).toInt()
    val dq = abs(rq - q)
    val dr = abs(rr - r)
    val ds = abs(rs - s)
    if (dq > dr && dq > ds) rq = -rr - rs else if (dr > ds) rr = -rq - rs else rs = -rq - rr
    return Triple(rq, rr, rs)
}

fun cubeToOffset(cube: Triple<Int, Int, Int>): HexagonData {
    val col = cube.first + (cube.second - (cube.second and 1)) / 2
    val row = cube.second
    return HexagonData(col, row)
}
