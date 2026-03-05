package com.folio.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.data.local.entity.RecentFileEntity
import com.folio.data.repository.BillingRepository
import com.folio.data.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    val recentFiles: StateFlow<List<RecentFileEntity>> = documentRepository.getRecentFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adsRemoved: StateFlow<Boolean> = billingRepository.adsRemoved

    /**
     * Time-aware greeting: "Good morning", "Good afternoon", "Good evening"
     */
    val greeting: String
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when {
                hour < 12 -> "Good morning"
                hour < 17 -> "Good afternoon"
                else -> "Good evening"
            }
        }

    fun removeRecentFile(file: RecentFileEntity) {
        viewModelScope.launch {
            documentRepository.removeRecentFile(file)
        }
    }
}
