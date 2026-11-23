package com.example.splitthedeals.ui.profile

import androidx.lifecycle.ViewModel
import com.example.splitthedeals.data.Profile
import com.example.splitthedeals.data.ProfileStore
import com.example.splitthedeals.ui.common.UiState
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


class ProfileViewModel(private val store: ProfileStore) : ViewModel() {

    val state: StateFlow<UiState<Profile>> = store.profile
        .map<Profile, UiState<Profile>> { UiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    fun reload() = store.refresh()

    fun save(profile: Profile) = store.save(profile)
}
