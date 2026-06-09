package com.example.automaticfinances.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automaticfinances.data.db.Category
import com.example.automaticfinances.data.db.CategoryRule
import com.example.automaticfinances.data.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the editable keyword→category rule screen (MANT-2). Reads the rule table and the active
 * categories live, splits rules into income/expense, and exposes add/delete. Rules whose target
 * category no longer exists are still listed (flagged via [RuleRow.categoryExists]) so the user can
 * notice and remove them.
 */
@HiltViewModel
class CategoryRulesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    data class RuleRow(
        val rule: CategoryRule,
        val categoryIcon: String?,
        val categoryExists: Boolean,
    )

    data class State(
        val expenseRules: List<RuleRow> = emptyList(),
        val incomeRules: List<RuleRow> = emptyList(),
        val expenseCategories: List<Category> = emptyList(),
        val incomeCategories: List<Category> = emptyList(),
        val isLoading: Boolean = true,
    )

    val state: StateFlow<State> =
        combine(
            categoryRepository.getAllRules(),
            categoryRepository.getAllActive(),
        ) { rules, categories ->
            fun rowsFor(isIncome: Boolean) = rules
                .filter { it.isIncome == isIncome }
                .sortedBy { it.keyword }
                .map { rule ->
                    val cat = categories.find { it.name == rule.categoryName && it.isIncome == isIncome }
                    RuleRow(rule = rule, categoryIcon = cat?.icon, categoryExists = cat != null)
                }
            State(
                expenseRules = rowsFor(isIncome = false),
                incomeRules = rowsFor(isIncome = true),
                expenseCategories = categories.filter { !it.isIncome },
                incomeCategories = categories.filter { it.isIncome },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = State(),
        )

    fun addRule(keyword: String, categoryName: String, isIncome: Boolean) {
        viewModelScope.launch {
            categoryRepository.addRule(keyword, categoryName, isIncome)
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            categoryRepository.deleteRule(id)
        }
    }
}
