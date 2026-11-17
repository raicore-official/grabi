package com.raisoft.grabi.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raisoft.grabi.R
import com.raisoft.grabi.domain.LinkValidator
import kotlinx.coroutines.launch

private enum class LinkState { UNCHECKED, VALID, INVALID }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen() {
    var link by rememberSaveable { mutableStateOf("") }
    var linkState by rememberSaveable { mutableStateOf(LinkState.UNCHECKED) }
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val validLinkMessage = stringResource(R.string.home_link_valid)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_paste_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = link,
                        onValueChange = {
                            link = it
                            linkState = LinkState.UNCHECKED
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.home_link_label)) },
                        placeholder = { Text(stringResource(R.string.home_link_placeholder)) },
                        singleLine = true,
                        isError = linkState == LinkState.INVALID,
                        supportingText = if (linkState == LinkState.INVALID) {
                            { Text(stringResource(R.string.home_link_invalid)) }
                        } else {
                            null
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    clipboard.getText()?.text?.let { pasted ->
                                        link = pasted
                                        linkState = LinkState.UNCHECKED
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = stringResource(R.string.home_paste_action),
                                )
                            }
                        },
                    )
                    Button(
                        onClick = {
                            val candidate = LinkValidator.extractUrl(link) ?: link.trim()
                            if (LinkValidator.isValidUrl(candidate)) {
                                linkState = LinkState.VALID
                                scope.launch { snackbarHostState.showSnackbar(validLinkMessage) }
                            } else {
                                linkState = LinkState.INVALID
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = link.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.home_fetch_button))
                    }
                }
            }

            Text(
                text = stringResource(R.string.home_supported_title),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("YouTube", "Facebook", "Instagram", "TikTok", "X", "Vimeo", "SoundCloud", "+1000 more")
                    .forEach { site ->
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                text = site,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_how_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.home_step_1), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.home_step_2), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.home_step_3), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}