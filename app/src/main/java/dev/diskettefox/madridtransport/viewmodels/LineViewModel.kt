package dev.diskettefox.madridtransport.viewmodels

import androidx.lifecycle.ViewModel
import dev.diskettefox.madridtransport.api.ApiCall
import dev.diskettefox.madridtransport.api.ApiInterface
import dev.diskettefox.madridtransport.models.LineModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class LineUiState(
    val data: List<LineModel.Data> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class LineViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LineUiState())
    val uiState: StateFlow<LineUiState> = _uiState.asStateFlow()

    fun fetchLineDetails(lineId: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val apiInterface = ApiCall.callApi().create(ApiInterface::class.java)
        apiInterface.getLineDetail(lineId, ApiCall.token).enqueue(object : Callback<LineModel> {
            override fun onResponse(call: Call<LineModel>, response: Response<LineModel>) {
                if (response.isSuccessful && response.body() != null) {
                    val line = response.body()
                    _uiState.value = _uiState.value.copy(
                        data = line?.data ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Error fetching line details")
                }
            }

            override fun onFailure(call: Call<LineModel>, t: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = t.message)
            }
        })
    }
}
