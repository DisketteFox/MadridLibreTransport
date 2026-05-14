package dev.diskettefox.MadridLibreTransport.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dev.diskettefox.MadridLibreTransport.R
import dev.diskettefox.MadridLibreTransport.StopActivity
import dev.diskettefox.MadridLibreTransport.api.ApiCall
import dev.diskettefox.MadridLibreTransport.api.ApiInterface
import dev.diskettefox.MadridLibreTransport.models.StopsModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.download.TileDownloadLayer
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.model.common.Observer
import com.google.android.material.floatingactionbutton.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FragmentMap : Fragment(), Observer {
    private var mapView: MapView? = null
    private var tileCache: TileCache? = null
    private var tileLayer: TileDownloadLayer? = null

    private var zoomIn: FloatingActionButton? = null
    private var zoomOut: FloatingActionButton? = null
    private var locateMe: FloatingActionButton? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            moveToCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidGraphicFactory.createInstance(requireActivity().application)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.view_map, container, false)
        mapView = view.findViewById(R.id.map_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapView = this.mapView ?: return

        zoomIn = view.findViewById(R.id.zoom_in)
        zoomOut = view.findViewById(R.id.zoom_out)
        locateMe = view.findViewById(R.id.locate_me)

        zoomIn?.setOnClickListener { mapView.model.mapViewPosition.zoomIn() }
        zoomOut?.setOnClickListener { mapView.model.mapViewPosition.zoomOut() }
        locateMe?.setOnClickListener { checkLocationPermissionAndMove() }

        mapView.mapScaleBar.isVisible = true
        mapView.setBuiltInZoomControls(false)
        mapView.setZoomLevel(15.toByte())
        mapView.setZoomLevelMax(18.toByte())

        OpenStreetMapMapnik.INSTANCE.setUserAgent("MadridLibreTransport")

        tileCache = AndroidUtil.createTileCache(
            requireContext(), "mapcache",
            mapView.model.displayModel.tileSize, 1f,
            mapView.model.frameBufferModel.overdrawFactor
        )

        tileLayer = TileDownloadLayer(
            tileCache, mapView.model.mapViewPosition,
            OpenStreetMapMapnik.INSTANCE, AndroidGraphicFactory.INSTANCE
        )

        mapView.layerManager.layers.add(tileLayer)
        tileLayer?.onResume()

        mapView.model.mapViewPosition.addObserver(this)
        updateZoomButtons()

        val center = LatLong(40.416974, -3.703634)
        mapView.setCenter(center)

        fetchStops()
    }

    private fun fetchStops() {
        val accessToken = ApiCall.token
        if (accessToken.isNullOrEmpty()) {
            Log.e("FragmentMap", "No access token available")
            return
        }

        val apiInterface = ApiCall.callApi().create(ApiInterface::class.java)
        apiInterface.getStopsList(accessToken).enqueue(object : Callback<StopsModel> {
            override fun onResponse(call: Call<StopsModel>, response: Response<StopsModel>) {
                if (response.isSuccessful && response.body() != null) {
                    val stops = response.body()?.stopsData
                    if (stops != null) {
                        displayStops(stops)
                    }
                } else {
                    Log.e("FragmentMap", "Failed to fetch stops: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<StopsModel>, t: Throwable) {
                Log.e("FragmentMap", "Error fetching stops", t)
            }
        })
    }

    private fun displayStops(stops: List<StopsModel.Stops>) {
        val context = context ?: return
        val mapView = this.mapView ?: return

        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_location_24dp) ?: return
        val bitmap = AndroidGraphicFactory.convertToBitmap(drawable)

        // Markers
        for (stop in stops) {
            val coords = stop.geometry?.coordinates
            if (coords != null && coords.size >= 2) {
                val latLong = LatLong(coords[1], coords[0])
                val marker = StopMarker(latLong, bitmap, stop)
                mapView.layerManager.layers.add(marker)
            }
        }
        mapView.layerManager.redrawLayers()
    }

    private fun checkLocationPermissionAndMove() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                moveToCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun moveToCurrentLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLong = LatLong(location.latitude, location.longitude)
                    mapView?.setCenter(latLong)
                    mapView?.setZoomLevel(16.toByte())
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_location), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Log.e("FragmentMap", "Security exception: ${e.message}")
        }
    }

    private inner class StopMarker(
        latLong: LatLong,
        bitmap: org.mapsforge.core.graphics.Bitmap,
        val stop: StopsModel.Stops
    ) : Marker(latLong, bitmap, 0, -bitmap.height / 2) {
        override fun onTap(tapLatLong: LatLong, layerXY: Point, tapXY: Point): Boolean {
            if (mapView != null && contains(layerXY, tapXY, mapView)) {
                val intent = Intent(context, StopActivity::class.java)
                intent.putExtra("stopId", stop.stopId)
                intent.putExtra("stopName", stop.stopName)
                startActivity(intent)
                return true
            }
            return false
        }
    }

    override fun onChange() {
        activity?.runOnUiThread {
            updateZoomButtons()
        }
    }

    private fun updateZoomButtons() {
        val mapView = this.mapView ?: return
        val mvp = mapView.model.mapViewPosition
        zoomIn?.isEnabled = mvp.zoomLevel < mvp.zoomLevelMax
        zoomOut?.isEnabled = mvp.zoomLevel > mvp.zoomLevelMin
    }

    override fun onDestroyView() {
        mapView?.model?.mapViewPosition?.removeObserver(this)
        tileLayer?.onPause()
        mapView?.destroyAll()
        tileCache?.destroy()
        AndroidGraphicFactory.clearResourceMemoryCache()
        mapView = null
        super.onDestroyView()
    }
}
