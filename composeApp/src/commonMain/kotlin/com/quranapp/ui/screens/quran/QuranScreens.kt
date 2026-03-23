package com.quranapp.ui.screens.quran

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.foundation.pager.*
import com.quranapp.ui.component.*
import com.quranapp.viewmodel.QuranViewModel
import kotlinx.coroutines.launch

/** Surah list screen */
object QuranHomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getScreenModel<QuranViewModel>()
        val uiState by viewModel.uiState.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("The Holy Quran") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.error != null) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (uiState.surahs.isEmpty()) {
                    Text("No Surahs found in database.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    ArabicText(
                                        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                        fontSize = uiState.arabicFontSize.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "The Holy Quran — 114 Surahs",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        items(uiState.surahs) { surah ->
                            SurahItem(
                                surah = surah,
                                onClick = { navigator.push(QuranReaderScreen(surah.number)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Quran reader (scroll + page modes) */
data class QuranReaderScreen(val surahNumber: Int) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<QuranViewModel>()
        val uiState by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        val scope = rememberCoroutineScope()
        val tafsirSheetState = rememberModalBottomSheetState()
        var showTafsirSheet by remember { mutableStateOf(false) }
        var selectedAyahForTafsir by remember { mutableStateOf<com.quranapp.domain.model.Ayah?>(null) }
        
        val annotationSheetState = rememberModalBottomSheetState()
        var showAnnotationSheet by remember { mutableStateOf(false) }
        var selectedAyahForAnnotation by remember { mutableStateOf<com.quranapp.domain.model.Ayah?>(null) }

        // Load surah on first entry
        androidx.compose.runtime.LaunchedEffect(surahNumber) {
            viewModel.loadSurah(surahNumber)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val surah = uiState.surahs.find { it.number == surahNumber }
                        Text(surah?.nameTransliteration ?: "Surah $surahNumber")
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleTranslation() }) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Toggle Translation",
                                tint = if (uiState.showTranslation) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { viewModel.toggleReadingMode() }) {
                            Icon(
                                imageVector = if (uiState.readingMode == com.quranapp.domain.model.ReadingMode.SCROLL)
                                    Icons.Default.MenuBook
                                else
                                    Icons.Default.FormatListBulleted,
                                contentDescription = "Toggle Mode"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.readingMode) {
                    com.quranapp.domain.model.ReadingMode.SCROLL -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            items(uiState.ayahs) { ayah ->
                                    AyahItem(
                                        ayah = ayah,
                                        showTranslation = uiState.showTranslation,
                                        script = uiState.script,
                                        fontSize = uiState.arabicFontSize,
                                        onLongClick = { 
                                        selectedAyahForAnnotation = ayah
                                        showAnnotationSheet = true 
                                    },
                                    onTafsirClick = { 
                                        selectedAyahForTafsir = ayah
                                        viewModel.loadTafsir(ayah)
                                        showTafsirSheet = true 
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    com.quranapp.domain.model.ReadingMode.PAGE -> {
                        val pagerState = rememberPagerState(pageCount = { 604 }) // 604 pages in standard Madani Mushaf
                        
                        LaunchedEffect(pagerState.currentPage) {
                            viewModel.loadPage(pagerState.currentPage + 1)
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            reverseLayout = true // Quran pages go Right-to-Left
                        ) { pageIdx ->
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                items(uiState.ayahs) { ayah ->
                                    AyahItem(
                                        ayah = ayah,
                                        showTranslation = uiState.showTranslation,
                                        script = uiState.script,
                                        fontSize = uiState.arabicFontSize,
                                        onLongClick = { 
                                            selectedAyahForAnnotation = ayah
                                            showAnnotationSheet = true 
                                        },
                                        onTafsirClick = { 
                                            selectedAyahForTafsir = ayah
                                            viewModel.loadTafsir(ayah)
                                            showTafsirSheet = true 
                                        }
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheets
        if (showTafsirSheet && selectedAyahForTafsir != null) {
            ModalBottomSheet(
                onDismissRequest = { showTafsirSheet = false },
                sheetState = tafsirSheetState
            ) {
                TafsirBottomSheet(
                    ayah = selectedAyahForTafsir!!,
                    tafsiers = uiState.tafsiers,
                    onDismiss = { showTafsirSheet = false }
                )
            }
        }

        if (showAnnotationSheet && selectedAyahForAnnotation != null) {
            ModalBottomSheet(
                onDismissRequest = { showAnnotationSheet = false },
                sheetState = annotationSheetState
            ) {
                AnnotationMenu(
                    ayah = selectedAyahForAnnotation!!,
                    onBookmark = { /* TODO */ },
                    onHighlight = { /* TODO */ },
                    onNote = { /* TODO */ },
                    onShare = { /* TODO */ }
                )
            }
        }
    }
}
