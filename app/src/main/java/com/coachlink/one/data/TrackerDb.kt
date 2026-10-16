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


/** Planning is separate from the actual records so a template may be reused. */
@Entity(tableName="workout_plans", indices=[Index("date")])
data class WorkoutPlan(
    @PrimaryKey(autoGenerate=true) val id: Long=0,
    val title: String,
    val date: String,
    val note: String=""
)

@Entity(tableName="workout_blocks", indices=[Index("planId")])
data class WorkoutBlock(
    @PrimaryKey(autoGenerate=true) val id: Long=0,
    val planId: Long,
    val position: Int,
    val title: String,
    val format: String,
    val durationSec: Int=0,
    val rounds: Int=8
)

@Entity(tableName="workout_movements", indices=[Index("blockId")])
data class WorkoutMovement(
    @PrimaryKey(autoGenerate=true) val id: Long=0,
    val blockId: Long,
    val position: Int,
    val name: String,
    val sets: Int,
    val reps: Int,
    val loadKg: Double,
    val restSec: Int
)

/** One active session at most. Timestamps are persisted so UI recreation does not reset timers. */
@Entity(tableName="workout_runs",indices=[Index("planId")])
data class WorkoutRun(
    @PrimaryKey(autoGenerate=true) val id: Long=0,
    val planId: Long,
    val startedAtMs: Long,
    val status: String="RUNNING",
    val endedAtMs: Long?=null,
    val pausedAtMs: Long?=null,
    val pausedTotalMs: Long=0L,
    val blockIndex: Int=0,
    val blockStartedAtMs: Long=0L,
    val blockPausedBaselineMs: Long=0L,
    val restStartedAtMs: Long=0L,
    val restRemainingSec: Int=0
)

@Entity(tableName="workout_set_results",indices=[Index("runId"),Index("movementId")])
data class WorkoutSetResult(
    @PrimaryKey(autoGenerate=true) val id: Long=0,
    val runId: Long,
    val movementId: Long,
    val movementName: String,
    val setNumber: Int,
    val reps: Int,
    val loadKg: Double,
    val status: String="DONE",
    val recordedAtMs: Long=System.currentTimeMillis()
)

@Entity(tableName="workout_block_results",indices=[Index("runId")])
data class WorkoutBlockResult(
    @PrimaryKey(autoGenerate=true) val id: Long=0,
    val runId: Long,
    val blockId: Long,
    val blockName: String,
    val score: String,
    val elapsedSec: Long
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

    @Query("SELECT * FROM workout_plans ORDER BY date ASC,id ASC") fun workoutPlans(): Flow<List<WorkoutPlan>>
    @Insert suspend fun insertWorkoutPlan(x: WorkoutPlan): Long
    @Query("DELETE FROM workout_plans WHERE id=:id") suspend fun deleteWorkoutPlan(id: Long)
    @Query("SELECT * FROM workout_blocks ORDER BY planId,position,id") fun workoutBlocks(): Flow<List<WorkoutBlock>>
    @Query("SELECT * FROM workout_blocks WHERE planId=:planId ORDER BY position,id") suspend fun blocksForPlan(planId:Long):List<WorkoutBlock>
    @Insert suspend fun insertWorkoutBlock(x: WorkoutBlock): Long
    @Query("DELETE FROM workout_blocks WHERE id=:id") suspend fun deleteWorkoutBlock(id:Long)
    @Query("SELECT * FROM workout_movements ORDER BY blockId,position,id") fun workoutMovements(): Flow<List<WorkoutMovement>>
    @Query("SELECT * FROM workout_movements WHERE blockId=:blockId ORDER BY position,id") suspend fun movesForBlock(blockId:Long):List<WorkoutMovement>
    @Insert suspend fun insertWorkoutMovement(x: WorkoutMovement):Long
    @Query("DELETE FROM workout_movements WHERE id=:id") suspend fun deleteWorkoutMovement(id:Long)
    @Query("SELECT * FROM workout_runs ORDER BY id DESC") fun workoutRuns(): Flow<List<WorkoutRun>>
    @Query("SELECT * FROM workout_runs WHERE status IN ('RUNNING','PAUSED') ORDER BY id DESC LIMIT 1") suspend fun activeWorkout():WorkoutRun?
    @Query("SELECT * FROM workout_runs WHERE id=:id LIMIT 1") suspend fun workoutRun(id:Long):WorkoutRun?
    @Insert suspend fun insertWorkoutRun(x:WorkoutRun):Long
    @Update suspend fun updateWorkoutRun(x:WorkoutRun)
    @Query("SELECT * FROM workout_set_results ORDER BY id ASC") fun workoutSetResults(): Flow<List<WorkoutSetResult>>
    @Insert suspend fun insertWorkoutSetResult(x:WorkoutSetResult)
    @Query("SELECT * FROM workout_block_results ORDER BY id ASC") fun workoutBlockResults(): Flow<List<WorkoutBlockResult>>
    @Insert suspend fun insertWorkoutBlockResult(x:WorkoutBlockResult)
    @Query("SELECT * FROM workout_set_results WHERE runId=:runId AND movementId=:movementId ORDER BY setNumber ASC")
    suspend fun recordedSets(runId:Long,movementId:Long):List<WorkoutSetResult>
    @Query("DELETE FROM workout_movements WHERE blockId=:blockId") suspend fun clearBlockMovements(blockId:Long)
    @Query("DELETE FROM workout_blocks WHERE planId=:planId") suspend fun clearPlanBlocks(planId:Long)
    @Transaction suspend fun removeBlockWithMovements(blockId:Long) {
        clearBlockMovements(blockId);deleteWorkoutBlock(blockId)
    }
    @Transaction suspend fun removePlanWithBlocks(planId:Long) {
        blocksForPlan(planId).forEach {clearBlockMovements(it.id)}
        clearPlanBlocks(planId);deleteWorkoutPlan(planId)
    }
    @Transaction suspend fun cloneWorkoutPlan(source: WorkoutPlan, date: String):Long {
        val newId=insertWorkoutPlan(source.copy(id=0,date=date))
        blocksForPlan(source.id).forEach { block ->
            val newBlockId=insertWorkoutBlock(block.copy(id=0,planId=newId))
            movesForBlock(block.id).forEach { insertWorkoutMovement(it.copy(id=0,blockId=newBlockId)) }
        }
        return newId
    }
    @Transaction suspend fun startWorkout(planId:Long,time:Long):Boolean {
        if(activeWorkout()!=null || blocksForPlan(planId).isEmpty()) return false
        insertWorkoutRun(WorkoutRun(planId=planId,startedAtMs=time,blockStartedAtMs=time))
        return true
    }
    @Transaction suspend fun completeWorkout(run:WorkoutRun,session:SportSession) {
        updateWorkoutRun(run)
        addSportSession(session)
    }

    @Query("SELECT * FROM friend_goals ORDER BY id DESC") fun friends(): Flow<List<FriendGoal>>
    @Insert suspend fun addFriendGoal(x: FriendGoal)
    @Query("DELETE FROM friend_goals WHERE id=:id") suspend fun deleteFriendGoal(id: Long)
}

