package com.twofasapp.feature.home.ui.services

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twofasapp.common.domain.Service
import com.twofasapp.core.design.MdtIcons
import com.twofasapp.core.design.MdtTheme
import com.twofasapp.core.design.feature.items.ServiceAuthType
import com.twofasapp.core.design.feature.items.ServiceImageType
import com.twofasapp.core.design.feature.items.ServiceState
import com.twofasapp.core.design.feature.items.animateExpireColor
import com.twofasapp.core.design.feature.items.asState
import com.twofasapp.core.design.feature.items.atoms.formatCode
import com.twofasapp.core.design.feature.settings.SettingsLink
import com.twofasapp.core.design.foundation.button.OutlinedButton
import com.twofasapp.core.design.foundation.dialog.BaseDialog
import com.twofasapp.core.design.foundation.dialog.ConfirmDialog
import com.twofasapp.core.design.foundation.dialog.InfoDialog
import com.twofasapp.core.design.foundation.dialog.ListRadioDialog
import com.twofasapp.core.design.foundation.screen.EmptyScreen
import com.twofasapp.core.design.ktx.assetAsBitmap
import com.twofasapp.core.design.ktx.copyToClipboard
import com.twofasapp.core.design.ktx.currentActivity
import com.twofasapp.core.design.ktx.openSafely
import com.twofasapp.core.design.ktx.toastShort
import com.twofasapp.data.services.domain.Group
import com.twofasapp.data.session.domain.ServicesSort
import com.twofasapp.data.session.domain.ServicesStyle
import com.twofasapp.feature.home.R
import com.twofasapp.feature.home.navigation.EditServiceInitialAction
import com.twofasapp.feature.home.navigation.HomeNavigationListener
import com.twofasapp.feature.home.ui.bottombar.BottomBarListener
import com.twofasapp.feature.home.ui.editservice.QrGenerator
import com.twofasapp.feature.home.ui.services.component.AppReviewItem
import com.twofasapp.feature.home.ui.services.component.PassBanner
import com.twofasapp.feature.home.ui.services.component.ServicesProgress
import com.twofasapp.feature.home.ui.services.component.SyncNoticeBar
import com.twofasapp.feature.home.ui.services.component.SyncReminderItem
import com.twofasapp.locale.TwLocale
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import com.twofasapp.locale.R as LocaleR

