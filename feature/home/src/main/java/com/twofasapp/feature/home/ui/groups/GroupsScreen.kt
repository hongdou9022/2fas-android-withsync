package com.twofasapp.feature.home.ui.groups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twofasapp.common.domain.Service
import com.twofasapp.core.design.MdtIcons
import com.twofasapp.core.design.MdtTheme
import com.twofasapp.core.design.feature.items.ServiceImageType
import com.twofasapp.core.design.feature.items.asState
import com.twofasapp.core.design.feature.settings.SettingsLink
import com.twofasapp.core.design.foundation.dialog.ConfirmDialog
import com.twofasapp.core.design.foundation.dialog.InputDialog
import com.twofasapp.core.design.ktx.assetAsBitmap
import com.twofasapp.data.services.domain.Group
import com.twofasapp.locale.TwLocale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.cos

@Composable
internal fun GroupsRoute(
    onBack: () -> Unit,
    onAssignEntries: (String) -> Unit,
    viewModel: GroupsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupsScreen(
        uiState = uiState,
        onBack = onBack,
        onAddGroup = viewModel::addGroup,
        onChangeName = viewModel::changeGroupName,
        onDeleteGroup = viewModel::deleteGroup,
        onAssignEntries = onAssignEntries,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupsScreen(
    uiState: GroupsUiState,
    onBack: () -> Unit,
    onAddGroup: (String) -> Unit,
    onChangeName: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onAssignEntries: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var groupPendingRename by remember { mutableStateOf<Group?>(null) }
    var groupPendingDelete by remember { mutableStateOf<Group?>(null) }
    var animateInitialRows by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(uiState.isLoading, uiState.groups.size, animateInitialRows) {
        if (uiState.isLoading.not() && animateInitialRows) {
            val lastItemDelay = (uiState.groups.lastIndex.coerceAtLeast(0) * StratumListItemDelayMillis)
            delay(lastItemDelay + StratumListItemDurationMillis)
            animateInitialRows = false
        }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = MdtTheme.color.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MdtTheme.color.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MdtIcons.ArrowBack, TwLocale.strings.commonBack, tint = MdtTheme.color.iconTint)
                    }
                },
                title = {
                    Text(
                        text = TwLocale.strings.groupsTitle,
                        style = MdtTheme.typo.h2,
                        color = MdtTheme.color.onSurfacePrimary,
                    )
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(MdtIcons.Add, TwLocale.strings.groupsAddRefreshed, tint = MdtTheme.color.primary)
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MdtTheme.color.primary)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = uiState.groups,
                        key = { _, group -> group.id ?: "default" },
                    ) { index, group ->
                        if (animateInitialRows) {
                            StratumListFadeTransition(itemIndex = index) {
                                GroupRow(
                                    group = group,
                                    serviceCount = uiState.services.count { it.groupId == group.id },
                                    onMoreClick = if (group.id == null) null else ({ selectedGroup = group }),
                                )
                            }
                        } else {
                            GroupRow(
                                group = group,
                                serviceCount = uiState.services.count { it.groupId == group.id },
                                onMoreClick = if (group.id == null) null else ({ selectedGroup = group }),
                            )
                        }
                    }
                }
            }
        }
    }

    selectedGroup?.let { group ->
        GroupActionsSheet(
            group = group,
            onDismiss = { selectedGroup = null },
            onChangeName = {
                groupPendingRename = group
            },
            onAssignEntries = {
                group.id?.let(onAssignEntries)
            },
            onDelete = {
                groupPendingDelete = group
            },
        )
    }

    if (showAddDialog) {
        InputDialog(
            title = TwLocale.strings.groupsAddRefreshed,
            onDismissRequest = { showAddDialog = false },
            positive = TwLocale.strings.commonAdd,
            negative = TwLocale.strings.commonCancel,
            hint = TwLocale.strings.groupsName,
            showCounter = true,
            minLength = 1,
            maxLength = 32,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
            ),
            onPositiveClick = { name ->
                onAddGroup(name)
                showAddDialog = false
            },
        )
    }

    groupPendingRename?.let { group ->
        InputDialog(
            title = TwLocale.strings.groupsChangeName,
            onDismissRequest = { groupPendingRename = null },
            positive = TwLocale.strings.commonSave,
            negative = TwLocale.strings.commonCancel,
            hint = TwLocale.strings.groupsName,
            prefill = group.name.orEmpty(),
            showCounter = true,
            minLength = 1,
            maxLength = 32,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
            ),
            onPositiveClick = { name ->
                group.id?.let { onChangeName(it, name) }
                groupPendingRename = null
            },
        )
    }

    groupPendingDelete?.let { group ->
        ConfirmDialog(
            onDismissRequest = { groupPendingDelete = null },
            title = TwLocale.strings.commonDelete,
            body = TwLocale.strings.groupsDelete,
            onPositive = {
                group.id?.let(onDeleteGroup)
                groupPendingDelete = null
            },
        )
    }
}