@Database(entities=[Profile::class,Measurement::class,Food::class,Program::class,Exercise::class,TrainingLog::class,FriendGoal::class,SportGoal::class,SportSession::class,WorkoutPlan::class,WorkoutBlock::class,WorkoutMovement::class,WorkoutRun::class,WorkoutSetResult::class,WorkoutBlockResult::class],version=3,exportSchema=false)
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

        private val MIGRATION_2_3 = object: androidx.room.migration.Migration(2,3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS workout_plans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL,date TEXT NOT NULL,note TEXT NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_plans_date ON workout_plans (date)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS workout_blocks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,planId INTEGER NOT NULL,position INTEGER NOT NULL,title TEXT NOT NULL,format TEXT NOT NULL,durationSec INTEGER NOT NULL,rounds INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_blocks_planId ON workout_blocks (planId)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS workout_movements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,blockId INTEGER NOT NULL,position INTEGER NOT NULL,name TEXT NOT NULL,sets INTEGER NOT NULL,reps INTEGER NOT NULL,loadKg REAL NOT NULL,restSec INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_movements_blockId ON workout_movements (blockId)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS workout_runs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,planId INTEGER NOT NULL,startedAtMs INTEGER NOT NULL,status TEXT NOT NULL,endedAtMs INTEGER,pausedAtMs INTEGER,pausedTotalMs INTEGER NOT NULL,blockIndex INTEGER NOT NULL,blockStartedAtMs INTEGER NOT NULL,blockPausedBaselineMs INTEGER NOT NULL,restStartedAtMs INTEGER NOT NULL,restRemainingSec INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_runs_planId ON workout_runs (planId)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS workout_set_results (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,runId INTEGER NOT NULL,movementId INTEGER NOT NULL,movementName TEXT NOT NULL,setNumber INTEGER NOT NULL,reps INTEGER NOT NULL,loadKg REAL NOT NULL,status TEXT NOT NULL,recordedAtMs INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_set_results_runId ON workout_set_results (runId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_set_results_movementId ON workout_set_results (movementId)")
                db.execSQL("""CREATE TABLE IF NOT EXISTS workout_block_results (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,runId INTEGER NOT NULL,blockId INTEGER NOT NULL,blockName TEXT NOT NULL,score TEXT NOT NULL,elapsedSec INTEGER NOT NULL)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_block_results_runId ON workout_block_results (runId)")
            }
        }
        @Volatile private var db: TrackerDb? = null
        fun get(context: Context): TrackerDb = db ?: synchronized(this) {
            db ?: Room.databaseBuilder(context.applicationContext,TrackerDb::class.java,"coachlink-one.db").addMigrations(MIGRATION_1_2,MIGRATION_2_3).build().also { db=it }
        }
    }
}
