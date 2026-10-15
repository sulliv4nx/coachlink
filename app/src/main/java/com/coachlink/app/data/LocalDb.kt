package com.coachlink.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// athleteId/coachId anticipate future multi-user synchronization.
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val athleteId: Long = 1,
    val calories: Int = 2200,
    val protein: Int = 150,
    val carbs: Int = 242,
    val fat: Int = 70,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meals", indices = [Index("athleteId"), Index("day")])
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long = 1,
    val day: String,
    val mealType: String,
    val name: String,
    val barcode: String? = null,
    val grams: Double,
    val calories100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double,
    @ColumnInfo(defaultValue = "'manual'") val source: String = "manual",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "programs", indices = [Index("athleteId")])
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long = 1,
    val coachId: Long = 1,
    val title: String,
    val dayLabel: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercises", foreignKeys = [ForeignKey(
    entity = Program::class, parentColumns = ["id"], childColumns = ["programId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("programId")])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val name: String,
    val sets: Int,
    val reps: Int,
    val targetLoadKg: Double,
    val restSeconds: Int = 90,
    @ColumnInfo(defaultValue = "'CORE'") val muscleGroup: String = "CORE"
)

@Entity(tableName = "workout_logs", indices = [Index("athleteId"), Index("exerciseId"), Index("day")])
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long = 1,
    val exerciseId: Long,
    val day: String,
    val setsDone: Int,
    val repsDone: Int,
    val loadKg: Double,
    val perceivedEffort: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "body_metrics", indices = [Index("athleteId"), Index("day")])
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long = 1,
    val day: String,
    val weightKg: Double,
    val createdAt: Long = System.currentTimeMillis()
)

/** Modes can be chosen independently for training and nutrition. One-device MVP profile. */
@Entity(tableName = "athlete_settings")
data class AthleteSettings(
    @PrimaryKey val athleteId: Long = 1,
    val sportMode: String = "AI",
    val nutritionMode: String = "AI",
    val objective: String = "FITNESS",
    val weakMuscle: String = "NONE",
    val sessionsPerWeek: Int = 3,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "extra_workouts", indices = [Index("athleteId"), Index("day")])
data class ExtraWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long = 1,
    val day: String,
    val name: String,
    val muscleGroup: String,
    val sets: Int,
    val reps: Int,
    val rpe: Int,
    val pain: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)


/** One CrossFit class may train several areas: athletes verify muscle groups before saving. */
@Entity(tableName = "crossfit_sessions", indices = [Index("athleteId"), Index("day")])
data class CrossFitSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long = 1,
    val day: String,
    val title: String,
    val format: String,
    val wod: String,
    val durationMinutes: Int,
    val rpe: Int,
    val muscleGroups: String,
    val pain: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Weekly recurring ingredient plan, independent from what has already been eaten. */
@Entity(tableName = "meal_plan", indices = [Index("athleteId")])
data class MealPlanIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val athleteId: Long = 1,
    val weekday: Int,
    val mealType: String,
    val food: String,
    val grams: Double,
    val servings: Int = 1
)

@Dao
interface CoachDao {
    @Query("SELECT * FROM goals WHERE athleteId = 1 LIMIT 1")
    fun goal(): Flow<Goal?>
    @Upsert suspend fun saveGoal(goal: Goal)

    @Query("SELECT * FROM meals WHERE athleteId = 1 ORDER BY day DESC, createdAt DESC")
    fun meals(): Flow<List<Meal>>
    @Insert suspend fun addMeal(meal: Meal)
    @Insert suspend fun addMeals(meals: List<Meal>)
    @Query("DELETE FROM meals WHERE id = :id AND athleteId = 1") suspend fun deleteMeal(id: Long)

    @Query("SELECT * FROM programs WHERE athleteId = 1 ORDER BY createdAt DESC")
    fun programs(): Flow<List<Program>>
    @Insert suspend fun addProgram(program: Program): Long
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    fun exercises(): Flow<List<Exercise>>
    @Insert suspend fun addExercise(exercise: Exercise)
    @Query("DELETE FROM programs WHERE id = :id AND athleteId = 1") suspend fun deleteProgram(id: Long)

    @Query("SELECT * FROM workout_logs WHERE athleteId = 1 ORDER BY day DESC, createdAt DESC")
    fun logs(): Flow<List<WorkoutLog>>
    @Insert suspend fun addLog(log: WorkoutLog)

    @Query("SELECT * FROM body_metrics WHERE athleteId = 1 ORDER BY day DESC, createdAt DESC")
    fun metrics(): Flow<List<BodyMetric>>
    @Insert suspend fun addMetric(metric: BodyMetric)
    @Query("SELECT * FROM athlete_settings WHERE athleteId = 1 LIMIT 1")
    fun settings(): Flow<AthleteSettings?>
    @Upsert suspend fun saveSettings(settings: AthleteSettings)