@Composable
private fun StratumListFadeTransition(
    itemIndex: Int,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(StratumListItemInitialAlpha) }

    LaunchedEffect(alpha, itemIndex) {
        delay(itemIndex * StratumListItemDelayMillis)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = StratumListItemDurationMillis.toInt(),
                easing = StratumAccelerateDecelerateEasing,
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha.value },
    ) {
        content()
    }
}

@Composable
private fun GroupRow(
    group: Group,
    serviceCount: Int,
    onMoreClick: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        color = MdtTheme.color.surface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name ?: TwLocale.strings.groupsDefault,
                    style = MdtTheme.typo.title,
                    color = MdtTheme.color.onSurfacePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = serviceCount.toString(),
                    style = MdtTheme.typo.caption,
                    color = MdtTheme.color.onSurfaceSecondary,
                )
            }
            onMoreClick?.let {
                IconButton(onClick = it) {
                    Icon(MdtIcons.More, TwLocale.strings.homeMore, tint = MdtTheme.color.iconTint)
                }
            }
        }
    }
}

private const val StratumListItemDurationMillis = 50L
private const val StratumListItemDelayMillis = 5L
private const val StratumListItemInitialAlpha = 0.5f
private val StratumAccelerateDecelerateEasing = Easing { progress ->
    0.5f - (cos(progress * Math.PI).toFloat() / 2f)
}

private const val GroupPageTransitionDurationMillis = 450
private const val GroupActionsOpenDurationMillis = 300
private const val GroupActionsDismissDurationMillis = 250
private const val GroupActionsScrimAlpha = 0.32f
private val GroupActionsDecelerateEasing = Easing { progress ->
    1f - (1f - progress) * (1f - progress)
}

