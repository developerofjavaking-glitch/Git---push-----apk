package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HelpScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PushScreen
import com.example.ui.screens.ReposScreen

enum class AppTab(val label: String, val testTag: String) {
    PUSH("Push", "tab_push"),
    REPOS("Repos", "tab_repos"),
    HISTORY("History", "tab_history"),
    HELP("Help", "tab_help")
}

@Composable
fun MainAppScreen(
    viewModel: GitPusherViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(AppTab.PUSH) }

    // If on a sub-tab, hardware back press returns to Push tab
    BackHandler(enabled = selectedTab != AppTab.PUSH) {
        selectedTab = AppTab.PUSH
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.PUSH,
                    onClick = { selectedTab = AppTab.PUSH },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Push") },
                    label = { Text("Push") },
                    modifier = Modifier.testTag(AppTab.PUSH.testTag)
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.REPOS,
                    onClick = { selectedTab = AppTab.REPOS },
                    icon = { Icon(Icons.Default.Source, contentDescription = "Repositories") },
                    label = { Text("Repos") },
                    modifier = Modifier.testTag(AppTab.REPOS.testTag)
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.HISTORY,
                    onClick = { selectedTab = AppTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    modifier = Modifier.testTag(AppTab.HISTORY.testTag)
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.HELP,
                    onClick = { selectedTab = AppTab.HELP },
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Help") },
                    label = { Text("Guide") },
                    modifier = Modifier.testTag(AppTab.HELP.testTag)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.PUSH -> {
                    PushScreen(
                        viewModel = viewModel,
                        onNavigateToRepos = { selectedTab = AppTab.REPOS },
                        onNavigateToHelp = { selectedTab = AppTab.HELP }
                    )
                }
                AppTab.REPOS -> {
                    ReposScreen(
                        viewModel = viewModel,
                        onSelectRepoForPush = { selectedTab = AppTab.PUSH }
                    )
                }
                AppTab.HISTORY -> {
                    HistoryScreen(viewModel = viewModel)
                }
                AppTab.HELP -> {
                    HelpScreen()
                }
            }
        }
    }
}
