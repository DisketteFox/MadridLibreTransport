package dev.diskettefox.madridtransport.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.diskettefox.madridtransport.FavoritesManager
import dev.diskettefox.madridtransport.R
import dev.diskettefox.madridtransport.Settings
import dev.diskettefox.madridtransport.StopActivity
import dev.diskettefox.madridtransport.models.StopModel
import dev.diskettefox.madridtransport.ui.theme.MadridTransportTheme
import dev.diskettefox.madridtransport.ui.theme.*
import dev.diskettefox.madridtransport.viewmodels.StopUiState
import dev.diskettefox.madridtransport.viewmodels.StopViewModel

class FragmentStop : Fragment() {
    private lateinit var viewModel: StopViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[StopViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MadridTransportTheme {
                    StopScreen(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refreshFavoritesIfNeeded()
        }
    }

    fun refreshFavorites() {
        if (::viewModel.isInitialized) {
            viewModel.refreshFavorites()
        }
    }

    fun fetchAllStops() {
        if (::viewModel.isInitialized) {
            viewModel.fetchAllStops()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopScreen(viewModel: StopViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedStopId by remember { mutableStateOf<String?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
    }

    val selectedStop = uiState.stopsList.find { it.stopId == selectedStopId }
    val selectedIndex = selectedStopId?.let { id ->
        uiState.stopsList.indexOfFirst { it.stopId == id }
    } ?: -1

    if (showRenameDialog && selectedStop != null) {
        var newNameInput by remember(selectedStop.stopId) {
            mutableStateOf(FavoritesManager.getFavoriteName(context, selectedStop.stopId) ?: selectedStop.name)
        }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.favorite_name)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nameToSave = if (newNameInput.isBlank()) selectedStop.name else newNameInput.trim()
                        FavoritesManager.addFavoriteWithName(context, selectedStop.stopId, nameToSave)
                        Toast.makeText(context, R.string.message_favorite_add, Toast.LENGTH_SHORT).show()
                        viewModel.refreshFavorites()
                        showRenameDialog = false
                        selectedStopId = null
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRenameDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isSearchActive) {
                if (selectedStopId != null) {
                    TopAppBar(
                        title = {
                            Text(
                                text = selectedStop?.let { FavoritesManager.getFavoriteName(context, it.stopId) ?: it.name } ?: "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedStopId = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close selection")
                            }
                        },
                        actions = {
                            if (selectedIndex > 0) {
                                IconButton(onClick = {
                                    val list = uiState.stopsList.toMutableList()
                                    val item = list.removeAt(selectedIndex)
                                    list.add(selectedIndex - 1, item)
                                    viewModel.updateFavoritesOrder(list)
                                }) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.move_up))
                                }
                            }
                            if (selectedIndex >= 0 && selectedIndex < uiState.stopsList.size - 1) {
                                IconButton(onClick = {
                                    val list = uiState.stopsList.toMutableList()
                                    val item = list.removeAt(selectedIndex)
                                    list.add(selectedIndex + 1, item)
                                    viewModel.updateFavoritesOrder(list)
                                }) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.move_down))
                                }
                            }
                            IconButton(onClick = { showRenameDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                            IconButton(onClick = {
                                selectedStop?.let { stop ->
                                    FavoritesManager.removeFavorite(context, stop.stopId)
                                    Toast.makeText(context, R.string.message_favorite_remove, Toast.LENGTH_SHORT).show()
                                    viewModel.refreshFavorites()
                                    selectedStopId = null
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.title_activity_stop),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            )
                        },
                        actions = {
                            IconButton(onClick = {
                                context.startActivity(Intent(context, Settings::class.java))
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.title_activity_settings)
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            StopSearchBar(
                uiState = uiState,
                active = isSearchActive,
                onActiveChange = { isSearchActive = it }
            )

            if (!isSearchActive) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (uiState.isNoConnection) {
                        EmptyStateView(
                            icon = Icons.Default.LinkOff,
                            message = stringResource(R.string.no_connection)
                        )
                    } else if (uiState.isNoFavorites) {
                        EmptyStateView(
                            icon = Icons.Default.Star,
                            message = stringResource(R.string.no_favorites)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.stopsList,
                                key = { it.stopId },
                                contentType = { "stop_card" }
                            ) { stop ->
                                StopCard(
                                    stop = stop,
                                    isSelected = (stop.stopId == selectedStopId),
                                    onLongClick = { selectedStopId = stop.stopId },
                                    onClick = {
                                        if (selectedStopId != null) {
                                            selectedStopId = if (selectedStopId == stop.stopId) null else stop.stopId
                                        } else {
                                            val intent = Intent(context, StopActivity::class.java).apply {
                                                putExtra("stopId", stop.stopId)
                                                putExtra("stopName", stop.name)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopSearchBar(
    uiState: StopUiState,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    val filteredSuggestions = remember(query, uiState.allStops) {
        if (query.isEmpty()) uiState.allStops
        else uiState.allStops.filter {
            it.stopName.contains(query, ignoreCase = true) || it.stopId.contains(query)
        }
    }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = {
                    if (it.isNotEmpty()) {
                        val intent = Intent(context, StopActivity::class.java).apply {
                            putExtra("stopId", it)
                        }
                        context.startActivity(intent)
                        onActiveChange(false)
                    }
                },
                expanded = active,
                onExpandedChange = onActiveChange,
                placeholder = { Text(stringResource(R.string.search_stops)) },
                leadingIcon = {
                    if (active) {
                        IconButton(onClick = { onActiveChange(false) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
        },
        expanded = active,
        onExpandedChange = onActiveChange,
        windowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .fillMaxWidth()
            .then(if (active) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = if (active) 0.dp else 16.dp)
            .padding(bottom = if (active) 0.dp else 8.dp)
    ) {
        if (filteredSuggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.stop_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filteredSuggestions) { stop ->
                    ListItem(
                        headlineContent = { Text(stop.stopName) },
                        supportingContent = { Text(stop.stopId) },
                        modifier = Modifier.clickable {
                            val intent = Intent(context, StopActivity::class.java).apply {
                                putExtra("stopId", stop.stopId)
                                putExtra("stopName", stop.stopName)
                            }
                            context.startActivity(intent)
                            onActiveChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StopCard(
    stop: StopModel.Stop,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val favoriteName = remember(stop.stopId) {
        FavoritesManager.getFavoriteName(context, stop.stopId)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stop.stopId,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = favoriteName ?: stop.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val lines = stop.dataLine
                    if (lines.size < 6) {
                        lines.forEach { line ->
                            LineBadge(line.label, getLineColor(line.lineId))
                        }
                    } else {
                        for (i in 0 until 5) {
                            LineBadge(lines[i].label, getLineColor(lines[i].lineId))
                        }
                        LineBadge("...", blue)
                    }
                }
            }
        }
    }
}

@Composable
fun LineBadge(label: String, backgroundColor: Color) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(26.dp)
            .wrapContentWidth()
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            val textColor = when (backgroundColor) {
                black -> gold
                yellow, gold -> black
                else -> white
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyStateView(icon: ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(90.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun getLineColor(lineId: String): Color {
    val id = lineId.toIntOrNull() ?: return blue
    return when (id) {
        in 501..599 -> black
        203 -> gold
        in 361..362 -> turquoise
        in 90..99 -> lightBlue
        in 452..457 -> brown
        in 601..603 -> yellow
        else -> blue
    }
}
