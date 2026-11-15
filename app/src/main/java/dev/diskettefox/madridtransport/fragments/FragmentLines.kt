package dev.diskettefox.madridtransport.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var searchQuery by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(lines.isEmpty()) }
    var isNoConnection by remember { mutableStateOf(false) }

    val filteredLines = remember(searchQuery, lines.size) {
        if (searchQuery.isEmpty()) {
            lines
        } else {
            lines.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { },
                        expanded = false,
                        onExpandedChange = { },
                        placeholder = { Text(stringResource(R.string.search_lines)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                },
                expanded = false,
                onExpandedChange = { },
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) { }

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
                        items(filteredLines) { line ->
                            LineItem(line, onLineClick)
                        }
                    }
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
                .padding(14.dp)
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

            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                Text(
                    text = line.nameA,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = line.nameB,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
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

@Composable
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
