package dev.diskettefox.madridtransport.fragments

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import dev.diskettefox.madridtransport.ThemeManager
import dev.diskettefox.madridtransport.ThemeMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import dev.diskettefox.madridtransport.R
import dev.diskettefox.madridtransport.StopActivity
import dev.diskettefox.madridtransport.api.*
import dev.diskettefox.madridtransport.models.StopsModel
import dev.diskettefox.madridtransport.ui.theme.MadridTransportTheme
import org.mapsforge.core.model.*
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.model.common.Observer
import retrofit2.*

class FragmentMap : Fragment(), Observer {
    private var mapView: MapView? = null
    private var tileCache: TileCache? = null
    private var tileLayer: TileDownloadLayer? = null
    private var userMarker: Marker? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var canZoomIn by mutableStateOf(true)
    private var canZoomOut by mutableStateOf(true)

    companion object {
        private val DEFAULT_CENTER = LatLong(40.416974, -3.703634)
    }

    private val locCallback = object : LocationCallback() {
        override fun onLocationResult(lr: LocationResult) {
            lr.lastLocation?.let { updatePos(LatLong(it.latitude, it.longitude)) }
        }
    }

    private val permLauncher = registerForActivityResult(RequestMultiplePermissions()) {
        if (it.values.any { p -> p }) move() else toast(R.string.error_location)
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        AndroidGraphicFactory.createInstance(requireActivity().application)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MadridTransportTheme {
                    MapScreen(
                        canZoomIn = canZoomIn,
                        canZoomOut = canZoomOut,
                        onZoomIn = { mapView?.model?.mapViewPosition?.zoomIn() },
                        onZoomOut = { mapView?.model?.mapViewPosition?.zoomOut() },
                        onLocate = { move() },
                        mapViewFactory = { context ->
                            MapView(context).also { mv ->
                                mapView = mv
                                mv.apply {
                                    mapScaleBar.isVisible = true
                                    setBuiltInZoomControls(false)
                                    setZoomLevel(15.toByte())
                                    setZoomLevelMax(18.toByte())
                                    model.mapViewPosition.apply {
                                        addObserver(this@FragmentMap)
                                        center = DEFAULT_CENTER
                                    }
                                    setOnTouchListener { v, event ->
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                            }
                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                                v.parent?.requestDisallowInterceptTouchEvent(false)
                                            }
                                        }
                                        false
                                    }
                                }
                                OpenStreetMapMapnik.INSTANCE.userAgent = "MadridTransport"
                                tileCache = AndroidUtil.createTileCache(context, "mapcache", mv.model.displayModel.tileSize, 1f, mv.model.frameBufferModel.overdrawFactor)
                                tileLayer = TileDownloadLayer(tileCache, mv.model.mapViewPosition, OpenStreetMapMapnik.INSTANCE, AndroidGraphicFactory.INSTANCE).also {
                                    mv.layerManager.layers.add(it)
                                    it.onResume()
                                }
                                fetchStops()
                                move()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() = super.onResume().also { startLoc() }
    override fun onPause() = super.onPause().also { fusedLocationClient.removeLocationUpdates(locCallback) }

    private fun startLoc() {
        if (checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION) == PERMISSION_GRANTED) {
            val req = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 10000).setMinUpdateIntervalMillis(5000).build()
            fusedLocationClient.requestLocationUpdates(req, locCallback, null)
        }
    }

    private fun fetchStops() {
        val token = ApiCall.token
        ApiCall.callApi().create(ApiInterface::class.java).getStopsList(token).enqueue(object : Callback<StopsModel> {
            override fun onResponse(c: Call<StopsModel>, r: Response<StopsModel>) {
                r.body()?.stopsData?.let { displayStops(it) }
            }
            override fun onFailure(c: Call<StopsModel>, t: Throwable) { Log.e("Map", "Error", t) }
        })
    }

    private fun displayStops(stops: List<StopsModel.Stops>) {
        val mv = mapView ?: return
        val bmp = getBitmapFromDrawable(requireContext(), R.drawable.ic_location_filled_24dp, R.color.blue)
        stops.forEach { stop ->
            stop.geometry?.coordinates?.let { mv.layerManager.layers.add(StopMarker(LatLong(it[1], it[0]), bmp, stop)) }
        }
        mv.layerManager.redrawLayers()
    }