    @Query("SELECT * FROM extra_workouts WHERE athleteId = 1 ORDER BY day DESC, createdAt DESC")
    fun extraWorkouts(): Flow<List<ExtraWorkout>>
    @Insert suspend fun addExtraWorkout(workout: ExtraWorkout)
    @Query("DELETE FROM extra_workouts WHERE athleteId = 1 AND id = :id")
    suspend fun deleteExtraWorkout(id: Long)

    @Query("SELECT * FROM crossfit_sessions WHERE athleteId = 1 ORDER BY day DESC, createdAt DESC")
    fun crossfitSessions(): Flow<List<CrossFitSession>>
    @Insert suspend fun addCrossfitSession(session: CrossFitSession)
    @Query("DELETE FROM crossfit_sessions WHERE id = :id AND athleteId = 1")
    suspend fun deleteCrossfitSession(id: Long)

    @Query("SELECT * FROM meal_plan WHERE athleteId = 1 ORDER BY weekday, mealType, id")
    fun mealPlan(): Flow<List<MealPlanIngredient>>
    @Insert suspend fun addMealPlanIngredient(item: MealPlanIngredient)
    @Query("DELETE FROM meal_plan WHERE athleteId = 1 AND id = :id")
    suspend fun deleteMealPlanIngredient(id: Long)
    @Query("DELETE FROM meal_plan WHERE athleteId = 1")
    suspend fun clearMealPlan()
    @Insert suspend fun addPlanIngredients(items: List<MealPlanIngredient>)

    @Transaction
    suspend fun replaceMealPlan(items: List<MealPlanIngredient>) {
        clearMealPlan()
        addPlanIngredients(items)
    }

    @Transaction
    suspend fun createSuggestedProgram(program: Program, entries: List<Exercise>) {
        val id = addProgram(program)
        entries.forEach { addExercise(it.copy(programId = id)) }
    }

}

@Database(entities = [Goal::class, Meal::class, Program::class, Exercise::class, WorkoutLog::class, BodyMetric::class, AthleteSettings::class, ExtraWorkout::class, MealPlanIngredient::class, CrossFitSession::class], version = 4, exportSchema = false)
abstract class CoachDatabase: RoomDatabase() {
    abstract fun dao(): CoachDao
    companion object {
        private val MIGRATION_1_2 = object: Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meals ADD COLUMN source TEXT NOT NULL DEFAULT 'manual'")
            }
        }
        private val MIGRATION_2_3 = object: Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN muscleGroup TEXT NOT NULL DEFAULT 'CORE'")
                db.execSQL("""CREATE TABLE IF NOT EXISTS athlete_settings (
                    athleteId INTEGER NOT NULL PRIMARY KEY, sportMode TEXT NOT NULL,
                    nutritionMode TEXT NOT NULL, objective TEXT NOT NULL, weakMuscle TEXT NOT NULL,
                    sessionsPerWeek INTEGER NOT NULL, updatedAt INTEGER NOT NULL)""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS extra_workouts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, athleteId INTEGER NOT NULL,
                    day TEXT NOT NULL, name TEXT NOT NULL, muscleGroup TEXT NOT NULL,
                    sets INTEGER NOT NULL, reps INTEGER NOT NULL, rpe INTEGER NOT NULL,
                    pain INTEGER NOT NULL, createdAt INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_extra_workouts_athleteId ON extra_workouts (athleteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_extra_workouts_day ON extra_workouts (day)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS meal_plan (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, athleteId INTEGER NOT NULL,
                    weekday INTEGER NOT NULL, mealType TEXT NOT NULL, food TEXT NOT NULL,
                    grams REAL NOT NULL, servings INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_plan_athleteId ON meal_plan (athleteId)")
            }
        }
        private val MIGRATION_3_4 = object: Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS crossfit_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    athleteId INTEGER NOT NULL, day TEXT NOT NULL,
                    title TEXT NOT NULL, format TEXT NOT NULL, wod TEXT NOT NULL,
                    durationMinutes INTEGER NOT NULL, rpe INTEGER NOT NULL,
                    muscleGroups TEXT NOT NULL, pain INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crossfit_sessions_athleteId ON crossfit_sessions (athleteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_crossfit_sessions_day ON crossfit_sessions (day)")
            }
        }
        @Volatile private var INSTANCE: CoachDatabase? = null
        fun get(context: Context): CoachDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, CoachDatabase::class.java, "coachlink-local.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { INSTANCE = it }
        }
    }
}
