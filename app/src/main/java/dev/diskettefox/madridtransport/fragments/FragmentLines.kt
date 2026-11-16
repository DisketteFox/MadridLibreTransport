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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import dev.diskettefox.madridtransport.LineActivity
import dev.diskettefox.madridtransport.R
import dev.diskettefox.madridtransport.api.ApiCall
import dev.diskettefox.madridtransport.api.ApiInterface
import dev.diskettefox.madridtransport.models.HelloModel
import dev.diskettefox.madridtransport.models.LineModel
import dev.diskettefox.madridtransport.ui.theme.MadridTransportTheme
import dev.diskettefox.madridtransport.ui.theme.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentLines : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MadridTransportTheme {
                    val apiInterface = remember { ApiCall.callApi().create(ApiInterface::class.java) }
                    LinesScreen(
                        lines = lineData,
                        apiInterface = apiInterface,
                        onLineClick = { line, colorName ->
                            val intent = Intent(context, LineActivity::class.java).apply {
                                putExtra("lineId", line.lineId)
                                putExtra("lineLabel", line.label)
                                putExtra("dsA", line.nameA)
                                putExtra("dsB", line.nameB)
                                putExtra("lineColor", colorName)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    companion object {
        private val lineData = mutableStateListOf<LineModel.Data>()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinesScreen(
    lines: SnapshotStateList<LineModel.Data>,
    apiInterface: ApiInterface? = null,
    onLineClick: (LineModel.Data, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
    }

    var isLoading by remember { mutableStateOf(lines.isEmpty()) }
    var isNoConnection by remember { mutableStateOf(false) }

    val accessToken = ApiCall.token

    LaunchedEffect(apiInterface) {
        if (apiInterface != null) {
            if (lines.isEmpty()) {
                isLoading = true
                apiInterface.getLines(accessToken).enqueue(object : Callback<LineModel> {
                    override fun onResponse(call: Call<LineModel>, response: Response<LineModel>) {
                        if (response.isSuccessful && response.body() != null) {
                            lines.clear()
                            response.body()?.data?.let { lines.addAll(it) }
                            isLoading = false
                        } else {
                            isNoConnection = true
                            isLoading = false
                        }
                    }
                    override fun onFailure(call: Call<LineModel>, t: Throwable) {
                        isNoConnection = true
                        isLoading = false
                    }
                })
            }
            
            // Ping
            apiInterface.getHello().enqueue(object : Callback<HelloModel> {
                override fun onResponse(call: Call<HelloModel>, response: Response<HelloModel>) {}
                override fun onFailure(call: Call<HelloModel>, t: Throwable) {
                    isNoConnection = true
                    isLoading = false
                    Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isSearchActive) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_activity_line),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LineSearchBar(
                lines = lines,
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                onLineClick = onLineClick
            )

            if (!isSearchActive) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading && lines.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else if (isNoConnection && lines.isEmpty()) {
                        NoConnectionView()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            items(
                                items = lines,
                                key = { it.lineId },
                                contentType = { "line_item" }
                            ) { line ->
                                LineItem(line, onLineClick)
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
fun LineSearchBar(
    lines: List<LineModel.Data>,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onLineClick: (LineModel.Data, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    val filteredLines = remember(query, lines.size) {
        if (query.isEmpty()) {
            lines
        } else {
            lines.filter {
                it.label.contains(query, ignoreCase = true) ||
                it.nameA.contains(query, ignoreCase = true) ||
                it.nameB.contains(query, ignoreCase = true) ||
                it.lineId.toString().contains(query)
            }
        }
    }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { queryText ->
                    if (queryText.isNotBlank()) {
                        val trimmed = queryText.trim()
                        val match = lines.find {
                            it.label.equals(trimmed, ignoreCase = true) ||
                            it.lineId.toString() == trimmed
                        } ?: filteredLines.firstOrNull()

                        if (match != null) {
                            onLineClick(match, getLineColors(match).colorName)
                            onActiveChange(false)
                        } else {
                            val intent = Intent(context, LineActivity::class.java).apply {
                                putExtra("lineId", trimmed.toIntOrNull() ?: 0)
                                putExtra("lineLabel", trimmed)
                                putExtra("dsA", "")
                                putExtra("dsB", "")
                                putExtra("lineColor", "blue")
                            }
                            context.startActivity(intent)
                            onActiveChange(false)
                        }
                    }
                },
                expanded = active,
                onExpandedChange = onActiveChange,
                placeholder = { Text(stringResource(R.string.search_lines)) },
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
        if (filteredLines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_connection),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(
                    items = filteredLines,
                    key = { it.lineId },
                    contentType = { "line_suggestion" }
                ) { line ->
                    LineItem(
                        line = line,
                        onLineClick = { selectedLine, colorName ->
                            onLineClick(selectedLine, colorName)
                            onActiveChange(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LineItem(
    line: LineModel.Data,
    onLineClick: (LineModel.Data, String) -> Unit
) {
    val colors = getLineColors(line)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLineClick(line, colors.colorName) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = line.label,
                    color = colors.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(colors.background, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.nameA,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = line.nameB,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun NoConnectionView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LinkOff,
            contentDescription = stringResource(R.string.description_chain),
            modifier = Modifier.size(90.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.no_connection),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

private data class LineColors(val background: Color, val text: Color, val colorName: String)

private fun getLineColors(line: LineModel.Data): LineColors {
    return when (line.lineId) {
        in 501..599 -> LineColors(black, gold, "black")
        203 -> LineColors(gold, black, "gold")
        in 361..362 -> LineColors(turquoise, white, "turquoise")
        in 90..99 -> LineColors(lightBlue, white, "lightblue")
        in 452..457 -> LineColors(brown, white, "brown")
        in 601..603 -> LineColors(yellow, black, "yellow")
        else -> LineColors(blue, white, "blue")
    }
}

@Preview(showBackground = true)
@Composable
fun FragmentLinesPreview() {
    val mockLines = remember {
        mutableStateListOf(
            LineModel.Data().apply {
                lineId = 1
                label = "1"
                nameA = "PROSPERIDAD"
                nameB = "CRISTO REY"
            },
            LineModel.Data().apply {
                lineId = 203
                label = "E1"
                nameA = "CIBELES"
                nameB = "AEROPUERTO"
            },
            LineModel.Data().apply {
                lineId = 501
                label = "N1"
                nameA = "CIBELES"
                nameB = "SANCHINARRO"
            }
        )
    }

    MadridTransportTheme {
        LinesScreen(lines = mockLines, apiInterface = null)
    }
}
