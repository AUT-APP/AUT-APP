package com.example.autapp.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grade_table",
    foreignKeys = [
        ForeignKey(
            entity = Assignment::class,
            parentColumns = ["assignmentId"],
            childColumns = ["assignmentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["assignmentId", "studentId"], unique = true)]
)
data class Grade(
    @PrimaryKey(autoGenerate = true)
    val gradeId: Int = 0,
    var assignmentId: Int,
    var studentId: Int,
    private var _score: Double,
    var grade: String,
    var feedback: String? = null
) {
    enum class GradeValue(val minScore: Double, val maxScore: Double, val description: String, val numericValue: Int) {
        A_PLUS(89.50, 100.00, "Pass with High Distinction", 9),
        A(84.50, 89.49, "Pass with Clear Distinction", 8),
        A_MINUS(79.50, 84.49, "Pass with Distinction", 7),
        B_PLUS(74.50, 79.49, "Pass with High Merit", 6),
        B(69.50, 74.49, "Pass with Clear Merit", 5),
        B_MINUS(64.50, 69.49, "Pass with Merit", 4),
        C_PLUS(59.50, 64.49, "High Pass", 3),
        C(54.50, 59.49, "Clear Pass", 2),
        C_MINUS(49.50, 54.49, "Pass", 1),
        D(0.00, 49.49, "Specified Fail", 0);

        override fun toString(): String {
            return when (this) {
                A_PLUS -> "A+"
                A -> "A"
                A_MINUS -> "A-"
                B_PLUS -> "B+"
                B -> "B"
                B_MINUS -> "B-"
                C_PLUS -> "C+"
                C -> "C"
                C_MINUS -> "C-"
                D -> "D"
            }
        }
    }

    var score: Double
        get() = _score
        set(value) {
            require(value in 0.00..100.00) { "Score must be between 0.00 and 100.00" }
            _score = value
            grade = calculateGrade(value).toString()
        }

    init {
        require(_score in 0.00..100.00) { "Score must be between 0.00 and 100.00" }
        grade = calculateGrade(_score).toString()
    }

    private fun calculateGrade(score: Double): GradeValue {
        return GradeValue.entries.firstOrNull { score in it.minScore..it.maxScore } ?: GradeValue.D
    }

    fun getNumericValue(): Int {
        val gradeValue = GradeValue.entries.firstOrNull { it.toString() == grade } ?: GradeValue.D
        return gradeValue.numericValue
    }

    override fun toString(): String {
        return "Grade(gradeId=$gradeId, assignmentId=$assignmentId, studentId=$studentId, score=$score, grade='$grade', feedback='$feedback')"
    }

    fun getGradeDescription(): String {
        val gradeValue = GradeValue.entries.firstOrNull { it.toString() == grade } ?: GradeValue.D
        return gradeValue.description
    }
}