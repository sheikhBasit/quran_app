package com.quranapp.ui.screens.qibla

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranapp.viewmodel.QiblaViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object QiblaScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<QiblaViewModel>()
        val currentBearing by viewModel.direction.collectAsState()
        val qiblaBearing by viewModel.qiblaBearing.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        // Calculate rotation for the needle
        // The needle should point towards (QiblaBearing - CurrentBearing)
        val rotation = (qiblaBearing - currentBearing).toFloat()
        
        val animatedRotation by animateFloatAsState(
            targetValue = rotation,
            animationSpec = spring(stiffness = Spring.StiffnessLow)
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
                Text(
                    text = "Point your phone towards the Kaaba",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                CompassUI(animatedRotation)
                
                Spacer(modifier = Modifier.height(48.dp))
                
                Card(
                    modifier = Modifier.padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Place, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Qibla Direction",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${qiblaBearing.toInt()}° North-East",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CompassUI(rotation: Float) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Compass background circle
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {}

            // Cardinal points (Fixed)
            CardinalPoints()

            // The rotating part (Needle)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation),
                contentAlignment = Alignment.Center
            ) {
                // Large arrow pointing to Kaaba
                Icon(
                    imageVector = Icons.Default.NavigateNext,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(-90f) // Point up
                        .offset(y = (-40).dp), // Move to top of circle
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Kaaba Icon at the tip
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = (-110).dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🕋", fontSize = 20.sp)
                }
            }
        }
    }

    @Composable
    private fun CardinalPoints() {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(8.dp), fontWeight = FontWeight.Bold, color = Color.Red)
            Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp), fontWeight = FontWeight.Bold)
            Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp), fontWeight = FontWeight.Bold)
            Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(8.dp), fontWeight = FontWeight.Bold)
        }
    }
}
