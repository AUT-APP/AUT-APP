package com.example.autapp.data.models

data class Grade(
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

        companion object {
            fun fromScore(score: Double): GradeValue {
                return entries.firstOrNull { score >= it.minScore && score <= it.maxScore } ?: D
            }
        }
    }

    var score: Double
        get() = _score
        set(value) {
            _score = value
            grade = GradeValue.fromScore(value).description
        }

    fun getNumericValue(): Int {
        return GradeValue.fromScore(_score).numericValue
    }

    override fun toString(): String {
        return "Grade(gradeId=$gradeId, assignmentId=$assignmentId, studentId=$studentId, score=$score, grade='$grade', feedback='$feedback')"
    }

    fun getGradeDescription(): String {
        return GradeValue.fromScore(score).description
    }
}