package com.example.autapp.ui.material

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.firebase.FirebaseCourseMaterialRepository
import com.example.autapp.data.firebase.FirebaseNotificationRepository
import com.example.autapp.data.models.CourseMaterial
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

class CourseMaterialViewModel(
    private val repository: FirebaseCourseMaterialRepository,
    private val notificationRepository: FirebaseNotificationRepository,
    private val context: Context
) : ViewModel() {

    private val _materials = MutableStateFlow<List<CourseMaterial>>(emptyList())
    val materials: StateFlow<List<CourseMaterial>> = _materials.asStateFlow()

    fun loadMaterialsForCourse(courseId: String) {
        viewModelScope.launch {
            try {
                _materials.value = repository.getMaterialsByCourse(courseId)
                Log.d("CourseMaterialViewModel", "Loaded ${_materials.value.size} materials for courseId: $courseId")
            } catch (e: Exception) {
                Log.e("CourseMaterialViewModel", "Failed to load materials: ${e.message}", e)
            }
        }
    }

    fun addMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            val success = repository.addMaterialAndNotify(material, context)
            if (success) loadMaterialsForCourse(material.courseId)
        }
    }

    fun updateMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            val success = repository.updateMaterialAndNotify(material, context)
            if (success) loadMaterialsForCourse(material.courseId)
        }
    }

    fun deleteMaterial(material: CourseMaterial) {
        viewModelScope.launch {
            val success = repository.deleteMaterialAndNotify(material, context)
            if (success) loadMaterialsForCourse(material.courseId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AUTApplication
                CourseMaterialViewModel(
                    repository = app.courseMaterialRepository,
                    notificationRepository = app.notificationRepository,
                    context = app.applicationContext
                )
            }
        }
    }
}