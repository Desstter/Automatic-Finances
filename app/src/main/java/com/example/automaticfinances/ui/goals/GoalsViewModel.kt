package com.example.automaticfinances.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.*
import com.example.automaticfinances.data.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalDao: FinancialGoalDao,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(GoalsState())
    val state: StateFlow<GoalsState> = _state.asStateFlow()
    
    private val allGoals = MutableStateFlow<List<GoalWithCategory>>(emptyList())
    
    init {
        loadCategories()
        loadGoals()
        setupFilteredGoals()
    }
    
    private fun setupFilteredGoals() {
        viewModelScope.launch {
            combine(
                allGoals,
                _state.map { it.selectedFilter }
            ) { goals, filter ->
                filterGoals(goals, filter)
            }.collect { filteredGoals ->
                _state.update { it.copy(filteredGoals = filteredGoals) }
            }
        }
    }
    
    fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllActive()
                    .collect { categories ->
                        _state.update { it.copy(categories = categories) }
                    }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error cargando categorías: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun loadGoals() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                goalDao.getGoalsWithCategories()
                    .collect { goals ->
                        allGoals.value = goals
                        _state.update { 
                            it.copy(
                                isLoading = false
                            ) 
                        }
                        loadSummary()
                    }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error cargando metas: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    private fun loadSummary() {
        viewModelScope.launch {
            try {
                val summaryRaw = goalDao.getGoalsSummary(System.currentTimeMillis())
                val summary = summaryRaw?.let {
                    val averageProgress = if (it.totalTargetCents == 0L) 0f 
                                         else (it.totalCurrentCents.toFloat() / it.totalTargetCents.toFloat()) * 100f
                    
                    GoalsSummary(
                        totalGoals = it.totalGoals,
                        activeGoals = it.activeGoals,
                        completedGoals = it.completedGoals,
                        overdueGoals = it.overdueGoals,
                        totalTargetCents = it.totalTargetCents,
                        totalCurrentCents = it.totalCurrentCents,
                        averageProgress = averageProgress
                    )
                }
                
                _state.update { it.copy(summary = summary) }
            } catch (e: Exception) {
                // Summary is not critical, continue without it
            }
        }
    }
    
    fun setFilter(filter: GoalsFilter) {
        _state.update { it.copy(selectedFilter = filter) }
    }
    
    private fun filterGoals(goals: List<GoalWithCategory>, filter: GoalsFilter): List<GoalWithCategory> {
        val currentTime = System.currentTimeMillis()
        
        return when (filter) {
            GoalsFilter.ALL -> goals
            GoalsFilter.ACTIVE -> goals.filter { !it.isCompleted }
            GoalsFilter.COMPLETED -> goals.filter { it.isCompleted }
            GoalsFilter.OVERDUE -> goals.filter { it.targetDate < currentTime && !it.isCompleted }
            GoalsFilter.SAVINGS -> goals.filter { it.type == GoalType.SAVINGS }
            GoalsFilter.EXPENSE_REDUCTION -> goals.filter { it.type == GoalType.EXPENSE_REDUCTION }
        }.sortedWith { goal1, goal2 ->
            when {
                goal1.isCompleted != goal2.isCompleted -> if (goal1.isCompleted) 1 else -1
                goal1.targetDate < currentTime && goal2.targetDate >= currentTime -> -1
                goal1.targetDate >= currentTime && goal2.targetDate < currentTime -> 1
                else -> goal1.targetDate.compareTo(goal2.targetDate)
            }
        }
    }
    
    fun createGoal(
        name: String, 
        description: String, 
        targetAmountCents: Long, 
        targetDate: Long, 
        type: GoalType, 
        categoryId: Long?
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val goal = FinancialGoal(
                    name = name,
                    description = description,
                    targetAmountCents = targetAmountCents,
                    targetDate = targetDate,
                    type = type,
                    categoryId = categoryId,
                    createdAt = System.currentTimeMillis()
                )
                
                goalDao.insertGoal(goal)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error creando meta: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun updateGoal(goal: FinancialGoal) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                goalDao.updateGoal(goal)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error actualizando meta: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun updateGoalProgress(goalId: Long, newAmountCents: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                goalDao.updateGoalProgress(goalId, newAmountCents)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error actualizando progreso: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun markGoalAsCompleted(goalId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                goalDao.markGoalAsCompleted(goalId)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error completando meta: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                goalDao.deleteGoal(goalId)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Error eliminando meta: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun refreshData() {
        loadCategories()
        loadGoals()
    }
}

enum class GoalsFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    OVERDUE,
    SAVINGS,
    EXPENSE_REDUCTION
}

data class GoalsState(
    val filteredGoals: List<GoalWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val summary: GoalsSummary? = null,
    val selectedFilter: GoalsFilter = GoalsFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)