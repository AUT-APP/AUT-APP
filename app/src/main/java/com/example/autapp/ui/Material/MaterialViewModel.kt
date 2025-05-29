package com.example.autapp.ui.Material

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.autapp.AUTApplication
import com.example.autapp.data.models.Material
import com.example.autapp.data.models.TimetableEntry
import com.example.autapp.data.repository.MaterialRepository
import com.example.autapp.data.repository.StudentRepository
import com.example.autapp.data.repository.TimetableEntryRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class MaterialViewModel (
    private val materialRepository: MaterialRepository,
    private val studentRepository: StudentRepository,
    private val timetableRepository: TimetableEntryRepository
) : ViewModel() {

    var studentId: Int = 0
    var todayMaterials: List<Material> = emptyList()
    var allMaterials: List<Material> = emptyList()
    var timetableEntries: List<TimetableEntry> = emptyList()
    var errorMessage: String? = null
    var materialsByCourse by mutableStateOf<List<Material>>(emptyList())


    fun initialize(studentId: Int) {
        this.studentId = studentId
        fetchTodayMaterials()
    }
    private fun fetchTodayMaterials() {
        viewModelScope.launch {
            try {
                // Fetch timetable
                timetableEntries = timetableRepository.getAllTimetableEntries()

                // Determine today's weekday (1=Monday, 7=Sunday)
                val rawToday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val today = if (rawToday == Calendar.SUNDAY) 7 else rawToday - 1

                // Courses scheduled today
                val todayCourseIds = timetableEntries
                    .filter { it.dayOfWeek == today }
                    .map { it.courseId }

                val studentWithCourses = studentRepository.getStudentWithCourses(studentId)
                val enrolledCourseIds = studentWithCourses?.courses?.map { it.courseId } ?: emptyList()

                val validCourseIds = enrolledCourseIds.intersect(todayCourseIds)


                val materials = validCourseIds.flatMap { courseId ->
                    materialRepository.getMaterialsByCourse(courseId)
                }

                todayMaterials = materials.sortedByDescending { it.uploadDate }
                allMaterials = materials
                errorMessage = null

            } catch (e: Exception) {
                errorMessage = "Failed to load materials: ${e.message}"
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AUTApplication
                MaterialViewModel(
                    app.materialRepository,
                    app.studentRepository,
                    app.timetableEntryRepository
                )
            }
        }
    }
}
