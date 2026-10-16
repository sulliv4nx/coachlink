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
    @Query("SELECT * FROM friend_goals ORDER BY id DESC") fun friends(): Flow<List<FriendGoal>>
    @Insert suspend fun addFriendGoal(x: FriendGoal)
    @Query("DELETE FROM friend_goals WHERE id=:id") suspend fun deleteFriendGoal(id: Long)
}

@Database(entities=[Profile::class,Measurement::class,Food::class,Program::class,Exercise::class,TrainingLog::class,FriendGoal::class],version=1,exportSchema=false)
abstract class TrackerDb: RoomDatabase() {
    abstract fun dao(): TrackerDao
    companion object {
        @Volatile private var db: TrackerDb? = null
        fun get(context: Context): TrackerDb = db ?: synchronized(this) {
            db ?: Room.databaseBuilder(context.applicationContext,TrackerDb::class.java,"coachlink-one.db").build().also { db=it }
        }
    }
}
