package com.adamfoerster.promisedland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import kotlin.math.*

data class HexagonData(val col: Int, val row: Int) {
    val id: String = "${('A'.code + col).toChar()}${row + 1}"
}

enum class ZoomPreset(val scale: Float) {
    OUT(0.5f), 
    INTERMEDIATE(1.5f),
    IN(3.5f)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun HexMap(
    modifier: Modifier = Modifier,
    onHexSelected: (HexagonData) -> Unit,
    selectedHex: HexagonData?
) {
    val coroutineScope = rememberCoroutineScope()
    val columns = 8
    val rows = 25
    val hexSize = 60f 

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val scale = remember { Animatable(0.5f) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var currentZoomPreset by remember { mutableStateOf(ZoomPreset.OUT) }

    val mapWidth = sqrt(3f) * hexSize * (columns + 0.5f)
    val mapHeight = hexSize * (1.5f * rows + 0.5f)

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val normalizedX = (tapOffset.x - offset.value.x) / scale.value
                    val normalizedY = (tapOffset.y - offset.value.y) / scale.value
                    val hex = findHexAt(normalizedX, normalizedY, hexSize)
                    if (hex != null && hex.col in 0 until columns && hex.row in 0 until rows) {
                        onHexSelected(hex)
                        animateToHex(hex, ZoomPreset.IN.scale)
                        currentZoomPreset = ZoomPreset.IN
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
            // Map Background - Uses a placeholder if map_background.png is missing
            MapBackground(mapWidth, mapHeight)

            // Hex Grid Overlay
            Canvas(modifier = Modifier.size(mapWidth.dp, mapHeight.dp)) {
                for (r in 0 until rows) {
                    for (c in 0 until columns) {
                        val hexPath = createHexPath(c, r, hexSize)
                        drawPath(path = hexPath, color = Color.White.copy(alpha = 0.3f), style = Stroke(width = 1f))
                        if (selectedHex?.col == c && selectedHex?.row == r) {
                            drawPath(path = hexPath, color = Color.Green, style = Stroke(width = 4f))
                            drawPath(path = hexPath, color = Color.Green.copy(alpha = 0.2f), style = Fill)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { cycleZoom() },
            modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp),
            backgroundColor = Color.Black.copy(alpha = 0.6f),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Search, contentDescription = "Zoom Cycle")
        }

        if (selectedHex != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.8f),
                elevation = 8.dp
            ) {
                Text(
                    text = "Selected Region: ${selectedHex.id}",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MapBackground(width: Float, height: Float) {
    // Note: To fix the MissingResourceException, add 'map_background.png' to your resources.
    // We wrap this in a way that allows the rest of the UI to function if the file is missing.
    Box(modifier = Modifier.size(width.dp, height.dp).background(Color(0xFF242424))) {
//        Text(
//            "Place 'map_background.png' in resources folder",
//            modifier = Modifier.align(Alignment.Center).padding(20.dp),
//            color = Color.DarkGray,
//            textAlign = TextAlign.Center
//        )
        
        // Uncomment the Image block below once you have added the file to your resources
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
