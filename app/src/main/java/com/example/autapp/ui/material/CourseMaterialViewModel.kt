package com.example.autapp.ui.material

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.firebase.FirebaseCourseMaterialRepository
import com.example.autapp.data.models.CourseMaterial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class CourseMaterialViewModel (
    private val repository: FirebaseCourseMaterialRepository
) : ViewModel() {

    // Holds the list of materials for a course
    private val _materials = MutableStateFlow<List<CourseMaterial>>(emptyList())
    val materials: StateFlow<List<CourseMaterial>> = _materials.asStateFlow()

    // Load all materials for a specific course
    fun loadMaterialsForCourse(courseId: String) {
        viewModelScope.launch {
            val fetchedMaterials = repository.getMaterialsByCourse(courseId)
            Log.d("CourseMaterialViewModel", "Loading materials for courseId = $courseId")
            // Fetch and display the materials
            _materials.value = repository.getMaterialsByCourse(courseId)
            Log.d("CourseMaterialViewModel", "Loaded ${_materials.value.size} materials")
        }
    }

    // Insert a new material
    fun addMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            repository.create(material)
            loadMaterialsForCourse(material.courseId) // Refresh
        }
    }

    fun deleteMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            repository.delete(material.materialId)
            loadMaterialsForCourse(material.courseId) // Refresh after delete
        }
    }

    fun updateMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            try {
                repository.update(material.materialId, material)
                loadMaterialsForCourse(material.courseId)
            } catch (e: Exception) {
                // Handle error
            }
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