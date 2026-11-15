package dev.diskettefox.madridtransport

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diskettefox.madridtransport.api.ApiCall
import dev.diskettefox.madridtransport.api.ApiInterface
import dev.diskettefox.madridtransport.api.TimeRequest
import dev.diskettefox.madridtransport.models.StopModel
import dev.diskettefox.madridtransport.models.TimeModel
import dev.diskettefox.madridtransport.models.TokenModel
import dev.diskettefox.madridtransport.ui.theme.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StopActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val stopId = intent.getStringExtra("stopId") ?: intent.data?.getQueryParameter("stopnum")

        setContent {
            MadridTransportTheme {
                StopScreen(
                    initialStopId = stopId,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopScreen(
    initialStopId: String?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var stopId by remember { mutableStateOf(initialStopId ?: "") }
    var stopName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var linesList by remember { mutableStateOf(listOf<StopModel.Dataline>()) }
    val arrivalTimes = remember { mutableStateMapOf<String, Pair<String, String>>() }
    var stop by remember { mutableStateOf<StopModel.Stop?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var customFavoriteName by remember { mutableStateOf("") }

    fun updateFavoriteState() {
        isFavorite = if (stop != null) {
            stop!!.isFavorite()
        } else if (stopId.isNotEmpty()) {
            FavoritesManager.isFavorite(context, stopId)
        } else {
            false
        }
    }

    fun formatTime(timeInSeconds: String): String {
        return try {
            val seconds = timeInSeconds.toInt()
            if (seconds >= 999999) "Delayed"
            else if (seconds < 60) "Arriving"
            else "${seconds / 60} min"
        } catch (_: NumberFormatException) {
            timeInSeconds
        }
    }

    fun fetchArrivalTimes(stopIdInt: Int, apiInterface: ApiInterface) {
        if (linesList.isEmpty()) {
            isLoading = false
            return
        }

        var responsesReceived = 0
        val totalLines = linesList.size

        for ((lineId1) in linesList) {
            val lineIdStr = lineId1
            try {
                val lineId = lineIdStr.toInt()
                Log.d("StopActivity", "Fetching time for line: $lineId at stop: $stopIdInt")
                val timeCall = apiInterface.getTime(stopIdInt, lineId, ApiCall.token, TimeRequest.get())

                timeCall.enqueue(object : Callback<TimeModel> {
                    override fun onResponse(call: Call<TimeModel>, response: Response<TimeModel>) {
                        if (response.isSuccessful && response.body() != null) {
                            val timeModel = response.body()!!
                            if (timeModel.data.isNotEmpty()) {
                                val times = timeModel.data[0].lines
                                if (times.isNotEmpty()) {
                                    val arriving = formatTime(times[0].time)
                                    val next = if (times.size > 1) formatTime(times[1].time) else "---"
                                    arrivalTimes[lineIdStr] = Pair(arriving, next)
                                    Log.d("StopActivity", "Time received for line $lineIdStr: arriving=$arriving, next=$next")
                                } else {
                                    arrivalTimes[lineIdStr] = Pair("---", "---")
                                }
                            } else {
                                arrivalTimes[lineIdStr] = Pair("---", "---")
                            }
                        } else {
                            Log.e("StopActivity", "Time response not successful for line $lineIdStr: code=${response.code()}")
                            arrivalTimes[lineIdStr] = Pair("---", "---")
                        }
                        responsesReceived++
                        if (responsesReceived == totalLines) {
                            isLoading = false
                        }
                    }

                    override fun onFailure(call: Call<TimeModel>, t: Throwable) {
                        Log.e("StopActivity", "Error fetching times for line $lineIdStr", t)
                        Toast.makeText(context, R.string.error_time, Toast.LENGTH_SHORT).show()
                        arrivalTimes[lineIdStr] = Pair("---", "---")
                        responsesReceived++
                        if (responsesReceived == totalLines) {
                            isLoading = false
                        }
                    }
                })
            } catch (e: NumberFormatException) {
                Log.e("StopActivity", "Invalid line ID: $lineIdStr", e)
                Toast.makeText(context, R.string.invalid_line, Toast.LENGTH_SHORT).show()
                arrivalTimes[lineIdStr] = Pair("---", "---")
                responsesReceived++
                if (responsesReceived == totalLines) {
                    isLoading = false
                }
            }
        }
    }

    fun fetchStopDetails(stopIdInt: Int) {
        val apiInterface = ApiCall.callApi().create(ApiInterface::class.java)
        val call = apiInterface.getStop(stopIdInt, ApiCall.token)

        call.enqueue(object : Callback<StopModel> {
            override fun onResponse(call: Call<StopModel>, response: Response<StopModel>) {
                if (response.isSuccessful && response.body() != null) {
                    val stopModel = response.body()!!

                    when (stopModel.code) {
                        "80" -> {
                            Toast.makeText(context, R.string.error_token, Toast.LENGTH_SHORT).show()
                            (context as? ComponentActivity)?.finish()
                            return
                        }
                        "81" -> {
                            Toast.makeText(context, R.string.stop_error, Toast.LENGTH_SHORT).show()
                            (context as? ComponentActivity)?.finish()
                            return
                        }
                        "90" -> {
                            Toast.makeText(context, R.string.invalid_stop, Toast.LENGTH_SHORT).show()
                            (context as? ComponentActivity)?.finish()
                            return
                        }
                    }

                    if (stopModel.stopsData.isNotEmpty()) {
                        val stops = stopModel.stopsData[0].stops
                        if (stops.isNotEmpty()) {
                            val loadedStop = stops[0]
                            loadedStop.favorite = FavoritesManager.isFavorite(context, loadedStop.stopId)
                            stop = loadedStop
                            stopName = loadedStop.name
                            stopId = loadedStop.stopId
                            updateFavoriteState()

                            val dataLines = loadedStop.dataLine
                            if (dataLines.isNotEmpty()) {
                                arrivalTimes.clear()
                                linesList = dataLines
                                fetchArrivalTimes(stopIdInt, apiInterface)
                                return
                            }
                        }
                    }
                }
                isLoading = false
            }

            override fun onFailure(call: Call<StopModel>, t: Throwable) {
                Log.e("StopActivity", "Error fetching stop details", t)
                Toast.makeText(context, R.string.stop_error, Toast.LENGTH_SHORT).show()
                (context as? ComponentActivity)?.finish()
                isLoading = false
            }
        })
    }

    fun fetchTokenAndLoad(targetStopId: String) {
        val apiInterface = ApiCall.callApi().create(ApiInterface::class.java)
        val call = apiInterface.getToken(MainActivity.CLIENT_ID, MainActivity.PASS_KEY)

        call.enqueue(object : Callback<TokenModel> {
            override fun onResponse(call: Call<TokenModel>, response: Response<TokenModel>) {
                if (response.isSuccessful && response.body() != null) {
                    val tokenModel = response.body()!!
                    if ((tokenModel.code == 0 || tokenModel.code == 1) && tokenModel.data.isNotEmpty()) {
                        val token = tokenModel.data[0].accessToken
                        ApiCall.setToken(token)
                        try {
                            fetchStopDetails(targetStopId.toInt())
                        } catch (e: NumberFormatException) {
                            Log.e("StopActivity", "Invalid stop ID format", e)
                            Toast.makeText(context, R.string.invalid_stop, Toast.LENGTH_SHORT).show()
                            isLoading = false
                        }
                    } else {
                        Log.e("StopActivity", "Token error: ${tokenModel.code}")
                        Toast.makeText(context, "Error authenticating", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                } else {
                    Log.e("StopActivity", "Token request failed")
                    isLoading = false
                }
            }

            override fun onFailure(call: Call<TokenModel>, t: Throwable) {
                Log.e("StopActivity", "Token request failure", t)
                isLoading = false
            }
        })
    }

    LaunchedEffect(stopId) {
        if (stopId.isNotEmpty()) {
            isLoading = true
            updateFavoriteState()
            if (ApiCall.token.isEmpty()) {
                fetchTokenAndLoad(stopId)
            } else {
                try {
                    fetchStopDetails(stopId.toInt())
                } catch (e: NumberFormatException) {
                    Log.e("StopActivity", "Invalid stop ID format", e)
                    Toast.makeText(context, R.string.invalid_stop, Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    fun refreshData() {
        if (stopId.isNotEmpty()) {
            isLoading = true
            linesList = emptyList()
            arrivalTimes.clear()
            try {
                fetchStopDetails(stopId.toInt())
            } catch (e: NumberFormatException) {
                Log.e("StopActivity", "Error while refreshing", e)
                Toast.makeText(context, R.string.error_refreshing, Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        Surface(
                            onClick = onBackClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.chevron_left),
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.title_stop_details),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            val currentStopId = stop?.stopId ?: stopId
                            if (currentStopId.isNotEmpty()) {
                                if (isFavorite) {
                                    FavoritesManager.removeFavorite(context, currentStopId)
                                    Toast.makeText(context, R.string.message_favorite_remove, Toast.LENGTH_SHORT).show()
                                    isFavorite = false
                                    stop?.favorite = false
                                } else {
                                    FavoritesManager.addFavorite(context, currentStopId)
                                    isFavorite = true
                                    stop?.favorite = true
                                    customFavoriteName = stopName.ifEmpty { "favorito" }
                                    showFavoriteDialog = true
                                }
                            }
                        }) {
                            Icon(
                                painter = painterResource(
                                    id = if (isFavorite) R.drawable.ic_favorite_filled_24dp else R.drawable.ic_favorite_24dp
                                ),
                                contentDescription = stringResource(R.string.toolbar_favorite)
                            )
                        }
                        IconButton(onClick = { refreshData() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_update_24dp),
                                contentDescription = stringResource(R.string.toolbar_update)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { refreshData() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    item {
                        // Stop Info Card (busCard_Stop)
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(15.dp)
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stopId,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Text(
                                    text = stopName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 15.dp)
                                )
                            }
                        }
                    }

                    items(linesList, key = { it.lineId }) { line ->
                        val times = arrivalTimes[line.lineId]
                        BusTimeCard(
                            line = line,
                            arrivingTime = times?.first,
                            nextTime = times?.second
                        )
                    }
                }
            }
        }
    }

    if (showFavoriteDialog) {
        AlertDialog(
            onDismissRequest = { showFavoriteDialog = false },
            title = { Text(stringResource(R.string.favorite_name)) },
            text = {
                OutlinedTextField(
                    value = customFavoriteName,
                    onValueChange = { customFavoriteName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.favorite_name)) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val finalName = customFavoriteName.ifEmpty { stopName.ifEmpty { "favorito" } }
                    FavoritesManager.addFavoriteWithName(context, stopId, finalName)
                    Toast.makeText(context, R.string.message_favorite_add, Toast.LENGTH_SHORT).show()
                    showFavoriteDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFavoriteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun BusTimeCard(
    line: StopModel.Dataline,
    arrivingTime: String?,
    nextTime: String?
) {
    val lineIdStr = line.lineId
    val lineIdInt = lineIdStr.toIntOrNull() ?: 0

    val (bgColor, numColor, dirColor) = when (lineIdInt) {
        in 501..599 -> Triple(black, black, gold)
        203 -> Triple(gold, gold, black)
        in 361..362 -> Triple(turquoise, turquoise, white)
        in 90..99 -> Triple(lightBlue, lightBlue, white)
        in 452..457 -> Triple(brown, brown, white)
        in 601..602 -> Triple(yellow, yellow, black)
        else -> Triple(blue, blue, white)
    }

    val directionText = if (line.direction == "A") line.headerA else line.headerB
    val timeArriving = arrivingTime ?: line.timeArriving
    val timeNext = nextTime ?: line.timeNext

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // card_color bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(bgColor)
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(55.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = line.label,
                        color = numColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = directionText,
                    color = dirColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
            }

            // Arriving row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bus),
                    contentDescription = stringResource(R.string.description_bus_icon),
                    modifier = Modifier
                        .size(30.dp)
                        .padding(4.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = stringResource(R.string.card_bus_arriving),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 5.dp)
                )
                Text(
                    text = timeArriving,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            // Next bus row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bus),
                    contentDescription = stringResource(R.string.description_bus_icon),
                    modifier = Modifier
                        .size(30.dp)
                        .padding(4.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text(
                    text = stringResource(R.string.card_bus_next),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 5.dp)
                )
                Text(
                    text = timeNext,
                    fontSize = 20.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
