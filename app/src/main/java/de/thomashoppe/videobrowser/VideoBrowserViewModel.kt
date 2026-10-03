package de.thomashoppe.videobrowser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.thomashoppe.videobrowser.auth.GoogleAuth
import de.thomashoppe.videobrowser.data.SubscriptionEntry
import de.thomashoppe.videobrowser.data.SearchFilters
import de.thomashoppe.videobrowser.data.VideoEntry
import de.thomashoppe.videobrowser.data.YouTubeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LoadState<out T>(
    val value: T? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

class VideoBrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: YouTubeRepository = (application as VideoBrowserApplication).repository
    private val appContext = application.applicationContext

    private val _signedInEmail = kotlinx.coroutines.flow.MutableStateFlow(GoogleAuth.signedInEmail(appContext))
    val signedInEmail: StateFlow<String?> = _signedInEmail
    private val _search = kotlinx.coroutines.flow.MutableStateFlow(LoadState<List<VideoEntry>>())
    val search: StateFlow<LoadState<List<VideoEntry>>> = _search
    private val _feed = kotlinx.coroutines.flow.MutableStateFlow(LoadState<List<VideoEntry>>())
    val feed: StateFlow<LoadState<List<VideoEntry>>> = _feed
    private val _subscriptions = kotlinx.coroutines.flow.MutableStateFlow(LoadState<List<SubscriptionEntry>>())
    val subscriptions: StateFlow<LoadState<List<SubscriptionEntry>>> = _subscriptions

    val favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteIds = favorites.map { it.map(VideoEntry::videoId).toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val cached = repository.cachedFeed()
            if (cached.isNotEmpty()) _feed.value = LoadState(value = cached)
            if (signedInEmail.value != null) refreshFeed()
        }
    }

    fun search(query: String) = search(query, SearchFilters())

    fun search(query: String, filters: SearchFilters) = viewModelScope.launch {
        if (query.isBlank()) {
            _search.value = LoadState(value = emptyList())
            return@launch
        }
        _search.value = LoadState(value = _search.value.value, loading = true)
        runCatching { repository.search(query, filters) }
            .onSuccess { _search.value = LoadState(value = it) }
            .onFailure { _search.value = LoadState(value = _search.value.value, error = it.userMessage()) }
    }

    fun refreshFeed() = refreshSubscriptions()

    fun refreshSubscriptions() = viewModelScope.launch {
        if (signedInEmail.value == null) {
            _feed.value = LoadState(error = "Melde dich an, um neue Videos deiner Abos zu sehen.")
            _subscriptions.value = LoadState(error = "Melde dich an, um deine Abos zu sehen.")
            return@launch
        }
        _feed.value = LoadState(value = _feed.value.value, loading = true)
        _subscriptions.value = LoadState(value = _subscriptions.value.value, loading = true)
        runCatching {
            repository.subscriptionOverview(GoogleAuth.accessToken(appContext))
        }.onSuccess {
            val latestVideos = repository.newestVideos(it)
            repository.cacheFeed(latestVideos)
            _subscriptions.value = LoadState(value = it)
            _feed.value = LoadState(value = latestVideos)
        }.onFailure {
            _feed.value = LoadState(value = _feed.value.value, error = it.userMessage())
            _subscriptions.value = LoadState(value = _subscriptions.value.value, error = it.userMessage())
        }
    }

    fun accountChanged() {
        _signedInEmail.value = GoogleAuth.signedInEmail(appContext)
        if (signedInEmail.value != null) refreshFeed()
        else {
            _feed.value = LoadState()
            _subscriptions.value = LoadState()
        }
    }

    fun toggleFavorite(video: VideoEntry) = viewModelScope.launch { repository.toggleFavorite(video) }
    fun removeFavorite(video: VideoEntry) = viewModelScope.launch { repository.removeFavorite(video) }
    fun clearFavorites() = viewModelScope.launch { repository.clearFavorites() }
    fun removeHistory(query: String) = viewModelScope.launch { repository.removeHistory(query) }
    fun clearHistory() = viewModelScope.launch { repository.clearHistory() }

    private fun Throwable.userMessage(): String = when {
        message.isNullOrBlank() -> "Etwas ist schiefgelaufen. Prüfe deine Internetverbindung und versuche es erneut."
        else -> message!!
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = VideoBrowserViewModel(application) as T
        }
    }
}
