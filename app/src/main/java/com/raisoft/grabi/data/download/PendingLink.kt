package com.raisoft.grabi.data.download

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

@Singleton
class PendingLink @Inject constructor() {
    private val _url = MutableStateFlow<String?>(null)
    val url: StateFlow<String?> = _url.asStateFlow()

    fun offer(link: String) {
        _url.value = link
    }

    fun consume(): String? = _url.getAndUpdate { null }
}