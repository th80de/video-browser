package de.thomashoppe.videobrowser.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.thomashoppe.videobrowser.AppThemeMode
import de.thomashoppe.videobrowser.LoadState
import de.thomashoppe.videobrowser.VideoBrowserViewModel
import de.thomashoppe.videobrowser.data.SubscriptionEntry
import de.thomashoppe.videobrowser.data.SearchFilters
import de.thomashoppe.videobrowser.data.VideoEntry
import de.thomashoppe.videobrowser.util.ExternalYouTube
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.text.NumberFormat

private enum class Destination(val label: String) {
    Search("Suche"), Feed("Neu"), Subscriptions("Abos"), Favorites("Favoriten"), More("Mehr")
}

private enum class SubscriptionSort(val label: String) { LatestVideo("Letztes Video"), SubscribedAt("Abonniert seit") }
private enum class MoreSection { Dashboard, History, Settings }
private enum class FeedCategory { Videos, LiveAndPlanned }
private enum class SearchDateTarget { From, To }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoBrowserApp(
    viewModel: VideoBrowserViewModel,
    email: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    var destination by remember { mutableStateOf(Destination.Search) }
    val favorites by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val favoriteList by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Video Browser") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    if (email == null) {
                        OutlinedButton(onClick = onSignIn) { Text("Anmelden") }
                    } else {
                        IconButton(onClick = onSignOut) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Abmelden")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = {
                            destination = item
                            if (item == Destination.Subscriptions && subscriptions.value == null && !subscriptions.loading) {
                                viewModel.refreshSubscriptions()
                            }
                        },
                        icon = { Icon(iconFor(item), contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (destination) {
                Destination.Search -> SearchScreen(
                    search,
                    favorites,
                    { query, filters -> viewModel.search(query, filters) },
                    viewModel::toggleFavorite,
                )
                Destination.Feed -> FeedScreen(feed, favorites, email, viewModel::refreshFeed, onSignIn, viewModel::toggleFavorite)
                Destination.Subscriptions -> SubscriptionScreen(subscriptions, email, viewModel::refreshSubscriptions, onSignIn)
                Destination.Favorites -> FavoritesScreen(favoriteList, viewModel::removeFavorite, viewModel::clearFavorites)
                Destination.More -> MoreScreen(
                    subscriptions = subscriptions,
                    email = email,
                    onRefreshSubscriptions = viewModel::refreshSubscriptions,
                    onSignIn = onSignIn,
                    history = history,
                    onSearch = viewModel::search,
                    onRemoveHistory = viewModel::removeHistory,
                    onClearHistory = viewModel::clearHistory,
                    themeMode = themeMode,
                    onThemeModeChanged = onThemeModeChanged,
                )
            }
        }
    }
}

