package com.example.splitthedeals.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Profile(
    val photoUri: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val description: String = "",
)

class ProfileStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("split_profile", Context.MODE_PRIVATE)

    private val flow: MutableStateFlow<Profile> =
        sharedFlow ?: MutableStateFlow(load()).also { sharedFlow = it }

    val profile: StateFlow<Profile> = flow.asStateFlow()

    fun load(): Profile = Profile(
        photoUri = prefs.getString(KEY_PHOTO, "") ?: "",
        name = prefs.getString(KEY_NAME, "") ?: "",
        email = prefs.getString(KEY_EMAIL, "") ?: "",
        phone = prefs.getString(KEY_PHONE, "") ?: "",
        description = prefs.getString(KEY_DESC, "") ?: "",
    )

    fun refresh() { flow.value = load() }
    fun save(profile: Profile) {
        prefs.edit {
            putString(KEY_PHOTO, profile.photoUri)
            putString(KEY_NAME, profile.name)
            putString(KEY_EMAIL, profile.email)
            putString(KEY_PHONE, profile.phone)
            putString(KEY_DESC, profile.description)
        }
        flow.value = profile
    }

    private companion object {
        @Volatile
        var sharedFlow: MutableStateFlow<Profile>? = null

        const val KEY_PHOTO = "photoUri"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_PHONE = "phone"
        const val KEY_DESC = "description"
    }
}