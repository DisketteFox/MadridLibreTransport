package dev.diskettefox.madridtransport.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import dev.diskettefox.madridtransport.FavoritesManager
import dev.diskettefox.madridtransport.api.ApiCall
import dev.diskettefox.madridtransport.api.ApiInterface
import dev.diskettefox.madridtransport.models.HelloModel
import dev.diskettefox.madridtransport.models.StopModel
import dev.diskettefox.madridtransport.models.StopsModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class StopUiState(
    val stopsList: List<StopModel.Stop> = emptyList(),
    val allStops: List<StopsModel.Stops> = emptyList(),
    val isLoading: Boolean = false,
    val isNoConnection: Boolean = false,
    val isNoFavorites: Boolean = false
)

class StopViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(StopUiState())
    val uiState: StateFlow<StopUiState> = _uiState.asStateFlow()

    private var currentLoadId = 0
    private var responsesReceived = 0
    private var stopIds: List<Int> = emptyList()
    private val stopIdToIndex = mutableMapOf<Int, Int>()
    private val tempStopsList = mutableListOf<StopModel.Stop>()
    private var lastLoadedStopIds: List<Int> = emptyList()
    private var lastLoadedNames: Map<Int, String?> = emptyMap()

    private val apiInterface = ApiCall.callApi().create(ApiInterface::class.java)

    init {
        fetchAllStops()
    }

    fun fetchAllStops() {
        val accessToken = ApiCall.token
        if (accessToken.isNullOrEmpty()) return

        apiInterface.getStopsList(accessToken).enqueue(object : Callback<StopsModel> {
            override fun onResponse(call: Call<StopsModel>, response: Response<StopsModel>) {
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(allStops = response.body()?.stopsData ?: emptyList())
                }
            }

            override fun onFailure(call: Call<StopsModel>, t: Throwable) {
                Log.e("StopViewModel", "Error fetching stops list", t)
            }
        })
    }

    fun refreshFavorites() {
        val loadId = ++currentLoadId
        stopIds = FavoritesManager.getFavorites(getApplication())
        lastLoadedStopIds = stopIds.toList()
        lastLoadedNames = stopIds.associateWith { FavoritesManager.getFavoriteName(getApplication(), it.toString()) }
        
        tempStopsList.clear()
        stopIdToIndex.clear()
        responsesReceived = 0

        _uiState.value = _uiState.value.copy(
            isNoConnection = false,
            isNoFavorites = false,
            isLoading = stopIds.isNotEmpty()
        )

        if (stopIds.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isNoFavorites = true,
                stopsList = emptyList(),
                isLoading = false
            )
            return
        }

        for (i in stopIds.indices) {
            stopIdToIndex[stopIds[i]] = i
        }

        val accessToken = ApiCall.token
        if (accessToken.isNullOrEmpty()) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        checkPing(loadId)

        for (stopId in stopIds) {
            fetchStopData(stopId, accessToken, loadId)
        }
    }

    private fun checkPing(loadId: Int) {
        apiInterface.getHello().enqueue(object : Callback<HelloModel> {
            override fun onResponse(call: Call<HelloModel>, response: Response<HelloModel>) {}
            override fun onFailure(call: Call<HelloModel>, t: Throwable) {
                if (loadId != currentLoadId) return
                _uiState.value = _uiState.value.copy(isLoading = false, isNoConnection = true)
            }
        })
    }

    private fun fetchStopData(stopId: Int, accessToken: String, loadId: Int) {
        apiInterface.getStop(stopId, accessToken).enqueue(object : Callback<StopModel> {
            override fun onResponse(call: Call<StopModel>, response: Response<StopModel>) {
                if (loadId != currentLoadId) return
                if (response.isSuccessful && response.body() != null) {
                    response.body()?.stopsData?.forEach { data ->
                        data.stops?.let { tempStopsList.addAll(it) }
                    }
                }
                onResponseReceived(loadId)
            }

            override fun onFailure(call: Call<StopModel>, t: Throwable) {
                if (loadId != currentLoadId) return
                onResponseReceived(loadId)
            }
        })
    }

    private fun onResponseReceived(loadId: Int) {
        if (loadId != currentLoadId) return
        responsesReceived++
        if (responsesReceived >= stopIds.size) {
            val sortedList = tempStopsList.toList().sortedBy { stop ->
                try {
                    stopIdToIndex[stop.stopId.toInt()] ?: Int.MAX_VALUE
                } catch (e: NumberFormatException) {
                    Int.MAX_VALUE
                }
            }
            _uiState.value = _uiState.value.copy(
                stopsList = sortedList,
                isLoading = false,
                isNoConnection = sortedList.isEmpty() && stopIds.isNotEmpty()
            )
        }
    }

    fun updateFavoritesOrder(newList: List<StopModel.Stop>) {
        val newStopIds = newList.mapNotNull { 
            runCatching { it.stopId.toInt() }.getOrNull()
        }
        FavoritesManager.saveFavorites(getApplication(), newStopIds)
        lastLoadedStopIds = newStopIds
        _uiState.value = _uiState.value.copy(stopsList = newList)
    }

    fun refreshFavoritesIfNeeded() {
        val currentIds = FavoritesManager.getFavorites(getApplication())
        val currentNames = currentIds.associateWith { FavoritesManager.getFavoriteName(getApplication(), it.toString()) }
        
        if (currentIds != lastLoadedStopIds || currentNames != lastLoadedNames) {
            refreshFavorites()
        }
    }
}