    private fun getBitmapFromDrawable(context: android.content.Context, drawableId: Int, tintColorRes: Int): org.mapsforge.core.graphics.Bitmap {
        val drawable = getDrawable(context, drawableId)!!.mutate()
        DrawableCompat.setTint(drawable, getColor(context, tintColorRes))

        val density = context.resources.displayMetrics.density
        val width = (36 * density).toInt()
        val height = (36 * density).toInt()
        drawable.setBounds(0, 0, width, height)

        return AndroidGraphicFactory.convertToBitmap(drawable)
    }

    private fun move() {
        if (checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            permLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            loc?.let { centerOn(LatLong(it.latitude, it.longitude)) } ?: run {
                fusedLocationClient.getCurrentLocation(PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { it?.let { centerOn(LatLong(it.latitude, it.longitude)) } }
            }
        }
    }

    private fun centerOn(ll: LatLong) {
        updatePos(ll)
        mapView?.apply { model.mapViewPosition.center = ll; setZoomLevel(16.toByte()) }
    }

    private fun updatePos(ll: LatLong) {
        val mv = mapView ?: return
        if (userMarker == null) {
            val bmp = getBitmapFromDrawable(requireContext(), R.drawable.ic_my_location_24dp, R.color.turquoise)
            userMarker = Marker(ll, bmp, -bmp.width / 2, -bmp.height / 2).also { mv.layerManager.layers.add(it) }
        } else userMarker?.latLong = ll
        mv.layerManager.redrawLayers()
    }

    private inner class StopMarker(l: LatLong, b: org.mapsforge.core.graphics.Bitmap, val s: StopsModel.Stops) : Marker(l, b, -b.width / 2, -b.height) {
        override fun onTap(tl: LatLong, ly: Point, ty: Point) =
            (mapView != null && contains(ly, ty, mapView)).also { if (it) startActivity(Intent(context, StopActivity::class.java).putExtra("stopId", s.stopId).putExtra("stopName", s.stopName)) }
    }

    override fun onChange() = activity?.runOnUiThread {
        val mvp = mapView?.model?.mapViewPosition ?: return@runOnUiThread
        canZoomIn = mvp.zoomLevel < mvp.zoomLevelMax
        canZoomOut = mvp.zoomLevel > mvp.zoomLevelMin
    } ?: Unit

    private fun toast(id: Int) = Toast.makeText(context, id, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        mapView?.apply { model.mapViewPosition.removeObserver(this@FragmentMap); tileLayer?.onPause(); destroyAll() }
        tileCache?.destroy()
        AndroidGraphicFactory.clearResourceMemoryCache()
        userMarker = null
        mapView = null
        super.onDestroyView()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    canZoomIn: Boolean,
    canZoomOut: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onLocate: () -> Unit,
    mapViewFactory: (android.content.Context) -> View
) {
    val themeMode = ThemeManager.themeMode
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorFilter = remember(isDark) {
        if (isDark) {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.85f, 0f, 0f, 0f, 240f,
                        0f, -0.85f, 0f, 0f, 240f,
                        0f, 0f, -0.85f, 0f, 240f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        } else null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_activity_map),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            )
        }
    ) { padding ->
        Box (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = mapViewFactory,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.colorFilter = colorFilter
                    }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = onLocate,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_my_location_24dp),
                        contentDescription = stringResource(R.string.my_location)
                    )
                }

                FloatingActionButton(
                    onClick = if (canZoomIn) onZoomIn else ({}),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.alpha(if (canZoomIn) 1f else 0.38f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = stringResource(R.string.zoom_in)
                    )
                }

                FloatingActionButton(
                    onClick = if (canZoomOut) onZoomOut else ({}),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.alpha(if (canZoomOut) 1f else 0.38f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove_24dp),
                        contentDescription = stringResource(R.string.zoom_out)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    val isInspection = LocalInspectionMode.current
    MadridTransportTheme {
        MapScreen(
            canZoomIn = true,
            canZoomOut = false,
            onZoomIn = {},
            onZoomOut = {},
            onLocate = {},
            mapViewFactory = { context ->
                if (isInspection) {
                    View(context).apply {
                        setBackgroundColor(android.graphics.Color.LTGRAY)
                    }
                } else {
                    MapView(context)
                }
            }
        )
    }
}
