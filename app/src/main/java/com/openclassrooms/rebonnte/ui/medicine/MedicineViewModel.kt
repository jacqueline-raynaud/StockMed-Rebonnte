package com.openclassrooms.rebonnte.ui.medicine

import androidx.lifecycle.ViewModel
import com.openclassrooms.rebonnte.data.model.Aisle
import com.openclassrooms.rebonnte.data.model.Medicine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.Random

class MedicineViewModel : ViewModel() {
    var _medicines = MutableStateFlow<MutableList<Medicine>>(mutableListOf())
    val medicines: StateFlow<List<Medicine>> get() = _medicines

    init {
        _medicines.value = ArrayList() // Initialiser avec une liste vide
    }

    fun addRandomMedicine(aisles: List<Aisle>) {
        val currentMedicines = ArrayList(medicines.value)
        currentMedicines.add(
            Medicine(
                "Medicine " + (currentMedicines.size + 1),
                Random().nextInt(100),
                aisles[Random().nextInt(aisles.size)].name,
                emptyList()
            )
        )
        _medicines.value = currentMedicines
    }

    fun filterByName(name: String) {
        val currentMedicines: List<Medicine> = medicines.value
        val filteredMedicines: MutableList<Medicine> = ArrayList()
        for (medicine in currentMedicines) {
            if (medicine.name.lowercase(Locale.getDefault())
                    .contains(name.lowercase(Locale.getDefault()))
            ) {
                filteredMedicines.add(medicine)
            }
        }
        _medicines.value = filteredMedicines
    }

    fun sortByNone() {
        _medicines.value = medicines.value.toMutableList() // Pas de tri
    }

    fun sortByName() {
        val currentMedicines = ArrayList(medicines.value)
        currentMedicines.sortWith(Comparator.comparing(Medicine::name))
        _medicines.value = currentMedicines
    }

    fun sortByStock() {
        val currentMedicines = ArrayList(medicines.value)
        currentMedicines.sortWith(Comparator.comparingInt(Medicine::stock))
        _medicines.value = currentMedicines
    }
}

