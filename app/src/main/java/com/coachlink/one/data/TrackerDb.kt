package com.coachlink.one.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Athlète",
    val age: Int = 30,
    val sex: String = "M",
    val heightCm: Double = 175.0,
    val activityFactor: Double = 1.55,
    val startWeightKg: Double = 80.0,
    val goalWeightKg: Double = 76.0,
    val goalDate: String = "2027-04-01",
    val objective: String = "LOSS",
    val calorieOverride: Int = 0
)

@Entity(tableName="measurements", indices=[Index("date")])
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weightKg: Double,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val hipsCm: Double? = null,
    val armCm: Double? = null,
    val thighCm: Double? = null
)

@Entity(tableName="foods", indices=[Index("date")])
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val kind: String,
    val name: String,
    val grams: Double,
    val kcal100: Double,
    val protein100: Double,
    val carbs100: Double,
    val fat100: Double
)

@Entity(tableName="programs")
data class Program(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val dayLabel: String)

@Entity(tableName="exercises",foreignKeys=[ForeignKey(entity=Program::class,parentColumns=["id"],childColumns=["programId"],onDelete=ForeignKey.CASCADE)],indices=[Index("programId")])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long,
    val name: String,
    val sets: Int,
    val reps: Int,
    val loadKg: Double,
    val restSec: Int
)

@Entity(tableName="training_logs",indices=[Index("date"),Index("exerciseId")])
data class TrainingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val exerciseId: Long,
    val name: String,
    val setsDone: Int,
    val repsDone: Int,
    val loadKg: Double
)


/** Measurable, independent sport goals. Multiple goals can share the same period. */
@Entity(tableName="sport_goals")
data class SportGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val metric: String,
    val period: String,
    val target: Int,
    val createdAt: String
)

/** One completed workout per session; calories never subtract from the food diary. */
@Entity(tableName="sport_sessions", indices=[Index("date")])
data class SportSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val activity: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val calorieMethod: String = "MANUAL",
    val notes: String = ""
)

@Entity(tableName="friend_goals")
data class FriendGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val objective: String,
    val targetKg: Double,
    val goalDate: String
)

@Dao interface TrackerDao {
    @Query("SELECT * FROM profile WHERE id=1 LIMIT 1") fun profile(): Flow<Profile?>
    @Upsert suspend fun saveProfile(x: Profile)
    @Query("SELECT * FROM measurements ORDER BY date DESC, id DESC") fun measurements(): Flow<List<Measurement>>
    @Insert suspend fun addMeasurement(x: Measurement)
    @Query("DELETE FROM measurements WHERE id=:id") suspend fun deleteMeasurement(id: Long)
    @Query("SELECT * FROM foods ORDER BY date DESC,id DESC") fun foods(): Flow<List<Food>>
    @Insert suspend fun addFood(x: Food)
    @Query("DELETE FROM foods WHERE id=:id") suspend fun deleteFood(id: Long)
    @Query("SELECT * FROM programs ORDER BY id DESC") fun programs(): Flow<List<Program>>
    @Insert suspend fun addProgram(x: Program)
    @Query("DELETE FROM programs WHERE id=:id") suspend fun deleteProgram(id: Long)
    @Query("SELECT * FROM exercises ORDER BY id ASC") fun exercises(): Flow<List<Exercise>>
    @Insert suspend fun addExercise(x: Exercise)
    @Query("DELETE FROM exercises WHERE id=:id") suspend fun deleteExercise(id: Long)
    @Query("SELECT * FROM training_logs ORDER BY date DESC,id DESC") fun logs(): Flow<List<TrainingLog>>
    @Insert suspend fun addLog(x: TrainingLog)
    @Query("SELECT * FROM sport_goals ORDER BY id DESC") fun sportGoals(): Flow<List<SportGoal>>
    @Insert suspend fun addSportGoal(x: SportGoal)
    @Query("DELETE FROM sport_goals WHERE id=:id") suspend fun deleteSportGoal(id: Long)
    @Query("SELECT * FROM sport_sessions ORDER BY date DESC,id DESC") fun sportSessions(): Flow<List<SportSession>>
    @Insert suspend fun addSportSession(x: SportSession)
    @Query("DELETE FROM sport_sessions WHERE id=:id") suspend fun deleteSportSession(id: Long)
    @Query("SELECT * FROM friend_goals ORDER BY id DESC") fun friends(): Flow<List<FriendGoal>>
    @Insert suspend fun addFriendGoal(x: FriendGoal)
    @Query("DELETE FROM friend_goals WHERE id=:id") suspend fun deleteFriendGoal(id: Long)
}

@Database(entities=[Profile::class,Measurement::class,Food::class,Program::class,Exercise::class,TrainingLog::class,FriendGoal::class,SportGoal::class,SportSession::class],version=2,exportSchema=false)
abstract class TrackerDb: RoomDatabase() {
    abstract fun dao(): TrackerDao
    companion object {
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS sport_goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL, metric TEXT NOT NULL, period TEXT NOT NULL,
                    target INTEGER NOT NULL, createdAt TEXT NOT NULL)""")
                db.execSQL("""CREATE TABLE IF NOT EXISTS sport_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date TEXT NOT NULL, activity TEXT NOT NULL,
                    durationMinutes INTEGER NOT NULL, caloriesBurned INTEGER NOT NULL,
                    calorieMethod TEXT NOT NULL, notes TEXT NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sport_sessions_date ON sport_sessions (date)")
            }
        }
        @Volatile private var db: TrackerDb? = null
        fun get(context: Context): TrackerDb = db ?: synchronized(this) {
            db ?: Room.databaseBuilder(context.applicationContext,TrackerDb::class.java,"coachlink-one.db").addMigrations(MIGRATION_1_2).build().also { db=it }
        }
    }
}