internal sealed interface SelectedGroup {
    data object All : SelectedGroup
    data object Default : SelectedGroup
    data class Custom(val id: String) : SelectedGroup
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun RefreshedServicesScreen(
    uiState: ServicesUiState,
    listener: HomeNavigationListener,
    bottomBarListener: BottomBarListener,
    onEventConsumed: (ServicesUiEvent) -> Unit,
    onExternalImportClick: () -> Unit = {},
    onDragStart: () -> Unit,
    onDragEnd: (List<Long>) -> Unit,
    onSortChange: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchFocusChange: (Boolean) -> Unit,
    onOpenBackupClick: (Boolean) -> Unit = {},
    onDismissSyncReminderClick: () -> Unit = {},
    onRateAppClick: (Activity) -> Unit = {},
    onDismissAppReviewClick: () -> Unit = {},
    onDismissPassBannerClick: () -> Unit = {},
    onDisablePassBannerClick: () -> Unit = {},
    onIncrementHotpCounterClick: (Service) -> Unit,
    onRevealClick: (Service) -> Unit,
    onPullBrowserRequest: () -> Unit,
    onServiceGroupChange: (Long, String?) -> Unit,
    onTrashService: (Long) -> Unit,
    onAuthenticate: (successCallback: () -> Unit) -> Unit,
) {
    val activity = LocalContext.currentActivity
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    var selectedGroupKey by rememberSaveable { mutableStateOf(GroupAllKey) }
    var showMenu by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showQrFromGalleryDialog by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var serviceForGroupAssignment by remember { mutableStateOf<Service?>(null) }
    var servicePendingDelete by remember { mutableStateOf<Service?>(null) }
    var serviceForQr by remember { mutableStateOf<Service?>(null) }
    var showQrNoLockDialog by remember { mutableStateOf(false) }
    var groupAnimationVersion by remember { mutableStateOf(0) }

    val selectedGroup = selectedGroupKey.toSelectedGroup()
    val isSearching = uiState.searchFocused || uiState.searchQuery.isNotEmpty()
    val visibleServices = uiState.services.filter { service ->
        when (selectedGroup) {
            SelectedGroup.All -> true
            SelectedGroup.Default -> service.groupId == null
            is SelectedGroup.Custom -> service.groupId == selectedGroup.id
        }
    }
    val manualSorting = uiState.appSettings.servicesSort == ServicesSort.Manual
    // Keep the state holder stable so the reorder controller never writes into an obsolete list after a save.
    var reorderableServices by remember { mutableStateOf(visibleServices) }
    var isDragging by remember { mutableStateOf(false) }
    val reorderableState = rememberReorderableLazyListState(
        listState = listState,
        canDragOver = { draggedOver, _ -> draggedOver.key is Long },
        onMove = { from, to ->
            val fromIndex = reorderableServices.indexOfFirst { it.id == from.key }
            val toIndex = reorderableServices.indexOfFirst { it.id == to.key }

            if (fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                if (isDragging.not()) {
                    onDragStart()
                    isDragging = true
                }

                reorderableServices = reorderableServices.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
            }
        },
        onDragEnd = { _, _ ->
            if (isDragging) {
                onDragEnd(reorderableServices.map { it.id })
                scope.launch {
                    delay(500)
                    isDragging = false
                }
            }
        },
    )

    LaunchedEffect(visibleServices, isDragging) {
        if (isDragging.not()) {
            reorderableServices = visibleServices
        }
    }

    LaunchedEffect(uiState.groups, selectedGroup) {
        if (selectedGroup is SelectedGroup.Custom && uiState.groups.none { it.id == selectedGroup.id }) {
            selectedGroupKey = GroupAllKey
        }
    }

    uiState.events.firstOrNull()?.let { event ->
        when (event) {
            ServicesUiEvent.ShowQrFromGalleryDialog -> showQrFromGalleryDialog = true
            is ServicesUiEvent.ServiceAdded -> {
                val added = uiState.getService(event.id)
                val isVisible = added != null && when (selectedGroup) {
                    SelectedGroup.All -> true
                    SelectedGroup.Default -> added.groupId == null
                    is SelectedGroup.Custom -> added.groupId == selectedGroup.id
                }

                if (isVisible) {
                    LaunchedEffect(event.id) {
                        val index = visibleServices.indexOfFirst { it.id == event.id }
                        if (index >= 0) listState.animateScrollToItem(index)
                    }
                }
            }

            is ServicesUiEvent.OpenImport -> listener.openBackupImport(event.filePath)
            is ServicesUiEvent.OpenBrowserRequest -> listener.openBrowserExtRequest(event.request)
            ServicesUiEvent.NoPendingBrowserRequest -> activity.toastShort(TwLocale.strings.browserPullRequestEmpty)
            ServicesUiEvent.BrowserRequestPullFailed -> activity.toastShort(TwLocale.strings.browserPullRequestFailed)
        }

        onEventConsumed(event)
    }

    LaunchedEffect(Unit) {
        if (uiState.searchFocused) {
            awaitFrame()
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(uiState.searchFocused) {
        if (uiState.searchFocused) {
            awaitFrame()
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { onSearchFocusChange(false) }
    }

    BackHandler(enabled = isSearching) {
        when {
            uiState.searchQuery.isNotEmpty() -> onSearchQueryChange("")
            else -> onSearchFocusChange(false)
        }
    }

    Scaffold(
        containerColor = MdtTheme.color.background,
        topBar = {
            RefreshedServicesTopBar(
                title = selectedGroup.title(uiState.groups),
                query = uiState.searchQuery,
                searching = isSearching,
                focusRequester = focusRequester,
                onSortClick = { showSortDialog = true },
                onQueryChange = onSearchQueryChange,
                onSearchClose = {
                    onSearchQueryChange("")
                    onSearchFocusChange(false)
                },
            )
        },
        bottomBar = {
            RefreshedServicesBottomBar(
                showBrowserRequestPull = uiState.showBrowserRequestPull,
                browserRequestPulling = uiState.browserRequestPulling,
                onMenuClick = { showMenu = true },
                onSearchClick = { onSearchFocusChange(true) },
                onBrowserRequestPull = onPullBrowserRequest,
                onAddClick = {
                    onSearchFocusChange(false)
                    listener.openAddServiceModal()
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = reorderableState.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .reorderable(reorderableState),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isLoading) {
                item {
                    ServicesProgress(Modifier.fillParentMaxSize())
                }
                return@LazyColumn
            }

            if (uiState.showSyncNoticeBar) {
                item {
                    SyncNoticeBar(
                        modifier = Modifier.fillMaxWidth(),
                        onOpenBackupClick = { onOpenBackupClick(false) },
                    )
                }
            }

            when {
                uiState.showSyncReminder -> {
                    item {
                        SyncReminderItem(
                            modifier = Modifier.fillMaxWidth(),
                            onOpenBackupClick = { onOpenBackupClick(true) },
                            onDismissClick = onDismissSyncReminderClick,
                        )
                    }
                }

                uiState.showAppReview -> {
                    item {
                        AppReviewItem(
                            modifier = Modifier.fillMaxWidth(),
                            onRateClick = { onRateAppClick(activity) },
                            onDismissClick = onDismissAppReviewClick,
                        )
                    }
                }

                uiState.showPassBanner -> {
                    item {
                        PassBanner(
                            modifier = Modifier.fillMaxWidth(),
                            onGoToStoreClick = {
                                uriHandler.openSafely(TwLocale.links.passPlayStore, activity)
                                onDismissPassBannerClick()
                            },
                            onDismissClick = onDisablePassBannerClick,
                        )
                    }
                }
            }

            if (uiState.totalServices == 0) {
                item {
                    EmptyScreen(
                        body = TwLocale.strings.servicesEmptyBody,
                        image = painterResource(id = R.drawable.img_services_empty),
                        additionalContent = {
                            OutlinedButton(
                                text = TwLocale.strings.servicesEmptyImportCta,
                                onClick = onExternalImportClick,
                            )
                        },
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
                return@LazyColumn
            }

            if (visibleServices.isEmpty()) {
                item {
                    EmptyScreen(
                        title = TwLocale.strings.servicesEmptySearch,
                        body = TwLocale.strings.servicesEmptySearchBody,
                        image = painterResource(id = R.drawable.img_services_empty_search),
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
                return@LazyColumn
            }

            itemsIndexed(
                items = reorderableServices,
                key = { _, service -> service.id },
            ) { index, service ->
                val serviceState = service.asState()
                ReorderableItem(
                    state = reorderableState,
                    key = service.id,
                ) { itemDragging ->
                    LaunchedEffect(itemDragging) {
                        if (itemDragging) {
                            if (isDragging.not()) {
                                onDragStart()
                                isDragging = true
                            }
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    StratumGroupTransition(
                        animationVersion = groupAnimationVersion,
                        itemIndex = index,
                    ) {
                        RefreshedServiceCard(
                            state = serviceState,
                            style = uiState.appSettings.servicesStyle,
                            showNextCode = uiState.appSettings.showNextCode,
                            hideCodes = uiState.appSettings.hideCodes,
                            isDragging = itemDragging,
                            modifier = if (manualSorting) {
                                Modifier.detectReorderAfterLongPress(reorderableState)
                            } else {
                                Modifier
                            },
                            onClick = { serviceState.copyToClipboard(activity, uiState.appSettings.showNextCode) },
                            onLongClick = if (manualSorting) {
                                null
                            } else {
                                {
                                    keyboardController?.hide()
                                    selectedService = service
                                }
                            },
                            onMoreClick = {
                                keyboardController?.hide()
                                selectedService = service
                            },
                            onIncrementCounterClick = { onIncrementHotpCounterClick(service) },
                            onRevealClick = { onRevealClick(service) },
                        )
                    }
                }
            }
        }
    }

    if (showMenu) {
        HomeMenuSheet(
            groups = uiState.groups,
            selectedGroup = selectedGroup,
            onDismiss = { showMenu = false },
            onGroupSelected = { group ->
                val nextGroupKey = group.key()
                if (selectedGroupKey != nextGroupKey) {
                    selectedGroupKey = nextGroupKey
                    groupAnimationVersion += 1
                }
                scope.launch { listState.scrollToItem(0) }
            },
            onManageGroups = bottomBarListener::openGroups,
            onSettingsClick = bottomBarListener::openSettings,
        )
    }

    selectedService?.let { service ->
        ServiceActionsSheet(
            onDismiss = { selectedService = null },
            onEditDetails = {
                selectedService = null
                listener.openService(activity, service.id, EditServiceInitialAction.Details)
            },
            onChangeIcon = {
                selectedService = null
                listener.openService(activity, service.id, EditServiceInitialAction.Icon)
            },
            onAssignGroup = {
                selectedService = null
                serviceForGroupAssignment = service
            },
            onShowQr = {
                selectedService = null
                if (uiState.hasLock) {
                    onAuthenticate { serviceForQr = uiState.getService(service.id) ?: service }
                } else {
                    showQrNoLockDialog = true
                }
            },
            onDelete = {
                selectedService = null
                servicePendingDelete = service
            },
        )
    }

    serviceForGroupAssignment?.let { service ->
        ServiceGroupPickerSheet(
            groups = uiState.groups,
            selectedGroupId = service.groupId,
            onDismiss = { serviceForGroupAssignment = null },
            onSelected = { groupId ->
                onServiceGroupChange(service.id, groupId)
                serviceForGroupAssignment = null
            },
        )
    }

    servicePendingDelete?.let { service ->
        ConfirmDialog(
            onDismissRequest = { servicePendingDelete = null },
            title = TwLocale.strings.commonDelete,
            body = TwLocale.strings.serviceActionsDeleteMessage,
            onPositive = {
                onTrashService(service.id)
                servicePendingDelete = null
            },
        )
    }

    serviceForQr?.let { service ->
        ServiceQrDialog(
            service = service,
            activity = activity,
            onDismiss = { serviceForQr = null },
        )
    }

    if (showQrNoLockDialog) {
        InfoDialog(
            onDismissRequest = { showQrNoLockDialog = false },
            title = stringResource(LocaleR.string.tokens__show_qr_code),
            body = stringResource(LocaleR.string.tokens__show_service_qr_setup_lock),
            positive = stringResource(LocaleR.string.commons__set),
            onNegative = {},
            onPositive = {
                showQrNoLockDialog = false
                listener.openSecurity(activity)
            },
        )
    }

    if (showSortDialog) {
        ListRadioDialog(
            onDismissRequest = { showSortDialog = false },
            title = TwLocale.strings.servicesSortBy,
            options = TwLocale.strings.servicesSortByOptions,
            selectedIndex = when (uiState.appSettings.servicesSort) {
                ServicesSort.Alphabetical -> 0
                ServicesSort.Manual -> 1
            },
            onOptionSelected = { index, _ -> onSortChange(index) },
        )
    }

    if (showQrFromGalleryDialog) {
        ConfirmDialog(
            onDismissRequest = { showQrFromGalleryDialog = false },
            title = TwLocale.strings.servicesQrFromGalleryTitle,
            positive = TwLocale.strings.servicesQrFromGalleryCta,
            negative = null,
            bodyAnnotated = buildAnnotatedString {
                append(TwLocale.strings.servicesQrFromGalleryBody1)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(TwLocale.strings.servicesQrFromGalleryBody2)
                }
                append(TwLocale.strings.servicesQrFromGalleryBody3)
            },
        )
    }
}

@Composable
private fun StratumGroupTransition(
    animationVersion: Int,
    itemIndex: Int,
    content: @Composable () -> Unit,
) {
    val progress = remember(animationVersion) {
        Animatable(if (animationVersion == 0) 1f else 0f)
    }

    LaunchedEffect(progress, itemIndex) {
        if (animationVersion == 0) return@LaunchedEffect

        delay(itemIndex * StratumGroupItemDelayMillis)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = StratumGroupItemDurationMillis,
                easing = StratumDecelerateEasing,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val remaining = 1f - progress.value
                val scale = 1f + (StratumGroupItemStartScaleDelta * remaining)

                alpha = progress.value
                translationY = -size.height * StratumGroupItemStartOffset * remaining
                scaleX = scale
                scaleY = scale
            },
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshedServicesTopBar(
    title: String,
    query: String,
    searching: Boolean,
    focusRequester: FocusRequester,
    onSortClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MdtTheme.color.background),
        title = {
            if (searching) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = MdtIcons.Search,
                        contentDescription = null,
                        tint = MdtTheme.color.iconTint,
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MdtTheme.typo.title.copy(color = MdtTheme.color.onSurfacePrimary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            Box {
                                if (query.isEmpty()) {
                                    Text(
                                        text = TwLocale.strings.commonSearch,
                                        style = MdtTheme.typo.title,
                                        color = MdtTheme.color.onSurfaceTertiary,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MdtTheme.typo.h2,
                    color = MdtTheme.color.onSurfacePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            if (searching) {
                SmoothIconButton(onClick = onSearchClose) {
                    Icon(MdtIcons.Close, TwLocale.strings.commonCancel, tint = MdtTheme.color.iconTint)
                }
            } else {
                SmoothIconButton(onClick = onSortClick) {
                    Icon(MdtIcons.Sort, TwLocale.strings.servicesSortBy, tint = MdtTheme.color.iconTint)
                }
            }
        },
    )
}

@Composable
private fun RefreshedServicesBottomBar(
    showBrowserRequestPull: Boolean,
    browserRequestPulling: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onBrowserRequestPull: () -> Unit,
    onAddClick: () -> Unit,
) {
    Surface(color = MdtTheme.color.background) {
        Column {
            HorizontalDivider(color = MdtTheme.color.divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmoothIconButton(onClick = onMenuClick, modifier = Modifier.size(52.dp)) {
                        Icon(MdtIcons.Menu, TwLocale.strings.homeMenu, tint = MdtTheme.color.iconTint)
                    }
                    SmoothIconButton(onClick = onSearchClick, modifier = Modifier.size(52.dp)) {
                        Icon(MdtIcons.Search, TwLocale.strings.commonSearch, tint = MdtTheme.color.iconTint)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showBrowserRequestPull) {
                        Surface(
                            onClick = { if (browserRequestPulling.not()) onBrowserRequestPull() },
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MdtTheme.color.surfaceVariant,
                            contentColor = MdtTheme.color.primary,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (browserRequestPulling) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MdtTheme.color.primary,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        painter = MdtIcons.BrowserRequestPull,
                                        contentDescription = TwLocale.strings.browserPullRequest,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        onClick = onAddClick,
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MdtTheme.color.primary,
                        contentColor = Color.White,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(MdtIcons.Add, TwLocale.strings.commonAdd, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmoothIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressedOverlay by animateColorAsState(
        targetValue = if (pressed) MdtTheme.color.onSurfacePrimary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(durationMillis = if (pressed) 80 else 220),
        label = "refreshedIconPress",
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(pressedOverlay)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RefreshedServiceCard(
    state: ServiceState,
    style: ServicesStyle,
    showNextCode: Boolean,
    hideCodes: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onMoreClick: () -> Unit,
    onIncrementCounterClick: () -> Unit,
    onRevealClick: () -> Unit,
) {
    val compact = style == ServicesStyle.Compact
    val tokenFontSize = if (compact) 30 else 36
    val codeColor by animateExpireColor(timer = state.timer)
    val codeVisible = state.revealed || hideCodes.not()
    val cardHeight = if (compact) 92.dp else 104.dp
    val progress = remember { Animatable(state.progress.coerceIn(0f, 1f)) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.012f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "refreshedServiceDragScale",
    )
    val dragElevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 0.dp,
        animationSpec = tween(durationMillis = 120),
        label = "refreshedServiceDragElevation",
    )
    val dragBorderColor by animateColorAsState(
        targetValue = if (isDragging) MdtTheme.color.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 120),
        label = "refreshedServiceDragBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (pressed || isDragging) MdtTheme.color.surfaceVariant else MdtTheme.color.surface,
        animationSpec = tween(durationMillis = if (pressed) 80 else 220),
        label = "refreshedServicePress",
    )

    LaunchedEffect(state.progress) {
        val target = state.progress.coerceIn(0f, 1f)
        if (target > progress.value) {
            progress.snapTo(target)
        } else {
            progress.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .graphicsLayer {
                scaleX = dragScale
                scaleY = dragScale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { if (codeVisible) onClick() else onRevealClick() },
                onLongClick = onLongClick,
            ),
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, dragBorderColor),
        shadowElevation = dragElevation,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RefreshedServiceImage(state = state, compact = compact)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.name,
                        style = if (compact) {
                            MdtTheme.typo.body2.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MdtTheme.typo.body1.copy(fontWeight = FontWeight.SemiBold)
                        },
                        color = MdtTheme.color.onSurfacePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    state.info?.takeIf { it.isNotBlank() }?.let { info ->
                        Text(
                            text = info,
                            style = MdtTheme.typo.body3,
                            color = MdtTheme.color.onSurfaceSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (codeVisible) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = state.code.formatCode(),
                                    style = MdtTheme.typo.codeLightSmall.copy(
                                        fontSize = tokenFontSize.sp,
                                        lineHeight = (tokenFontSize + if (compact) 8 else 4).sp,
                                    ),
                                    color = if (state.authType == ServiceAuthType.Hotp) {
                                        MdtTheme.color.onSurfacePrimary
                                    } else {
                                        codeColor
                                    },
                                    maxLines = 1,
                                )
                                if (state.isNextCodeEnabled(showNextCode)) {
                                    Text(
                                        text = state.nextCode.formatCode(),
                                        style = MdtTheme.typo.caption.copy(fontSize = 15.sp, lineHeight = 20.sp),
                                        color = MdtTheme.color.onSurfaceSecondary,
                                        maxLines = 1,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                    )
                                }
                            }
                        } else {
                            HiddenCodeDots(
                                code = state.code.formatCode(),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        if (state.authType == ServiceAuthType.Hotp) {
                            IconButton(
                                onClick = onIncrementCounterClick,
                                enabled = state.hotpCounterEnabled,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(MdtIcons.IncrementHotp, null, tint = MdtTheme.color.primary)
                            }
                        }
                    }
                }

                IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                    Icon(MdtIcons.More, TwLocale.strings.serviceActionsTitle, tint = MdtTheme.color.iconTint)
                }
            }

            if (state.authType == ServiceAuthType.Hotp) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MdtTheme.color.surfaceVariant),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MdtTheme.color.surfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.value)
                            .fillMaxHeight()
                            .background(MdtTheme.color.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun RefreshedServiceImage(state: ServiceState, compact: Boolean) {
    val size = if (compact) 38.dp else 44.dp
    when (state.imageType) {
        ServiceImageType.Icon -> {
            Image(
                bitmap = assetAsBitmap(if (MdtTheme.isDark) state.iconDark else state.iconLight),
                contentDescription = null,
                modifier = Modifier.size(size),
            )
        }

        ServiceImageType.Label -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(state.labelColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.labelText.orEmpty(),
                    style = MdtTheme.typo.h2,
                    color = MdtTheme.color.onSurfacePrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HiddenCodeDots(code: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        code.forEach { char ->
            if (char.isWhitespace()) {
                Spacer(Modifier.width(4.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(MdtTheme.color.onSurfacePrimary, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun ServiceGroupPickerRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = title, color = MdtTheme.color.onSurfacePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MdtTheme.color.primaryIndicator else MdtTheme.color.surface,
        ),
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceGroupPickerSheet(
    groups: List<Group>,
    selectedGroupId: String?,
    onDismiss: () -> Unit,
    onSelected: (String?) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MdtTheme.color.surface,
    ) {
        Text(
            text = TwLocale.strings.serviceActionsAssignGroup,
            style = MdtTheme.typo.h2,
            color = MdtTheme.color.onSurfacePrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        ServiceGroupPickerRow(
            title = TwLocale.strings.groupsDefault,
            selected = selectedGroupId == null,
            onClick = { onSelected(null) },
        )
        groups.filter { it.id != null }.forEach { group ->
            ServiceGroupPickerRow(
                title = group.name.orEmpty(),
                selected = selectedGroupId == group.id,
                onClick = { onSelected(group.id) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceActionsSheet(
    onDismiss: () -> Unit,
    onEditDetails: () -> Unit,
    onChangeIcon: () -> Unit,
    onAssignGroup: () -> Unit,
    onShowQr: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MdtTheme.color.surface,
    ) {
        Text(
            text = TwLocale.strings.serviceActionsTitle,
            style = MdtTheme.typo.h2,
            color = MdtTheme.color.onSurfacePrimary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp),
        )
        SettingsLink(TwLocale.strings.serviceActionsEditDetails, icon = MdtIcons.Edit, onClick = onEditDetails)
        SettingsLink(TwLocale.strings.serviceActionsChangeIcon, icon = MdtIcons.Change, onClick = onChangeIcon)
        SettingsLink(TwLocale.strings.serviceActionsAssignGroup, icon = MdtIcons.AddGroup, onClick = onAssignGroup)
        SettingsLink(TwLocale.strings.serviceActionsShowQr, icon = MdtIcons.Qr, onClick = onShowQr)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MdtTheme.color.divider)
        SettingsLink(
            title = TwLocale.strings.commonDelete,
            icon = MdtIcons.Delete,
            iconTint = MdtTheme.color.error,
            textColor = MdtTheme.color.error,
            onClick = onDelete,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ServiceQrDialog(
    service: Service,
    activity: Activity,
    onDismiss: () -> Unit,
) {
    BaseDialog(
        onDismissRequest = onDismiss,
        title = stringResource(LocaleR.string.tokens__show_qr_code),
        positive = stringResource(LocaleR.string.commons__OK),
        negative = stringResource(LocaleR.string.tokens__copy_uri),
        onNegativeClick = {
            activity.copyToClipboard(service.toUri(), isSensitive = true)
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = QrGenerator.generateBitmap(service.toUri()).asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeMenuSheet(
    groups: List<Group>,
    selectedGroup: SelectedGroup,
    onDismiss: () -> Unit,
    onGroupSelected: (SelectedGroup) -> Unit,
    onManageGroups: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val dismiss: () -> Unit = {
        scope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = HomeMenuDismissDurationMillis,
                    easing = StratumDecelerateEasing,
                ),
            )
            onDismiss()
        }
    }

    val actionThenDismiss: (() -> Unit) -> Unit = { action ->
        action()
        dismiss()
    }

    val openOverMenu: (() -> Unit) -> Unit = { action ->
        action()
        scope.launch {
            delay(HomePageTransitionDurationMillis.toLong())
            onDismiss()
        }
    }

    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = HomeMenuOpenDurationMillis,
                easing = StratumDecelerateEasing,
            ),
        )
    }

    BackHandler(onBack = dismiss)

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = HomeMenuScrimAlpha * progress.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = dismiss,
                ),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = size.height * (1f - progress.value)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            color = MdtTheme.color.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight * 0.92f)
                        .navigationBarsPadding(),
                ) {
                    Text(
                        text = TwLocale.strings.homeMainMenu,
                        style = MdtTheme.typo.h2,
                        color = MdtTheme.color.onSurfacePrimary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 20.dp, bottom = 16.dp),
                    )
                    Text(
                        text = TwLocale.strings.groupsTitle,
                        style = MdtTheme.typo.body2,
                        color = MdtTheme.color.onSurfaceSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                    ) {
                        item(key = "all") {
                            HomeMenuGroupRow(
                                title = TwLocale.strings.groupsAll,
                                selected = selectedGroup == SelectedGroup.All,
                                onClick = { actionThenDismiss { onGroupSelected(SelectedGroup.All) } },
                            )
                        }
                        item(key = "default") {
                            HomeMenuGroupRow(
                                title = TwLocale.strings.groupsDefault,
                                selected = selectedGroup == SelectedGroup.Default,
                                onClick = { actionThenDismiss { onGroupSelected(SelectedGroup.Default) } },
                            )
                        }
                        items(
                            items = groups.filter { it.id != null },
                            key = { it.id.orEmpty() },
                        ) { group ->
                            HomeMenuGroupRow(
                                title = group.name.orEmpty(),
                                selected = selectedGroup is SelectedGroup.Custom && selectedGroup.id == group.id,
                                onClick = {
                                    group.id?.let { id ->
                                        actionThenDismiss { onGroupSelected(SelectedGroup.Custom(id)) }
                                    }
                                },
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MdtTheme.color.divider,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = TwLocale.strings.homeMore,
                        style = MdtTheme.typo.body2,
                        color = MdtTheme.color.onSurfaceSecondary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                    SettingsLink(
                        title = TwLocale.strings.groupsTitle,
                        icon = MdtIcons.AddGroup,
                        onClick = { openOverMenu(onManageGroups) },
                    )
                    SettingsLink(
                        title = TwLocale.strings.settingsSettings,
                        icon = MdtIcons.Settings,
                        onClick = { openOverMenu(onSettingsClick) },
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeMenuGroupRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = MdtTheme.color.onSurfacePrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) MdtTheme.color.primaryIndicator else MdtTheme.color.surface,
        ),
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    )
}

private fun String.toSelectedGroup(): SelectedGroup {
    return when (this) {
        GroupAllKey -> SelectedGroup.All
        GroupDefaultKey -> SelectedGroup.Default
        else -> SelectedGroup.Custom(this)
    }
}

private fun SelectedGroup.key(): String {
    return when (this) {
        SelectedGroup.All -> GroupAllKey
        SelectedGroup.Default -> GroupDefaultKey
        is SelectedGroup.Custom -> id
    }
}

@Composable
private fun SelectedGroup.title(groups: List<Group>): String {
    return when (this) {
        SelectedGroup.All -> TwLocale.strings.groupsAll
        SelectedGroup.Default -> TwLocale.strings.groupsDefault
        is SelectedGroup.Custom -> groups.firstOrNull { it.id == id }?.name ?: TwLocale.strings.groupsDefault
    }
}

private const val GroupAllKey = "__all__"
private const val GroupDefaultKey = "__default__"
private const val HomePageTransitionDurationMillis = 450
private const val HomeMenuOpenDurationMillis = 300
private const val HomeMenuDismissDurationMillis = 250
private const val HomeMenuScrimAlpha = 0.32f
private const val StratumGroupItemDurationMillis = 500
private const val StratumGroupItemDelayMillis = 25L
private const val StratumGroupItemStartOffset = 0.2f
private const val StratumGroupItemStartScaleDelta = 0.05f
private val StratumDecelerateEasing = Easing { progress ->
    1f - (1f - progress) * (1f - progress)
}
