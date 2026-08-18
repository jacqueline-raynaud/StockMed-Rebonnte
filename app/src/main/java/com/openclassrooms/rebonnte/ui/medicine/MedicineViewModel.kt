package com.openclassrooms.rebonnte.ui.medicine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openclassrooms.rebonnte.R
import com.openclassrooms.rebonnte.data.repository.AisleRepository
import com.openclassrooms.rebonnte.data.repository.MedicineRepository
import com.openclassrooms.rebonnte.data.repository.MedicineSort
import com.openclassrooms.rebonnte.data.repository.UserRepository
import com.openclassrooms.rebonnte.ui.UiMessage
import com.openclassrooms.rebonnte.ui.toMessageRes
import com.openclassrooms.rebonnte.ui.toUiMessage
import com.openclassrooms.rebonnte.ui.whileSignedIn
import com.openclassrooms.rebonnte.ui.model.HistoryUi
import com.openclassrooms.rebonnte.ui.model.MedicineUi
import com.openclassrooms.rebonnte.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

// state for list, le critère et le tri

@Immutable
data class MedicineUiState(
    val medicines: List<MedicineUi> = emptyList(),
    val sort: MedicineSort = MedicineSort.NONE,
    val query: String = "",
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)

// state for medicine, history, and loading

@Immutable
data class MedicineDetailUiState(
    val medicine: MedicineUi? = null,
    val histories: List<HistoryUi> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null
)

// Outcome of medicines read without filter and tri
private data class MedicinesLoad(
    val medicines: List<MedicineUi> = emptyList(),
    val isLoading: Boolean = true,
    @StringRes val error: Int? = null
)


@HiltViewModel
class MedicineViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val userRepository: UserRepository,
    aisleRepository: AisleRepository
) : ViewModel() {

    private val aisleNames = aisleRepository.observeAisles()
        .map { aisles -> aisles.associate { it.id to it.name } }

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(MedicineSort.NONE)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val medicines: Flow<List<MedicineUi>> =
        combine(query, sort) { query, sort -> query to sort }
            .flatMapLatest { (query, sort) -> repository.observeMedicines(query, sort) }
            .combine(aisleNames) { medicines, names ->
                medicines.map { it.toUi(names[it.aisleId]) }
            }

    // onStart` loading state, `catch` error state.
    private val medicinesLoad: Flow<MedicinesLoad> =
        medicines.whileSignedIn(userRepository, emptyList())
            .map { MedicinesLoad(medicines = it, isLoading = false) }
            .onStart { emit(MedicinesLoad()) }
            .catch { emit(MedicinesLoad(isLoading = false, error = it.toMessageRes())) }

    private val _actionError = MutableStateFlow<UiMessage?>(null)
    val actionError: StateFlow<UiMessage?> = _actionError.asStateFlow()

    val uiState: StateFlow<MedicineUiState> =
        combine(medicinesLoad, query, sort) { load, query, sort ->
            MedicineUiState(
                medicines = load.medicines,
                sort = sort,
                query = query,
                isLoading = load.isLoading,
                errorMessage = load.error
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MedicineUiState()
        )

    fun observeDetail(medicineId: String): Flow<MedicineDetailUiState> =
        combine(
            repository.observeMedicine(medicineId)
                .combine(aisleNames) { medicine, names -> medicine?.toUi(names[medicine.aisleId]) },
            repository.observeHistory(medicineId)
                .map { entries -> entries.map { it.toUi() } }
        ) { medicine, histories ->
            MedicineDetailUiState(medicine = medicine, histories = histories, isLoading = false)
        }
            .whileSignedIn(userRepository, MedicineDetailUiState(isLoading = false))
            .onStart { emit(MedicineDetailUiState()) }
            .catch {
                emit(MedicineDetailUiState(isLoading = false, errorMessage = it.toMessageRes()))
            }

    // confirm movement before show error
    private val _movementConfirmed = MutableStateFlow<UiMessage?>(null)
    val movementConfirmed: StateFlow<UiMessage?> = _movementConfirmed.asStateFlow()
        fun actionErrorShown() {
        _actionError.value = null
    }

    fun movementConfirmationShown() {
        _movementConfirmed.value = null
    }

    fun updateStock(medicineId: String, delta: Int) {
        viewModelScope.launch {
            runCatching { repository.updateStock(medicineId, delta, currentUserEmail()) }
                .onSuccess {
                    _movementConfirmed.value = UiMessage(
                        res = if (delta < 0) {
                            R.string.detail_units_removed
                        } else {
                            R.string.detail_units_added
                        },
                        args = listOf(abs(delta))
                    )
                }
                .onFailure { _actionError.value = it.toUiMessage() }
        }
    }

    fun deleteMedicine(medicineId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteMedicine(medicineId, currentUserEmail()) }
                .onFailure { _actionError.value = it.toUiMessage() }
        }
    }

    fun filterByName(name: String) {
        query.value = name
    }

    fun sortBy(criterion: MedicineSort) {
        sort.value = criterion
    }

    private fun currentUserEmail(): String =
        userRepository.currentUserOrNull()?.email.orEmpty()
}