@Composable
private fun GroupActionsSheet(
    group: Group,
    onDismiss: () -> Unit,
    onChangeName: () -> Unit,
    onAssignEntries: () -> Unit,
    onDelete: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val dismiss: () -> Unit = {
        scope.launch {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = GroupActionsDismissDurationMillis,
                    easing = GroupActionsDecelerateEasing,
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
            delay(GroupPageTransitionDurationMillis.toLong())
            onDismiss()
        }
    }

    LaunchedEffect(progress) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = GroupActionsOpenDurationMillis,
                easing = GroupActionsDecelerateEasing,
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
                .background(Color.Black.copy(alpha = GroupActionsScrimAlpha * progress.value))
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = group.name.orEmpty(),
                    style = MdtTheme.typo.h2,
                    color = MdtTheme.color.onSurfacePrimary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 20.dp, bottom = 12.dp),
                )
                SettingsLink(
                    title = TwLocale.strings.groupsChangeName,
                    icon = MdtIcons.Edit,
                    onClick = { actionThenDismiss(onChangeName) },
                )
                SettingsLink(
                    title = TwLocale.strings.groupsAssignEntries,
                    icon = MdtIcons.CheckCircle,
                    onClick = { openOverMenu(onAssignEntries) },
                )
                SettingsLink(
                    title = TwLocale.strings.commonDelete,
                    icon = MdtIcons.Delete,
                    iconTint = MdtTheme.color.error,
                    textColor = MdtTheme.color.error,
                    onClick = { actionThenDismiss(onDelete) },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
internal fun GroupEntriesRoute(
    groupId: String,
    onBack: () -> Unit,
    viewModel: GroupsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupEntriesScreen(
        groupId = groupId,
        uiState = uiState,
        onBack = onBack,
        onConfirm = { selectedIds ->
            viewModel.assignGroupEntries(
                groupId = groupId,
                selectedServiceIds = selectedIds,
                onSaved = onBack,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupEntriesScreen(
    groupId: String,
    uiState: GroupsUiState,
    onBack: () -> Unit,
    onConfirm: (Set<Long>) -> Unit,
) {
    var selectedIds by remember(groupId) { mutableStateOf(emptySet<Long>()) }
    var selectionInitialized by remember(groupId) { mutableStateOf(false) }
    val group = uiState.groups.firstOrNull { it.id == groupId }
    val duplicateNames = remember(uiState.services) {
        uiState.services
            .groupBy { it.name.trim().lowercase() }
            .filterValues { it.size > 1 }
            .keys
    }

    LaunchedEffect(uiState.isLoading, groupId) {
        if (uiState.isLoading.not() && selectionInitialized.not()) {
            selectedIds = uiState.services.filter { it.groupId == groupId }.map { it.id }.toSet()
            selectionInitialized = true
        }
    }

    BackHandler(enabled = uiState.isSavingEntries.not(), onBack = onBack)

    Scaffold(
        containerColor = MdtTheme.color.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MdtTheme.color.background),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = uiState.isSavingEntries.not(),
                    ) {
                        Icon(MdtIcons.ArrowBack, TwLocale.strings.commonBack, tint = MdtTheme.color.iconTint)
                    }
                },
                title = {
                    Text(
                        text = TwLocale.strings.groupsAssignEntries,
                        style = MdtTheme.typo.h2,
                        color = MdtTheme.color.onSurfacePrimary,
                    )
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { onConfirm(selectedIds) },
                enabled = selectionInitialized && uiState.isSavingEntries.not(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                if (uiState.isSavingEntries) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(TwLocale.strings.commonOk, fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { padding ->
        when {
            uiState.isLoading || group == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MdtTheme.color.primary)
                }
            }

            else -> {
                FlowRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.services.forEach { service ->
                        val selected = service.id in selectedIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedIds = if (selected) selectedIds - service.id else selectedIds + service.id
                            },
                            label = {
                                Text(
                                    text = service.displayNameForAssignment(
                                        includeInfo = service.name.trim().lowercase() in duplicateNames,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = { AssignmentServiceIcon(service) },
                            modifier = Modifier.widthIn(max = 320.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MdtTheme.color.surface,
                                selectedContainerColor = MdtTheme.color.primaryIndicator,
                                labelColor = MdtTheme.color.onSurfacePrimary,
                                selectedLabelColor = MdtTheme.color.onSurfacePrimary,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentServiceIcon(service: Service) {
    val state = service.asState()
    when (state.imageType) {
        ServiceImageType.Icon -> {
            Image(
                bitmap = assetAsBitmap(if (MdtTheme.isDark) state.iconDark else state.iconLight),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }

        ServiceImageType.Label -> {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(state.labelColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.labelText.orEmpty(),
                    style = MdtTheme.typo.caption,
                    color = MdtTheme.color.onSurfacePrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun Service.displayNameForAssignment(includeInfo: Boolean): String {
    if (includeInfo.not()) return name
    return info?.takeIf { it.isNotBlank() }?.let { "$name ($it)" } ?: name
}