private fun iconFor(destination: Destination): ImageVector = when (destination) {
    Destination.Search -> Icons.Filled.Search
    Destination.Feed -> Icons.Filled.Subscriptions
    Destination.Subscriptions -> Icons.Filled.Subscriptions
    Destination.Favorites -> Icons.Filled.Favorite
    Destination.More -> Icons.Filled.MoreHoriz
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    state: LoadState<List<VideoEntry>>,
    favoriteIds: Set<String>,
    onSearch: (String, SearchFilters) -> Unit,
    onToggleFavorite: (VideoEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filtersOpen by remember { mutableStateOf(false) }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }
    var excludedTerms by remember { mutableStateOf("") }
    var filterError by remember { mutableStateOf<String?>(null) }
    var datePickerTarget by remember { mutableStateOf<SearchDateTarget?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val submitSearch: () -> Unit = {
        val filters = runCatching { buildSearchFilters(fromDate, toDate, excludedTerms) }
            .onFailure { filterError = it.message ?: "Ungültige Filter" }
            .getOrNull()
        if (filters != null) {
            filterError = null
            onSearch(query, filters)
            keyboard?.hide()
        }
        Unit
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            label = { Text("Videos suchen") },
            trailingIcon = { IconButton(onClick = submitSearch) { Icon(Icons.Filled.Search, "Suchen") } },
        )
        IconButton(
            onClick = { filtersOpen = !filtersOpen },
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.FilterList,
                contentDescription = if (filtersOpen) "Filter ausblenden" else "Filter anzeigen",
            )
        }
        }
        if (filtersOpen) {
            OutlinedButton(
                onClick = { datePickerTarget = SearchDateTarget.From },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Von: ${fromDate.ifBlank { "beliebig" }}")
            }
            OutlinedButton(
                onClick = { datePickerTarget = SearchDateTarget.To },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Bis: ${toDate.ifBlank { "beliebig" }}")
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                value = excludedTerms,
                onValueChange = { excludedTerms = it },
                singleLine = true,
                label = { Text("Ausschließen, mit Komma trennen") },
            )
            filterError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(12.dp))
        LoadableVideoList(state, favoriteIds, onToggleFavorite, emptyText = "Suche nach Videos, Kanälen oder Themen.")
    }
    datePickerTarget?.let { target ->
        val selectedDate = if (target == SearchDateTarget.From) fromDate else toDate
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = datePickerInitialMillis(selectedDate))
        DatePickerDialog(
            onDismissRequest = { datePickerTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val value = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                            .format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        if (target == SearchDateTarget.From) fromDate = value else toDate = value
                    }
                    datePickerTarget = null
                }) { Text("Übernehmen") }
            },
            dismissButton = { TextButton(onClick = { datePickerTarget = null }) { Text("Abbrechen") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SubscriptionScreen(
    state: LoadState<List<SubscriptionEntry>>,
    email: String?,
    onRefresh: () -> Unit,
    onSignIn: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Meine Abos", style = MaterialTheme.typography.headlineSmall)
                Text(email ?: "Melde dich an, um deine Abos zu sehen.", style = MaterialTheme.typography.bodySmall)
            }
            if (email == null) Button(onClick = onSignIn) { Text("Anmelden") }
            else IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Aktualisieren") }
        }
        Spacer(Modifier.height(12.dp))
        if (email == null) {
            EmptyState("Die Liste deiner Abos bleibt bei Google und wird nur lesend geladen.")
        } else when {
            state.loading && state.value == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            state.error != null && state.value == null -> EmptyState(state.error)
            state.value == null -> EmptyState("Abo-Daten werden geladen …")
            else -> SubscriptionList(state.value!!)
        }
    }
}

