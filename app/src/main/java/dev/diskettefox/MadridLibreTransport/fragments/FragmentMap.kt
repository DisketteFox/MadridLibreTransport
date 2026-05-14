package dev.diskettefox.MadridLibreTransport.fragments

import android.Manifest.permission.*
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.diskettefox.MadridLibreTransport.R
import dev.diskettefox.MadridLibreTransport.StopActivity
import dev.diskettefox.MadridLibreTransport.api.*
import dev.diskettefox.MadridLibreTransport.models.StopsModel
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

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) = 
        i.inflate(R.layout.view_map, c, false).also { mapView = it.findViewById(R.id.map_view) }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)
        val mv = mapView ?: return
        v.findViewById<FloatingActionButton>(R.id.zoom_in).setOnClickListener { mv.model.mapViewPosition.zoomIn() }
        v.findViewById<FloatingActionButton>(R.id.zoom_out).setOnClickListener { mv.model.mapViewPosition.zoomOut() }
        v.findViewById<FloatingActionButton>(R.id.locate_me).setOnClickListener { move() }

        mv.apply {
            mapScaleBar.isVisible = true
            setBuiltInZoomControls(false)
            setZoomLevel(15.toByte())
            setZoomLevelMax(18.toByte())
            model.mapViewPosition.apply {
                addObserver(this@FragmentMap)
                setCenter(LatLong(40.416974, -3.703634))
            }
        }

        OpenStreetMapMapnik.INSTANCE.userAgent = "MadridLibreTransport"
        tileCache = AndroidUtil.createTileCache(requireContext(), "mapcache", mv.model.displayModel.tileSize, 1f, mv.model.frameBufferModel.overdrawFactor)
        tileLayer = TileDownloadLayer(tileCache, mv.model.mapViewPosition, OpenStreetMapMapnik.INSTANCE, AndroidGraphicFactory.INSTANCE).also {
            mv.layerManager.layers.add(it)
            it.onResume()
        }

        fetchStops()
        move()
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
        val token = ApiCall.token ?: return
        ApiCall.callApi().create(ApiInterface::class.java).getStopsList(token).enqueue(object : Callback<StopsModel> {
            override fun onResponse(c: Call<StopsModel>, r: Response<StopsModel>) {
                r.body()?.stopsData?.let { displayStops(it) }
            }
            override fun onFailure(c: Call<StopsModel>, t: Throwable) { Log.e("Map", "Error", t) }
        })
    }

    private fun displayStops(stops: List<StopsModel.Stops>) {
        val mv = mapView ?: return
        val bmp = AndroidGraphicFactory.convertToBitmap(getDrawable(requireContext(), R.drawable.ic_location_24dp)!!)
        stops.forEach { stop ->
            stop.geometry?.coordinates?.let { mv.layerManager.layers.add(StopMarker(LatLong(it[1], it[0]), bmp, stop)) }
        }
        mv.layerManager.redrawLayers()
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
        mapView?.apply { model.mapViewPosition.setCenter(ll); setZoomLevel(16.toByte()) }
    }

    private fun updatePos(ll: LatLong) {
        val mv = mapView ?: return
        if (userMarker == null) {
            val bmp = AndroidGraphicFactory.convertToBitmap(getDrawable(requireContext(), R.drawable.ic_my_location_24dp)!!)
            userMarker = Marker(ll, bmp, 0, 0).also { mv.layerManager.layers.add(it) }
        } else userMarker?.latLong = ll
        mv.layerManager.redrawLayers()
    }

    private inner class StopMarker(l: LatLong, b: org.mapsforge.core.graphics.Bitmap, val s: StopsModel.Stops) : Marker(l, b, 0, -b.height / 2) {
        override fun onTap(tl: LatLong, ly: Point, ty: Point) = 
            (mapView != null && contains(ly, ty, mapView)).also { if (it) startActivity(Intent(context, StopActivity::class.java).putExtra("stopId", s.stopId).putExtra("stopName", s.stopName)) }
    }

    override fun onChange() = activity?.runOnUiThread {
        val mvp = mapView?.model?.mapViewPosition ?: return@runOnUiThread
        view?.findViewById<View>(R.id.zoom_in)?.isEnabled = mvp.zoomLevel < mvp.zoomLevelMax
        view?.findViewById<View>(R.id.zoom_out)?.isEnabled = mvp.zoomLevel > mvp.zoomLevelMin
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
