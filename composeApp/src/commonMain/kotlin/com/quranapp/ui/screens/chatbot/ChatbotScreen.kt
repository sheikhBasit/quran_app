package com.quranapp.ui.screens.chatbot

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranapp.domain.model.ChatMessage
import com.quranapp.domain.model.ChatRole
import com.quranapp.viewmodel.ChatbotUiState
import com.quranapp.viewmodel.ChatbotViewModel

@OptIn(ExperimentalMaterial3Api::class)
object ChatbotScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<ChatbotViewModel>()
        val uiState by viewModel.uiState.collectAsState(ChatbotUiState())
        var inputText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()
        val navigator = LocalNavigator.currentOrThrow

        // Auto-scroll to bottom when new messages arrive or content updates
        LaunchedEffect(uiState.messages.lastOrNull()?.content, uiState.messages.size) {
            if (uiState.messages.isNotEmpty()) {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("AI Quran Assistant") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask anything about Islam...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            ),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !uiState.isLoading,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (uiState.messages.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Salam! I'm your AI assistant.",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Ask me anything about the Quran, Hadith, or Islamic history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            ChatBubble(message)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isUser && !message.isStreaming && message.content.contains("RELEVANT AYAHS")) {
            // Parsed structured response
            AnimatedVisibility(
                visible = !message.isStreaming,
                enter = fadeIn(animationSpec = tween(500))
            ) {
                AssistantResponseParsed(message.content)
            }
        } else {
            // Standard bubble (User or Streaming Assistant)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(containerColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 300.dp)
            ) {
                if (message.isLoading && message.content.isEmpty()) {
                    TypingIndicator()
                } else {
                    Text(
                        text = message.content,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition()
    val dotAlphas = (0..2).map { index ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 600
                    0.2f at index * 100
                    1f at index * 100 + 200
                    0.2f at index * 100 + 400
                },
                repeatMode = RepeatMode.Restart
            )
        ).value
    }

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dotAlphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun AssistantResponseParsed(content: String) {
    val sections = remember(content) { parseResponse(content) }
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sections.forEach { section ->
            ResponseSectionCard(section)
        }
    }
}

data class ResponseSection(
    val title: String,
    val icon: ImageVector,
    val content: String,
    val color: Color
)

fun parseResponse(content: String): List<ResponseSection> {
    val sections = mutableListOf<ResponseSection>()
    
    val markers = listOf(
        Triple("RELEVANT AYAHS", Icons.Default.MenuBook, Color(0xFF2E7D32)), // Green
        Triple("TAFSIR", Icons.Default.LibraryBooks, Color(0xFF1565C0)),      // Blue
        Triple("RELATED HADITHS", Icons.Default.FormatQuote, Color(0xFFEF6C00)), // Amber
        Triple("SCHOLARLY REASONING", Icons.Default.Psychology, Color(0xFF7B1FA2)) // Purple
    )

    var currentText = content
    markers.forEachIndexed { index, (title, icon, color) ->
        val currentMarker = title
        val nextMarker = markers.getOrNull(index + 1)?.first
        
        val start = currentText.indexOf(currentMarker, ignoreCase = true)
        if (start != -1) {
            val end = if (nextMarker != null) {
                currentText.indexOf(nextMarker, start, ignoreCase = true)
            } else {
                currentText.length
            }
            
            val sectionContent = if (end != -1) {
                currentText.substring(start + currentMarker.length, end).trim()
            } else {
                currentText.substring(start + currentMarker.length).trim()
            }
            
            if (sectionContent.isNotEmpty()) {
                sections.add(ResponseSection(title, icon, removeHeaderDecorations(sectionContent), color))
            }
        }
    }
    
    return sections
}

fun removeHeaderDecorations(text: String): String {
    return text.replaceFirst(Regex("^[- \\n📖📚📿🤔]+", RegexOption.IGNORE_CASE), "").trim()
}

@Composable
fun ResponseSectionCard(section: ResponseSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
        ) {
            // Colored status bar on left
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(section.color)
            )
            
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        section.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = section.color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = section.color,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (section.title == "RELEVANT AYAHS") {
                    AyahSectionContent(section.content)
                } else {
                    Text(
                        text = section.content,
                        style = if (section.title == "SCHOLARLY REASONING") 
                            MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                        else MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AyahSectionContent(content: String) {
    // Basic Arabic detection for better visual alignment
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content.split("\n\n").forEach { block ->
            if (block.any { it in '\u0600'..'\u06FF' }) {
                Text(
                    text = block,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 36.sp
                )
            } else {
                Text(
                    text = block,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
