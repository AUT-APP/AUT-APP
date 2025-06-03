package com.example.autapp.ui.material

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.models.CourseMaterial
import com.example.autapp.data.repository.CourseMaterialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class CourseMaterialViewModel (
    private val repository: CourseMaterialRepository
) : ViewModel() {

    // Holds the list of materials for a course
    private val _materials = MutableStateFlow<List<CourseMaterial>>(emptyList())
    val materials: StateFlow<List<CourseMaterial>> = _materials.asStateFlow()

    // Load all materials for a specific course
    fun loadMaterialsForCourse(courseId: Int) {
        viewModelScope.launch {

            // Fetch and display the materials
            _materials.value = repository.getMaterialsForCourse(courseId)
        }
    }

    // Insert a new material
    fun addMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            repository.insertMaterial(material)
            loadMaterialsForCourse(material.courseId) // Refresh
        }
    }

    fun deleteMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            repository.deleteMaterial(material)
            loadMaterialsForCourse(material.courseId) // Refresh after delete
        }
    }

    // Factory to inject dependencies (repository)
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AUTApplication
                CourseMaterialViewModel(application.courseMaterialRepository)
            }
        }
    }
}