@Composable
private fun SubscriptionList(subscriptions: List<SubscriptionEntry>) {
    var sort by remember { mutableStateOf(SubscriptionSort.LatestVideo) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val ordered = remember(subscriptions, sort) {
        when (sort) {
            SubscriptionSort.LatestVideo -> subscriptions.sortedByDescending { it.latestVideo?.publishedAt?.asTimestamp() ?: Long.MIN_VALUE }
            SubscriptionSort.SubscribedAt -> subscriptions.sortedByDescending { it.subscribedAt.asTimestamp() }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("${subscriptions.size} Abos · ${sort.label}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { sortMenuOpen = true }) { Icon(Icons.Filled.Sort, "Sortierung ändern") }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                SubscriptionSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = { sort = option; sortMenuOpen = false },
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        items(ordered, key = { it.channelId }) { subscription -> SubscriptionCard(subscription, sort) }
    }
}

@Composable
private fun SubscriptionCard(subscription: SubscriptionEntry, sort: SubscriptionSort) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .clickable { ExternalYouTube.openChannel(context, subscription.channelId) }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = subscription.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(subscription.channelTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            val latest = subscription.latestVideo
            Text(
                latest?.let { "Neu: ${it.title}" } ?: "Kein öffentliches Video gefunden",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when (sort) {
                    SubscriptionSort.LatestVideo -> latest?.let { "Letztes Video vom: ${formatDate(it.publishedAt)}" }
                        ?: "Letztes Video: keines gefunden"
                    SubscriptionSort.SubscribedAt -> "Abo seit: ${formatDate(subscription.subscribedAt)}"
                },
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun MoreScreen(
    subscriptions: LoadState<List<SubscriptionEntry>>,
    email: String?,
    onRefreshSubscriptions: () -> Unit,
    onSignIn: () -> Unit,
    history: List<String>,
    onSearch: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
) {
    var section by remember { mutableStateOf<MoreSection?>(null) }
    if (section == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Mehr", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text("Einstellungen") },
                supportingContent = { Text("Darstellung und App-Optionen") },
                modifier = Modifier.clickable { section = MoreSection.Settings },
            )
            ListItem(
                headlineContent = { Text("Abo-Dashboard") },
                supportingContent = { Text("Aktivität, Größen und Zeiträume deiner Abos") },
                modifier = Modifier.clickable {
                    section = MoreSection.Dashboard
                    if (subscriptions.value == null && !subscriptions.loading) onRefreshSubscriptions()
                },
            )
            ListItem(
                headlineContent = { Text("Suchverlauf") },
                supportingContent = { Text("Frühere Suchanfragen erneut verwenden oder löschen") },
                modifier = Modifier.clickable { section = MoreSection.History },
            )
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { section = null }) { Icon(Icons.Filled.ArrowBack, "Zurück") }
            Text(
                when (section) {
                    MoreSection.Dashboard -> "Abo-Dashboard"
                    MoreSection.History -> "Suchverlauf"
                    MoreSection.Settings -> "Einstellungen"
                    null -> "Mehr"
                },
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Spacer(Modifier.height(8.dp))
        when (section) {
            MoreSection.Dashboard -> when {
                email == null -> EmptyState("Melde dich an, um dein Abo-Dashboard zu sehen.")
                subscriptions.loading && subscriptions.value == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                subscriptions.error != null && subscriptions.value == null -> EmptyState(subscriptions.error)
                subscriptions.value != null -> SubscriptionDashboard(subscriptions.value!!)
                else -> EmptyState("Abo-Daten werden geladen …")
            }
            MoreSection.History -> HistoryContent(history, onSearch, onRemoveHistory, onClearHistory)
            MoreSection.Settings -> SettingsContent(themeMode, onThemeModeChanged)
            null -> Unit
        }
    }
}

@Composable
private fun SettingsContent(themeMode: AppThemeMode, onThemeModeChanged: (AppThemeMode) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Darstellung", style = MaterialTheme.typography.titleMedium)
        Text("Wähle, wie die App dargestellt wird.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        AppThemeMode.entries.forEach { mode ->
            ListItem(
                headlineContent = { Text(mode.label) },
                supportingContent = {
                    Text(
                        when (mode) {
                            AppThemeMode.System -> "Folgt der Einstellung deines Smartphones"
                            AppThemeMode.Light -> "Helle Darstellung"
                            AppThemeMode.Dark -> "Dunkle Darstellung"
                        },
                    )
                },
                trailingContent = {
                    RadioButton(selected = themeMode == mode, onClick = { onThemeModeChanged(mode) })
                },
                modifier = Modifier.clickable { onThemeModeChanged(mode) },
            )
        }
    }
}

@Composable
private fun SubscriptionDashboard(subscriptions: List<SubscriptionEntry>) {
    val knownSubscribers = subscriptions.mapNotNull { it.subscriberCount }
    val knownVideoCounts = subscriptions.mapNotNull { it.videoCount }
    val channelsWithVideos = subscriptions.count { it.latestVideo != null }
    val mostSubscribed = subscriptions.filter { it.subscriberCount != null }.sortedByDescending { it.subscriberCount }.take(5)
    val recent = subscriptions.filter { it.latestVideo != null }.sortedByDescending { it.latestVideo?.publishedAt }.take(5)
    val oldestSubscription = subscriptions.minByOrNull { it.subscribedAt.asTimestamp() }
    val newestSubscription = subscriptions.maxByOrNull { it.subscribedAt.asTimestamp() }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text("Überblick", style = MaterialTheme.typography.titleMedium)
            Text("${subscriptions.size} Kanäle · $channelsWithVideos mit öffentlichem Upload", style = MaterialTheme.typography.bodyLarge)
            if (knownVideoCounts.isNotEmpty()) {
                Text("${formatNumber(knownVideoCounts.sum())} Videos über ${knownVideoCounts.size} Kanäle", style = MaterialTheme.typography.bodySmall)
            }
            if (knownSubscribers.isNotEmpty()) {
                Text(
                    "${formatNumber(knownSubscribers.sum())} Abonnenten über ${knownSubscribers.size} sichtbare Kanäle",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (oldestSubscription != null && newestSubscription != null) {
                Text(
                    "Abozeitraum: ${formatDate(oldestSubscription.subscribedAt)} bis ${formatDate(newestSubscription.subscribedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (mostSubscribed.isNotEmpty()) {
            item { Text("Größte Kanäle", style = MaterialTheme.typography.titleMedium) }
            items(mostSubscribed, key = { "sub-${it.channelId}" }) { subscription ->
                ListItem(
                    headlineContent = { Text(subscription.channelTitle) },
                    supportingContent = { Text("${formatNumber(subscription.subscriberCount ?: 0)} Abonnenten · ${subscription.videoCount ?: 0} Videos") },
                )
            }
        }
        if (recent.isNotEmpty()) {
            item { Text("Zuletzt aktiv", style = MaterialTheme.typography.titleMedium) }
            items(recent, key = { "recent-${it.channelId}" }) { subscription ->
                ListItem(
                    headlineContent = { Text(subscription.channelTitle) },
                    supportingContent = { Text(subscription.latestVideo?.title ?: "") },
                    trailingContent = { Text(formatDate(subscription.latestVideo?.publishedAt.orEmpty())) },
                )
            }
        }
    }
}

private fun formatNumber(value: Long): String = NumberFormat.getIntegerInstance().format(value)

private fun String.asTimestamp(): Long = runCatching { Instant.parse(this).toEpochMilli() }.getOrDefault(Long.MIN_VALUE)

@Composable
private fun FeedScreen(
    state: LoadState<List<VideoEntry>>,
    favoriteIds: Set<String>,
    email: String?,
    onRefresh: () -> Unit,
    onSignIn: () -> Unit,
    onToggleFavorite: (VideoEntry) -> Unit,
) {
    var category by remember { mutableStateOf(FeedCategory.Videos) }
    val standardVideos = state.value?.filter { it.liveStatus == "none" }.orEmpty()
    val liveAndPlanned = state.value?.filter { it.liveStatus == "live" || it.liveStatus == "upcoming" }
        .orEmpty().sortedBy { it.scheduledStartAt?.asTimestamp() ?: it.publishedAt.asTimestamp() }
    val selectedVideos = if (category == FeedCategory.Videos) standardVideos else liveAndPlanned
    val selectedState = state.copy(value = state.value?.let { selectedVideos })
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Neue Videos", style = MaterialTheme.typography.headlineSmall)
                Text(email ?: "Melde dich für deinen persönlichen Feed an.", style = MaterialTheme.typography.bodySmall)
            }
            if (email == null) Button(onClick = onSignIn) { Text("Anmelden") }
            else IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Aktualisieren") }
        }
        Spacer(Modifier.height(12.dp))
        if (email == null) EmptyState("Deine Abos bleiben bei Google. Die App fordert nur Leserechte an.")
        else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (category == FeedCategory.Videos) Button(onClick = {}) { Text("Videos") }
                else OutlinedButton(onClick = { category = FeedCategory.Videos }) { Text("Videos") }
                if (category == FeedCategory.LiveAndPlanned) Button(onClick = {}) { Text("Live & geplant (${liveAndPlanned.size})") }
                else OutlinedButton(onClick = { category = FeedCategory.LiveAndPlanned }) { Text("Live & geplant (${liveAndPlanned.size})") }
            }
            Spacer(Modifier.height(8.dp))
            LoadableVideoList(
                selectedState,
                favoriteIds,
                onToggleFavorite,
                emptyText = if (category == FeedCategory.Videos) "Keine neuen veröffentlichten Uploads gefunden."
                else "Keine laufenden oder geplanten Streams gefunden.",
            )
        }
    }
}

