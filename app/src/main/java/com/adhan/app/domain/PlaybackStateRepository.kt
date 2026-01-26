package com.adhan.app.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStateRepository @Inject constructor() {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    fun setPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }
}
