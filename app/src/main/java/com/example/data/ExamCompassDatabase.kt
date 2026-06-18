package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- entities ---

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val email: String,
    val name: String,
    val profilePhoto: String,
    val course: String,
    val branch: String,
    val semester: String,
    val college: String,
    val xp: Int = 0,
    val level: Int = 1,
    val studyStreak: Int = 0,
    val totalQuizzesCompleted: Int = 0,
    val mockTestsAttempted: Int = 0,
    val accuracy: Float = 0.0f,
    val studyHoursThisWeek: Float = 0.0f
) {
    fun getLevelTitle(): String {
        return when {
            level >= 50 -> "Legend"
            level >= 30 -> "Master"
            level >= 20 -> "Expert"
            level >= 10 -> "Scholar"
            level >= 5 -> "Learner"
            else -> "Beginner"
        }
    }
}

@Entity(tableName = "subjects")
data class SubjectItem(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val semester: String,
    val description: String
)

@Entity(tableName = "notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: String,
    val unit: String,
    val title: String,
    val content: String,
    val type: String, // PDF, Revision, Faculty, Student
    val author: String,
    val isBookmarked: Boolean = false
)

@Entity(tableName = "question_papers")
data class QuestionPaperItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: String,
    val year: String,
    val regulation: String,
    val course: String,
    val isBookmarked: Boolean = false
)

@Entity(tableName = "quizzes")
data class QuizItem(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val difficulty: String // Easy, Medium, Hard, Expert
)

@Entity(tableName = "quiz_questions")
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val quizId: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // A, B, C, D
    val explanation: String
)

@Entity(tableName = "saved_chat_conversations")
data class ChatConversation(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: String,
    val role: String, // user, model
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

// --- DAOs ---

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("UPDATE users SET xp = :xp, level = :level WHERE email = :email")
    suspend fun updateUserXpAndLevel(email: String, xp: Int, level: Int)

    @Query("UPDATE users SET studyStreak = studyStreak + 1 WHERE email = :email")
    suspend fun incrementStudyStreak(email: String)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectItem>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectItem>)
}

@Dao
interface NotesDao {
    @Query("SELECT * FROM notes WHERE subjectId = :subjectId")
    fun getNotesBySubject(subjectId: String): Flow<List<NoteItem>>

    @Query("SELECT * FROM notes WHERE isBookmarked = 1")
    fun getBookmarkedNotes(): Flow<List<NoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteItem>)

    @Query("UPDATE notes SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: Int, isBookmarked: Boolean)
}

@Dao
interface QuestionPaperDao {
    @Query("SELECT * FROM question_papers WHERE subjectId = :subjectId")
    fun getPapersBySubject(subjectId: String): Flow<List<QuestionPaperItem>>

    @Query("SELECT * FROM question_papers WHERE isBookmarked = 1")
    fun getBookmarkedPapers(): Flow<List<QuestionPaperItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaper(paper: QuestionPaperItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPapers(papers: List<QuestionPaperItem>)

    @Query("UPDATE question_papers SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: Int, isBookmarked: Boolean)
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes WHERE subjectId = :subjectId")
    fun getQuizzesBySubject(subjectId: String): Flow<List<QuizItem>>

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId")
    suspend fun getQuestionsForQuiz(quizId: String): List<QuizQuestion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<QuizItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestion>)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM saved_chat_conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<ChatConversation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conv: ChatConversation)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessage)

    @Query("UPDATE chat_messages SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateMessageBookmark(id: Int, isBookmarked: Boolean)

    @Query("SELECT * FROM chat_messages WHERE isBookmarked = 1")
    fun getBookmarkedMessages(): Flow<List<ChatMessage>>
}

// --- App Database ---

@Database(
    entities = [
        UserProfile::class,
        SubjectItem::class,
        NoteItem::class,
        QuestionPaperItem::class,
        QuizItem::class,
        QuizQuestion::class,
        ChatConversation::class,
        ChatMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExamCompassDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun subjectDao(): SubjectDao
    abstract fun notesDao(): NotesDao
    abstract fun paperDao(): QuestionPaperDao
    abstract fun quizDao(): QuizDao
    abstract fun chatDao(): ChatDao
}
