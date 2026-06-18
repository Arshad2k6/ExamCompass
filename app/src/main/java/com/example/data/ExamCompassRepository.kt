package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class ExamCompassRepository(context: Context) {

    val db: ExamCompassDatabase = Room.databaseBuilder(
        context.applicationContext,
        ExamCompassDatabase::class.java,
        "exam_compass_db"
    )
        .fallbackToDestructiveMigration()
        .build()

    private val userDao = db.userDao()
    private val subjectDao = db.subjectDao()
    private val notesDao = db.notesDao()
    private val paperDao = db.paperDao()
    private val quizDao = db.quizDao()
    private val chatDao = db.chatDao()

    // --- User profile ---
    fun getUserProfileFlow(): Flow<UserProfile?> = userDao.getUserProfileFlow()
    suspend fun getUserProfile(): UserProfile? = userDao.getUserProfile()
    suspend fun saveUserProfile(profile: UserProfile) = userDao.saveUserProfile(profile)
    suspend fun incrementXp(email: String, earnedXp: Int) {
        val currentProfile = userDao.getUserProfile() ?: return
        val newXp = currentProfile.xp + earnedXp
        // Simple level threshold, e.g. 100 XP per level
        val newLevel = (newXp / 100) + 1
        userDao.updateUserXpAndLevel(email, newXp, newLevel)
    }

    // --- Subjects ---
    fun getAllSubjects(): Flow<List<SubjectItem>> = subjectDao.getAllSubjects()
    suspend fun insertSubject(subject: SubjectItem) = subjectDao.insertSubject(subject)

    // --- Notes ---
    fun getNotesBySubject(subjectId: String): Flow<List<NoteItem>> = notesDao.getNotesBySubject(subjectId)
    fun getBookmarkedNotes(): Flow<List<NoteItem>> = notesDao.getBookmarkedNotes()
    suspend fun updateNoteBookmark(id: Int, isBookmarked: Boolean) = notesDao.updateBookmarkStatus(id, isBookmarked)
    suspend fun insertNote(note: NoteItem) = notesDao.insertNote(note)

    // --- Question Papers ---
    fun getPapersBySubject(subjectId: String): Flow<List<QuestionPaperItem>> = paperDao.getPapersBySubject(subjectId)
    fun getBookmarkedPapers(): Flow<List<QuestionPaperItem>> = paperDao.getBookmarkedPapers()
    suspend fun updatePaperBookmark(id: Int, isBookmarked: Boolean) = paperDao.updateBookmarkStatus(id, isBookmarked)
    suspend fun insertPaper(paper: QuestionPaperItem) = paperDao.insertPaper(paper)

    // --- Quizzes ---
    fun getQuizzesBySubject(subjectId: String): Flow<List<QuizItem>> = quizDao.getQuizzesBySubject(subjectId)
    suspend fun getQuestionsForQuiz(quizId: String): List<QuizQuestion> = quizDao.getQuestionsForQuiz(quizId)
    suspend fun insertQuiz(quiz: QuizItem, questions: List<QuizQuestion>) {
        quizDao.insertQuiz(quiz)
        quizDao.insertQuestions(questions)
    }

    // --- AI Chat ---
    fun getSavedConversations(): Flow<List<ChatConversation>> = chatDao.getAllConversations()
    fun getMessagesForConversation(convId: String): Flow<List<ChatMessage>> = chatDao.getMessagesForConversation(convId)
    suspend fun insertConversation(conv: ChatConversation) = chatDao.insertConversation(conv)
    suspend fun insertMessage(msg: ChatMessage) = chatDao.insertMessage(msg)
    suspend fun updateMessageBookmark(id: Int, isBookmarked: Boolean) = chatDao.updateMessageBookmark(id, isBookmarked)
    fun getBookmarkedMessages(): Flow<List<ChatMessage>> = chatDao.getBookmarkedMessages()

    // --- Seeding Data ---
    suspend fun seedMockDataIfNeeded() {
        val existing = subjectDao.getAllSubjects().firstOrNull() ?: emptyList()
        if (existing.isNotEmpty()) return

        // Create Seed Subjects
        val subjects = listOf(
            SubjectItem(
                id = "ds_101",
                name = "Data Structures & Algorithms",
                code = "CS-301",
                semester = "Semester 3",
                description = "Master fundamental coding blocks including Trees, Graphs, Sorting, and Complexity."
            ),
            SubjectItem(
                id = "adb_102",
                name = "Database Management Systems",
                code = "CS-302",
                semester = "Semester 3",
                description = "Deep dive into SQL, Normalization, Firestore, NoSQL, and Spanner Database schemas."
            ),
            SubjectItem(
                id = "os_201",
                name = "Operating Systems",
                code = "CS-401",
                semester = "Semester 4",
                description = "Learn process synchronization, thread pooling, scheduling, memory strategies."
            ),
            SubjectItem(
                id = "se_202",
                name = "Software Architecture",
                code = "CS-402",
                semester = "Semester 4",
                description = "Explore Clean Architecture, SOLID design patterns, MVVM pattern, and product scale."
            )
        )
        subjectDao.insertSubjects(subjects)

        // Seed Notes
        val notes = listOf(
            NoteItem(
                subjectId = "ds_101",
                unit = "Unit 1: Fundamentals & Trees",
                title = "Concepts of Binary Search Tree (BST)",
                content = "A Binary Search Tree is a node-based binary tree data structure with properties:\n- The left subtree of a node contains only nodes with keys lesser than the node’s key.\n- The right subtree of a node contains only nodes with keys greater than the node’s key.\n- Left and right subtrees must also be binary search trees.\n\nTime Complexities:\n- Search: O(log n) average, O(n) worst case\n- Insertion: O(log n) average, O(n) worst case\n- Deletion: O(log n) average, O(n) worst case",
                type = "Revision",
                author = "Prof. Alice Carter"
            ),
            NoteItem(
                subjectId = "ds_101",
                unit = "Unit 2: Complexity Analysis",
                title = "Asymptotic Notation Crib Sheet",
                content = "Definitions of Big O, Omega, and Theta notation.\n- Big O (O): Upper bound on progression.\n- Omega (Ω): Lower bound on execution.\n- Theta (Θ): Tight asymptotic bound.\n\nGraph traversal comparisons:\n- DFS: Stack-based, explores deep pathways\n- BFS: Queue-based, level-order sweep.",
                type = "Faculty",
                author = "Prof. Robert Downey"
            ),
            NoteItem(
                subjectId = "adb_102",
                unit = "Unit 1: Normalization Rules",
                title = "1NF, 2NF, 3NF and BCNF Explained",
                content = "Normalization is a systematic approach of decomposing tables to eliminate data redundancy:\n- 1NF: Atomic values, unique column names, no multi-valued attributes.\n- 2NF: Must be in 1NF. No partial dependency (non-key attributes dependent on candidate key subset).\n- 3NF: Must be in 2NF. No transitive dependency (non-key field dependent on another non-key field).\n- BCNF: Stronger version of 3NF. For every functional dependency X -> Y, X must be a super key.",
                type = "PDF note",
                author = "Dr. James Gosling"
            ),
            NoteItem(
                subjectId = "se_202",
                unit = "Unit 1: SOLID Principles",
                title = "SOLID Architecture Quick Reference",
                content = "- S: Single Responsibility Principle (A class should have one reason to change).\n- O: Open/Closed Principle (Open for extension, closed for modification).\n- L: Liskov Substitution Principle (Derived classes must be substitutable for base).\n- I: Interface Segregation Principle (Clients shouldn't be forced to depend on unused interfaces).\n- D: Dependency Inversion Principle (High-level classes shouldn't depend on raw modules).",
                type = "Revision",
                author = "Arch. Sandeep Kumar"
            )
        )
        notesDao.insertNotes(notes)

        // Seed papers
        val papers = listOf(
            QuestionPaperItem(
                subjectId = "ds_101",
                year = "2024",
                regulation = "R22",
                course = "B.Tech Engineering"
            ),
            QuestionPaperItem(
                subjectId = "ds_101",
                year = "2023",
                regulation = "R20",
                course = "Diploma Polytechnic"
            ),
            QuestionPaperItem(
                subjectId = "adb_102",
                year = "2024",
                regulation = "R22",
                course = "B.Tech Engineering"
            ),
            QuestionPaperItem(
                subjectId = "se_202",
                year = "2025",
                regulation = "R22",
                course = "B.Sc Computer Science"
            )
        )
        paperDao.insertPapers(papers)

        // Seed Quizzes and Quiz questions
        val dsQuiz = QuizItem(
            id = "quiz_ds_trees",
            subjectId = "ds_101",
            title = "Tree Traversal Mastery",
            difficulty = "Medium"
        )
        val dsQuestions = listOf(
            QuizQuestion(
                quizId = "quiz_ds_trees",
                questionText = "Which traversal visit nodes in sorted order for a Binary Search Tree?",
                optionA = "Pre-order",
                optionB = "In-order",
                optionC = "Post-order",
                optionD = "Level-order",
                correctAnswer = "B",
                explanation = "In-order traversal visits left subtree, then root, then right subtree, resulting in sorted key order for BSTs."
            ),
            QuizQuestion(
                quizId = "quiz_ds_trees",
                questionText = "What is the worst-case search complexity of an un-balanced BST?",
                optionA = "O(1)",
                optionB = "O(log N)",
                optionC = "O(N)",
                optionD = "O(N log N)",
                correctAnswer = "C",
                explanation = "When a tree is completely skewed (e.g. elements inserted in sorted order), it degenerates into a linked list with search complexity O(N)."
            ),
            QuizQuestion(
                quizId = "quiz_ds_trees",
                questionText = "Which matching data structure is utilized in breadth-first traversal?",
                optionA = "Stack",
                optionB = "Queue",
                optionC = "Priority Queue",
                optionD = "Hash Map",
                correctAnswer = "B",
                explanation = "Breadth-First Search (BFS) / Level-order traversal uses a Queue to track nodes at each level."
            )
        )
        insertQuiz(dsQuiz, dsQuestions)

        val dbQuiz = QuizItem(
            id = "quiz_db_norm",
            subjectId = "adb_102",
            title = "Normalization Fundamentals",
            difficulty = "Hard"
        )
        val dbQuestions = listOf(
            QuizQuestion(
                quizId = "quiz_db_norm",
                questionText = "A table is in 3NF if it contains no transitively dependent non-key attributes. True or False?",
                optionA = "True",
                optionB = "False",
                optionC = "Partially True",
                optionD = "None of the above",
                correctAnswer = "A",
                explanation = "Yes, 3NF ensures no transitively dependent attributes exist. (X -> Y and Y -> Z is forbidden where both are non prime keys)."
            ),
            QuizQuestion(
                quizId = "quiz_db_norm",
                questionText = "If for every Functional Dependency X -> Y, X is a super key, the relation is in which form?",
                optionA = "1NF",
                optionB = "2NF",
                optionC = "3NF",
                optionD = "BCNF",
                correctAnswer = "D",
                explanation = "Boyce-Codd Normal Form (BCNF) strictly requires that for any dependency X -> Y, X must be a superkey."
            )
        )
        insertQuiz(dbQuiz, dbQuestions)
    }
}
