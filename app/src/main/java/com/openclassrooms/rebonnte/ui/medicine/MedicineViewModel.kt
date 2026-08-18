package com.openclassrooms.rebonnte.ui.medicine

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
    private val medicinesLoad: Flow<MedicineListUiState> =
        medicines.whileSignedIn(userRepository, emptyList())
            .map { MedicineListUiState(medicines = it, isLoading = false) }
            .onStart { emit(MedicineListUiState()) }
            .catch { emit(MedicineListUiState(isLoading = false, errorMessage = it.toMessageRes())) }

    /**
     * The medicines of one aisle, filtered by the database.
     *
     * The screen used to download the whole stock and filter it in memory. The
     * aisle name is not resolved here: on this screen it is already the title.
     */
    fun observeMedicinesInAisle(aisleId: String): Flow<MedicineListUiState> =
        repository.observeMedicinesInAisle(aisleId)
            .map { medicines -> medicines.map { it.toUi(locationName = null) } }
            .whileSignedIn(userRepository, emptyList())
            .map { MedicineListUiState(medicines = it, isLoading = false) }
            .onStart { emit(MedicineListUiState()) }
            .catch { emit(MedicineListUiState(isLoading = false, errorMessage = it.toMessageRes())) }

    private val _actionError = MutableStateFlow<UiMessage?>(null)
    val actionError: StateFlow<UiMessage?> = _actionError.asStateFlow()

    val uiState: StateFlow<MedicineUiState> =
        combine(medicinesLoad, query, sort) { load, query, sort ->
            MedicineUiState(
                medicines = load.medicines,
                sort = sort,
                query = query,
                isLoading = load.isLoading,
                errorMessage = load.errorMessage
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MedicineUiState()
        )

    // How much history is currently asked for. Held here rather than in the
    // screen so that widening the window does not rebuild the flow: the
    // composable keeps collecting the same one, and the medicine card does not
    // flash back through its loading state.
    private val historyLimit = MutableStateFlow(HISTORY_PAGE_SIZE)

    /**
     * A medicine and the most recent entries of its history.
     *
     * The history is read by pages: opening a card downloads [HISTORY_PAGE_SIZE]
     * entries, not the several hundred a much-handled medicine accumulates.
     * One extra entry is asked for — never displayed — because that is what
     * tells the screen whether an older page exists.
     *
     * The window goes back to its first page on every subscription: opening
     * another card must not inherit the depth reached on the previous one.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDetail(medicineId: String): Flow<MedicineDetailUiState> =
        historyLimit
            .flatMapLatest { limit -> detail(medicineId, limit) }
            .whileSignedIn(userRepository, MedicineDetailUiState(isLoading = false))
            .onStart {
                historyLimit.value = HISTORY_PAGE_SIZE
                emit(MedicineDetailUiState())
            }
            .catch {
                emit(MedicineDetailUiState(isLoading = false, errorMessage = it.toMessageRes()))
            }

    private fun detail(medicineId: String, limit: Int): Flow<MedicineDetailUiState> =
        combine(
            repository.observeMedicine(medicineId)
                .combine(aisleNames) { medicine, names -> medicine?.toUi(names[medicine.aisleId]) },
            repository.observeHistory(medicineId, limit = limit + 1)
                .map { entries -> entries.map { it.toUi() } }
        ) { medicine, histories ->
            MedicineDetailUiState(
                medicine = medicine,
                histories = histories.take(limit),
                hasMoreHistory = histories.size > limit,
                isLoading = false
            )
        }

    /** Widens the history window by one page. */
    fun showMoreHistory() {
        historyLimit.value += HISTORY_PAGE_SIZE
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

/** Enough entries to fill a screen, and to answer « qui a touche a ca ». */
const val HISTORY_PAGE_SIZE = 20
