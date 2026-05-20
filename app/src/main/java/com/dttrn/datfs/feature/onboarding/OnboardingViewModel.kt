package com.dttrn.datfs.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.data.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho Onboarding — chỉ cần một hàm markDone
 * để ghi flag vào DataStore.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    fun markOnboardingDone() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingDone()
        }
    }
}