@Composable
private fun LoadableVideoList(
    state: LoadState<List<VideoEntry>>,
    favoriteIds: Set<String>,
    onToggleFavorite: (VideoEntry) -> Unit,
    emptyText: String,
) {
    when {
        state.loading && state.value.isNullOrEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        state.error != null && state.value.isNullOrEmpty() -> EmptyState(state.error)
        state.value.isNullOrEmpty() -> EmptyState(emptyText)
        else -> {
            Column(Modifier.fillMaxSize()) {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
                state.loading.let { if (it) CircularProgressIndicator(Modifier.size(20.dp).align(Alignment.CenterHorizontally)) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(state.value!!, key = { it.videoId }) { video ->
                        VideoCard(video, video.videoId in favoriteIds, onToggleFavorite)
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoEntry, favorite: Boolean, onToggleFavorite: (VideoEntry) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .clickable { ExternalYouTube.openVideo(context, video) }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 120.dp, height = 68.dp).clip(MaterialTheme.shapes.small),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(video.channelTitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { ExternalYouTube.openChannel(context, video.channelId) })
            Text(videoStatusLabel(video), style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = { onToggleFavorite(video) }) {
            Icon(if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (favorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen")
        }
    }
}

@Composable
private fun FavoritesScreen(items: List<VideoEntry>, onRemove: (VideoEntry) -> Unit, onClear: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Favoriten", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.Delete, "Alle löschen") }
        }
        OutlinedButton(onClick = { ExternalYouTube.openWatchLater(context) }) { Text("Später ansehen in YouTube öffnen") }
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) EmptyState("Noch keine Favoriten gespeichert.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(items, key = { it.videoId }) { VideoCard(it, true, onRemove) } }
    }
    if (confirmClear) ConfirmClear("Alle Favoriten löschen?", { onClear(); confirmClear = false }, { confirmClear = false })
}

@Composable
private fun HistoryContent(items: List<String>, onSearch: (String) -> Unit, onRemove: (String) -> Unit, onClear: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("${items.size} Suchanfragen", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (items.isNotEmpty()) IconButton(onClick = { confirmClear = true }) { Icon(Icons.Filled.Delete, "Alle löschen") }
        }
        if (items.isEmpty()) EmptyState("Deine letzten Suchanfragen erscheinen hier.")
        else LazyColumn { items(items, key = { it }) { query ->
            ListItem(
                headlineContent = { Text(query) },
                modifier = Modifier.clickable { onSearch(query) },
                trailingContent = { IconButton(onClick = { onRemove(query) }) { Icon(Icons.Filled.Delete, "Löschen") } },
            )
        } }
    }
    if (confirmClear) ConfirmClear("Den gesamten Suchverlauf löschen?", { onClear(); confirmClear = false }, { confirmClear = false })
}

@Composable
private fun EmptyState(text: String) = Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun ConfirmClear(text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wirklich löschen?") },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm) { Text("Löschen") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

private fun formatDate(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault("")

private fun videoStatusLabel(video: VideoEntry): String = when (video.liveStatus) {
    "live" -> "LIVE"
    "upcoming" -> video.scheduledStartAt?.let { "Geplant: ${formatDate(it)}" } ?: "Geplant"
    else -> formatDate(video.publishedAt)
}

private fun buildSearchFilters(fromText: String, toText: String, excludedTerms: String): SearchFilters {
    val dateFormatter = DateTimeFormatter.ofPattern("d.M.uuuu")
    val from = fromText.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, dateFormatter) }
    val to = toText.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, dateFormatter) }
    require(from == null || to == null || !from.isAfter(to)) { "„Von“ darf nicht nach „Bis“ liegen." }
    return SearchFilters(
        publishedAfter = from?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toString(),
        // End of the selected calendar day, expressed as the next midnight.
        publishedBefore = to?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toString(),
        excludedTerms = excludedTerms,
    )
}

private fun datePickerInitialMillis(value: String): Long? = runCatching {
    LocalDate.parse(value, DateTimeFormatter.ofPattern("d.M.uuuu"))
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}.getOrNull()
