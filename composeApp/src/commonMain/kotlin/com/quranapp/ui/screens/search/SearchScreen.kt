package com.quranapp.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.quranapp.domain.model.SearchResult
import com.quranapp.ui.component.ArabicText
import com.quranapp.ui.screens.quran.QuranReaderScreen
import com.quranapp.viewmodel.SearchScreenUiState
import com.quranapp.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
object SearchScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = getScreenModel<SearchViewModel>()
        val uiState by viewModel.uiState.collectAsState(SearchScreenUiState())
        val navigator = LocalNavigator.currentOrThrow

        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("Quran", "Hadith")

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(title = { Text("Global Search") })
                    
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search keywords, surah names...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )

                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    val count = if (index == 0) uiState.quranResults.size else uiState.hadithResults.size
                                    Text("$title ($count)") 
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (uiState.query.length < 3) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null, 
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            "Type at least 3 characters to search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    val results = if (selectedTabIndex == 0) uiState.quranResults else uiState.hadithResults
                    
                    if (results.isEmpty()) {
                        Text(
                            "No results found for \"${uiState.query}\"",
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(results) { result ->
                                when (result) {
                                    is SearchResult.AyahResult -> QuranResultItem(result) {
                                        navigator.push(QuranReaderScreen(result.ayah.surahNumber))
                                    }
                                    is SearchResult.HadithResult -> HadithResultItem(result)
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuranResultItem(result: SearchResult.AyahResult, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = { Text("Surah ${result.ayah.surahNumber} • Ayah ${result.ayah.ayahNumber}") },
        headlineContent = {
            Text(
                text = result.ayah.translationEnglish,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            ArabicText(
                text = result.ayah.arabicTextHafs,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    )
}

@Composable
fun HadithResultItem(result: SearchResult.HadithResult) {
    ListItem(
        overlineContent = { Text("${result.hadith.collection} • ${result.hadith.chapterName}") },
        headlineContent = {
            Text(
                text = result.hadith.translation,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "Hadith #${result.hadith.hadithNumber} • ${result.hadith.narrator}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
}
