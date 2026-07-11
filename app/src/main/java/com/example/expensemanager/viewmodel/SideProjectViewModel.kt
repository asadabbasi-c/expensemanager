package com.example.expensemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.data.model.Category
import com.example.expensemanager.data.model.Expense
import com.example.expensemanager.data.model.ProjectTypes
import com.example.expensemanager.data.model.SideProject
import com.example.expensemanager.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SideProjectViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val allProjects: StateFlow<List<SideProject>> = repository.getAllSideProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class ProjectSummary(
        val project: SideProject,
        val spent: Double,
        val remaining: Double,
        val percentUsed: Double,
        val expenseCount: Int
    )

    val projectSummaries: StateFlow<List<ProjectSummary>> = combine(allProjects, allExpenses) { projects, expenses ->
        projects.map { project ->
            val projectExpenses = expenses.filter { it.projectId == project.id }
            val spent = projectExpenses.sumOf { it.amount }
            ProjectSummary(
                project      = project,
                spent        = spent,
                remaining    = (project.budget - spent).coerceAtLeast(0.0),
                percentUsed  = if (project.budget > 0) (spent / project.budget).coerceIn(0.0, 1.5) else 0.0,
                expenseCount = projectExpenses.size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    val selectedProjectExpenses: StateFlow<List<Expense>> = combine(allExpenses, _selectedProjectId) { expenses, id ->
        if (id == null) emptyList() else expenses.filter { it.projectId == id }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedProjectBreakdown: StateFlow<Map<String, Double>> = combine(selectedProjectExpenses, allCategories) { expenses, categories ->
        val categoryMap = categories.associateBy { it.id }
        expenses
            .groupBy { categoryMap[it.categoryId]?.name ?: "Other" }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectProject(id: Long?) {
        _selectedProjectId.value = id
    }

    fun addProject(name: String, type: String, budget: Double = 0.0) {
        viewModelScope.launch {
            val info = ProjectTypes.info(type)
            repository.upsertSideProject(
                SideProject(name = name, icon = info.icon, budget = budget, color = info.color, type = info.key)
            )
        }
    }

    fun updateProject(project: SideProject) {
        viewModelScope.launch { repository.updateSideProject(project) }
    }

    fun deleteProject(project: SideProject) {
        viewModelScope.launch { repository.updateSideProject(project.copy(isActive = false)) }
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SideProjectViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SideProjectViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
