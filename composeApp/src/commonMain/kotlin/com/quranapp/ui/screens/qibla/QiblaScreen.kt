package com.quranapp.ui.screens.qibla

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranapp.util.LocationPermissionRequest
import com.quranapp.util.MathUtils
import com.quranapp.viewmodel.QiblaViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object QiblaScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<QiblaViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        // Ensure location permission is requested on entry
        LocationPermissionRequest {}

        // State for continuous rotation tracking to prevent counter-rotation 
        var currentRotation by remember { mutableStateOf(0f) }
        
        // Calculate the next target rotation using shortest path logic
        val targetRotation = MathUtils.shortestRotation(
            current = currentRotation,
            target = (uiState.qiblaBearing - uiState.direction).toFloat()
        )
        
        val animatedRotation by animateFloatAsState(
            targetValue = targetRotation,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            finishedListener = { currentRotation = it }
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Qibla Finder", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main Compass Card
                Card(
                    modifier = Modifier.size(320.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CompassDial(animatedRotation)
                    }
                }

                Spacer(modifier = Modifier.height(56.dp))

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Qibla: ${uiState.qiblaBearing.toInt()}° ${MathUtils.getCardinalDirection(uiState.qiblaBearing)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Face this direction to pray",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CompassDial(rotation: Float) {
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            DegreesMarkers()
            CardinalLabels()

            // The rotating part (Green Needle + Kaaba Tip)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation),
                contentAlignment = Alignment.Center
            ) {
                // Qibla Arrow
                Icon(
                    imageVector = Icons.Default.NavigateNext,
                    contentDescription = null,
                    modifier = Modifier
                        .size(140.dp)
                        .rotate(-90f) // Point to top of phone
                        .offset(y = (-50).dp),
                    tint = Color(0xFF4CAF50)
                )

                // Kaaba Symbol at the top of the needle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .offset(y = (-120).dp)
                        .background(Color.Black, RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🕋", fontSize = 24.sp)
                }
            }
        }
    }

    @Composable
    private fun CardinalLabels() {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(12.dp), fontWeight = FontWeight.ExtraBold, color = Color.Red, fontSize = 20.sp)
            Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(12.dp), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
        }
    }

    @Composable
    private fun DegreesMarkers() {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.center
            val radius = size.minDimension / 2
            for (i in 0 until 360 step 15) {
                val angleRad = (i - 90) * (PI / 180f).toFloat()
                val lineLength = if (i % 45 == 0) 20.dp.toPx() else 10.dp.toPx()
                val thickness = if (i % 45 == 0) 3.dp.toPx() else 1.dp.toPx()
                val color = if (i % 45 == 0) Color.White else Color.Gray

                val start = Offset(
                    center.x + (radius - lineLength) * cos(angleRad),
                    center.y + (radius - lineLength) * sin(angleRad)
                )
                val end = Offset(
                    center.x + radius * cos(angleRad),
                    center.y + radius * sin(angleRad)
                )
                drawLine(color, start, end, thickness)
            }
        }
    }
}
