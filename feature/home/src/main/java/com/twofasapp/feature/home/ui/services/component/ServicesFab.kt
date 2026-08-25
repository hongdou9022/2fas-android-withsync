package com.twofasapp.feature.home.ui.services.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.twofasapp.core.design.MdtIcons
import com.twofasapp.core.design.MdtTheme
import com.twofasapp.core.design.foundation.progress.CircularProgressIndicator
import com.twofasapp.locale.TwLocale

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun ServicesFab(
    isVisible: Boolean,
    isExtendedVisible: Boolean,
    isNormalVisible: Boolean,
    showBrowserRequestPull: Boolean,
    browserRequestPulling: Boolean,
    onBrowserRequestPull: () -> Unit,
    onClick: () -> Unit,
) {
    if (isVisible) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedVisibility(
                visible = showBrowserRequestPull && (isExtendedVisible || isNormalVisible || browserRequestPulling),
                enter = scaleIn(tween(150)),
                exit = scaleOut(tween(150)),
            ) {
                FloatingActionButton(
                    onClick = { if (browserRequestPulling.not()) onBrowserRequestPull() },
                    containerColor = MdtTheme.color.surfaceVariant,
                    contentColor = MdtTheme.color.primary,
                    content = {
                        if (browserRequestPulling) {
                            CircularProgressIndicator(color = MdtTheme.color.primary)
                        } else {
                            Icon(
                                painter = MdtIcons.Refresh,
                                contentDescription = TwLocale.strings.browserPullRequest,
                            )
                        }
                    },
                )
            }

            if (isExtendedVisible) {
                ExtendedFloatingActionButton(
                    onClick = onClick,
                    icon = { Icon(MdtIcons.Add, null) },
                    text = { Text(text = TwLocale.strings.servicesEmptyPairServiceCta) },
                    containerColor = MdtTheme.color.primary,
                    contentColor = Color.White,
                )
            } else {
                AnimatedVisibility(
                    visible = isNormalVisible,
                    enter = scaleIn(tween(150)),
                    exit = scaleOut(tween(150)),
                ) {
                    FloatingActionButton(
                        onClick = onClick,
                        content = { Icon(MdtIcons.Add, null) },
                        containerColor = MdtTheme.color.primary,
                        contentColor = Color.White,
                    )
                }
            }
        }
    }
}
