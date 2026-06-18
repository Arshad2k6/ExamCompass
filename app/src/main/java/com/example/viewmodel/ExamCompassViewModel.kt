package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ExamCompassViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExamCompassRepository(application)

    // User State
    val userProfile: StateFlow<UserProfile?> = repository.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Subjects & Content Lists
    val subjects: StateFlow<List<SubjectItem>> = repository.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedSubjectId: StateFlow<String?> = _selectedSubjectId.asStateFlow()

    val notesForSelectedSubject: StateFlow<List<NoteItem>> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getNotesBySubject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val papersForSelectedSubject: StateFlow<List<QuestionPaperItem>> = _selectedSubjectId
        .flatMapLatest { id ->
            if (id != null) repository.getPapersBySubject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedNotes: StateFlow<List<NoteItem>> = repository.getBookmarkedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedPapers: StateFlow<List<QuestionPaperItem>> = repository.getBookmarkedPapers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Compass AI Chat ---
    val conversations: StateFlow<List<ChatConversation>> = repository.getSavedConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = _activeConversationId
        .flatMapLatest { convId ->
            if (convId != null) repository.getMessagesForConversation(convId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // --- Quiz & Mock Tests ---
    private val _activeQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val activeQuizQuestions: StateFlow<List<QuizQuestion>> = _activeQuizQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

    private val _quizSelectedAnswerIndex = MutableStateFlow<String?>(null) // "A", "B", "C", "D"
    val quizSelectedAnswerIndex: StateFlow<String?> = _quizSelectedAnswerIndex.asStateFlow()

    private val _showQuizExplanation = MutableStateFlow(false)
    val showQuizExplanation: StateFlow<Boolean> = _showQuizExplanation.asStateFlow()

    private val _quizFinished = MutableStateFlow(false)
    val quizFinished: StateFlow<Boolean> = _quizFinished.asStateFlow()

    // --- Study Planner State ---
    private val _plannerOutput = MutableStateFlow<String?>(null)
    val plannerOutput: StateFlow<String?> = _plannerOutput.asStateFlow()

    private val _isPlanning = MutableStateFlow(false)
    val isPlanning: StateFlow<Boolean> = _isPlanning.asStateFlow()

    // --- AI Exam Predictor State ---
    private val _predictedUnitAnalysis = MutableStateFlow<String?>(null)
    val predictedUnitAnalysis: StateFlow<String?> = _predictedUnitAnalysis.asStateFlow()

    private val _isPredicting = MutableStateFlow(false)
    val isPredicting: StateFlow<Boolean> = _isPredicting.asStateFlow()

    init {
        viewModelScope.launch {
            // Precheck and seed DB
            repository.seedMockDataIfNeeded()
        }
    }

    // --- Setters / Actions ---

    fun selectSubject(subjectId: String?) {
        _selectedSubjectId.value = subjectId
    }

    fun startNewChat(title: String) {
        val newConvId = UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.insertConversation(ChatConversation(id = newConvId, title = title))
            _activeConversationId.value = newConvId
            // System Initial message from Compass AI
            repository.insertMessage(
                ChatMessage(
                    conversationId = newConvId,
                    role = "model",
                    text = "Hello! I am **Compass AI**, your expert personal Tutor. Ask me any question, ask me to summarize a complex topic, or ask me to generate a personalized revision sheet."
                )
            )
        }
    }

    fun selectConversation(convId: String) {
        _activeConversationId.value = convId
    }

    fun sendChatMessage(text: String) {
        val convId = _activeConversationId.value ?: return
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            // User message
            repository.insertMessage(ChatMessage(conversationId = convId, role = "user", text = text))

            _isChatLoading.value = true

            // Generate Prompt context for better EdTech behavior
            val userProfileData = repository.getUserProfile()
            val courseText = userProfileData?.let { "Course: ${it.course}, Branch: ${it.branch}, Semester: ${it.semester}" } ?: ""
            val systemInstruction = "You are Compass AI, a highly technical EdTech tutor for University exams. Be descriptive. Focus heavily on actual formulas, codes, architectures, normalization constraints, and memory strategies depending on user requests. User profile: $courseText."

            val aiResponse = GeminiApiClient.getAiResponse(text, systemInstruction)

            // Save Response
            repository.insertMessage(ChatMessage(conversationId = convId, role = "model", text = aiResponse))
            _isChatLoading.value = false

            // Reward 5 XP for using tutor!
            userProfileData?.let {
                repository.incrementXp(it.email, 5)
            }
        }
    }

    fun toggleMessageBookmark(id: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            repository.updateMessageBookmark(id, isBookmarked)
        }
    }

    // --- Onboarding / Auth Setup ---
    fun registerUser(
        name: String,
        email: String,
        course: String,
        branch: String,
        semester: String,
        college: String
    ) {
        viewModelScope.launch {
            val user = UserProfile(
                email = email,
                name = name,
                profilePhoto = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200",
                course = course,
                branch = branch,
                semester = semester,
                college = college,
                xp = 10, // Free onboarding bonus
                level = 1,
                studyStreak = 1
            )
            repository.saveUserProfile(user)
        }
    }

    // --- Quiz Systems ---
    fun startQuiz(quizId: String) {
        viewModelScope.launch {
            val questions = repository.getQuestionsForQuiz(quizId)
            _activeQuizQuestions.value = questions
            _currentQuestionIndex.value = 0
            _quizScore.value = 0
            _quizSelectedAnswerIndex.value = null
            _showQuizExplanation.value = false
            _quizFinished.value = false
        }
    }

    fun selectQuizAnswer(option: String) {
        if (_quizSelectedAnswerIndex.value != null) return // Already answered
        _quizSelectedAnswerIndex.value = option
        val questions = _activeQuizQuestions.value
        val currentIndex = _currentQuestionIndex.value
        if (questions.isNotEmpty() && currentIndex in questions.indices) {
            val q = questions[currentIndex]
            if (option == q.correctAnswer) {
                _quizScore.value = _quizScore.value + 1
            }
        }
        _showQuizExplanation.value = true
    }

    fun nextQuizQuestion() {
        val questions = _activeQuizQuestions.value
        val nextIndex = _currentQuestionIndex.value + 1
        if (nextIndex < questions.size) {
            _currentQuestionIndex.value = nextIndex
            _quizSelectedAnswerIndex.value = null
            _showQuizExplanation.value = false
        } else {
            // Done! Reward XP
            _quizFinished.value = true
            viewModelScope.launch {
                val profile = repository.getUserProfile()
                if (profile != null) {
                    val scaleScoreFactor = _quizScore.value * 25 // 25 XP per correct answer
                    repository.incrementXp(profile.email, scaleScoreFactor)
                }
            }
        }
    }

    // --- Bookmark Toggles ---
    fun toggleNoteBookmark(noteId: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            repository.updateNoteBookmark(noteId, isBookmarked)
        }
    }

    fun togglePaperBookmark(paperId: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            repository.updatePaperBookmark(paperId, isBookmarked)
        }
    }

    // --- AI Exam Predictor ---
    fun generateExamPrediction(subjectName: String, code: String) {
        _isPredicting.value = true
        _predictedUnitAnalysis.value = null
        viewModelScope.launch {
            val prompt = "Analyze the syllabus weightage for the course subject: $subjectName (Code: $code) with historical trends. Return a structured prediction including: 1. Core predicted units, 2. Frequently asked problems (theory & numerical queries), 3. Weightage patterns. Keep it clear, concise, and structured with bold highlights."
            val prediction = GeminiApiClient.getAiResponse(prompt, "You are a senior academic coordinator and statistical modeler for exams.")
            _predictedUnitAnalysis.value = prediction
            _isPredicting.value = false
        }
    }

    // --- Study Planner ---
    fun generateStudyPlan(examDate: String, availableHours: String, selectedSubjectNames: List<String>) {
        _isPlanning.value = true
        _plannerOutput.value = null
        viewModelScope.launch {
            val subjectsStr = selectedSubjectNames.joinToString(", ")
            val prompt = "Create an optimized, dynamic study plan for exams on $examDate. Available daily hours: $availableHours/day. Subjects to cover: $subjectsStr. Return a scheduled overview (Daily routine + Weekly roadmap) structured in elegant markdown."
            val plan = GeminiApiClient.getAiResponse(prompt, "You are an extreme EdTech study advisor and time management master.")
            _plannerOutput.value = plan
            _isPlanning.value = false
        }
    }

    // --- Admin Operations ---
    fun adminAddSubject(name: String, code: String, semester: String, desc: String) {
        viewModelScope.launch {
            val sub = SubjectItem(
                id = "subj_${UUID.randomUUID().toString().take(6)}",
                name = name,
                code = code,
                semester = semester,
                description = desc
            )
            repository.insertSubject(sub)
        }
    }

    fun adminAddNote(subjectId: String, title: String, content: String, category: String, author: String) {
        viewModelScope.launch {
            val note = NoteItem(
                subjectId = subjectId,
                unit = "Unit Dynamic",
                title = title,
                content = content,
                type = category,
                author = author
            )
            repository.insertNote(note)
        }
    }

    fun adminAddPaper(subjectId: String, year: String, regulation: String, course: String) {
        viewModelScope.launch {
            val paper = QuestionPaperItem(
                subjectId = subjectId,
                year = year,
                regulation = regulation,
                course = course
            )
            repository.insertPaper(paper)
        }
    }
}
