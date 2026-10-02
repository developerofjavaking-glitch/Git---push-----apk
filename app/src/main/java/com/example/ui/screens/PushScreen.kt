package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.AuthState
import com.example.ui.GitPusherViewModel
import com.example.ui.PushUiState
import com.example.ui.components.CreateRepoDialog
import com.example.ui.components.FileListPreviewDialog
import com.example.ui.components.RepoSelectorDialog
import com.example.ui.theme.GitGreen
import com.example.ui.theme.GitGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushScreen(
    viewModel: GitPusherViewModel,
    onNavigateToRepos: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val userRepos by viewModel.userRepos.collectAsStateWithLifecycle()
    val isLoadingRepos by viewModel.isLoadingRepos.collectAsStateWithLifecycle()

    val tokenText by viewModel.tokenInput.collectAsStateWithLifecycle()
    val ownerText by viewModel.ownerInput.collectAsStateWithLifecycle()
    val repoNameText by viewModel.repoNameInput.collectAsStateWithLifecycle()
    val branchText by viewModel.branchInput.collectAsStateWithLifecycle()
    val commitMsgText by viewModel.commitMessageInput.collectAsStateWithLifecycle()
    val targetSubfolderText by viewModel.targetSubfolderInput.collectAsStateWithLifecycle()
    val ignoreBuildDirs by viewModel.ignoreBuildDirs.collectAsStateWithLifecycle()

    val selectedFolderName by viewModel.selectedFolderName.collectAsStateWithLifecycle()
    val scannedFiles by viewModel.scannedFiles.collectAsStateWithLifecycle()
    val isScanningFiles by viewModel.isScanningFiles.collectAsStateWithLifecycle()
    val pushUiState by viewModel.pushUiState.collectAsStateWithLifecycle()

    var showTokenPassword by remember { mutableStateOf(false) }
    var showRepoSelectorDialog by remember { mutableStateOf(false) }
    var showCreateRepoDialog by remember { mutableStateOf(false) }
    var showFileListDialog by remember { mutableStateOf(false) }

    // Launcher for selecting a directory tree from Android storage
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not supported
            }
            val folderName = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')
                ?: "Selected_Folder"
            viewModel.onFolderSelected(uri, folderName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GitPusher",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Push local folder to GitHub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHelp) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Token Guide & Help",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // CARD 1: GitHub Access Token (Authentication)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1. GitHub Access Token",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        if (authState is AuthState.Authenticated) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GitGreen.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GitGreenLight)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Connected",
                                        fontSize = 11.sp,
                                        color = GitGreenLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (val state = authState) {
                        is AuthState.Authenticated -> {
                            val user = state.user
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (user.avatarUrl != null) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = "User Avatar",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = user.login.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name ?: user.login,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "@${user.login}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${user.publicRepos} public repos",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.logout() },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Logout", fontSize = 12.sp)
                                }
                            }
                        }

                        else -> {
                            Text(
                                text = "Enter your GitHub Personal Access Token (classic or fine-grained with 'repo' scope):",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = tokenText,
                                onValueChange = { viewModel.tokenInput.value = it },
                                label = { Text("GitHub Token (ghp_... or github_pat_...)") },
                                placeholder = { Text("Paste your token here") },
                                singleLine = true,
                                visualTransformation = if (showTokenPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showTokenPassword = !showTokenPassword }) {
                                        Icon(
                                            if (showTokenPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showTokenPassword) "Hide token" else "Show token"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("token_input"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (state is AuthState.Error) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/settings/tokens/new?scopes=repo&description=GitPusher-Android")
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Get Token", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { viewModel.verifyToken(tokenText) },
                                    enabled = tokenText.isNotBlank() && state !is AuthState.Loading,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("verify_token_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (state is AuthState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text("Verify & Save", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // CARD 2: Target Repository & Branch
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Source,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "2. Target Repository",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        TextButton(
                            onClick = { showRepoSelectorDialog = true },
                            modifier = Modifier.testTag("select_from_repos_button")
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Browse Repos", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ownerText,
                            onValueChange = { viewModel.ownerInput.value = it },
                            label = { Text("Username / Owner *") },
                            placeholder = { Text("e.g. octocat") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = repoNameText,
                            onValueChange = { viewModel.repoNameInput.value = it },
                            label = { Text("Repository Name *") },
                            placeholder = { Text("e.g. my-app") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("repo_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = branchText,
                        onValueChange = { viewModel.branchInput.value = it },
                        label = { Text("Target Branch") },
                        placeholder = { Text("main") },
                        leadingIcon = {
                            Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("branch_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // CARD 3: Phone Folder Selection (Storage Access Framework)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "3. Select Folder from Phone",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Text(
                        text = "ফোল্ডার নির্বাচন করলেই ভেতরের সব ফাইল ও সাব-ফোল্ডার স্ক্যান হয়ে যাবে:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big Folder picker action box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                folderPickerLauncher.launch(null)
                            }
                            .border(
                                width = 1.dp,
                                color = if (selectedFolderName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .testTag("select_folder_button"),
                        color = if (selectedFolderName != null) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (selectedFolderName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (selectedFolderName != null) Icons.Default.FolderOpen else Icons.Default.CreateNewFolder,
                                        contentDescription = null,
                                        tint = if (selectedFolderName != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                if (selectedFolderName != null) {
                                    Text(
                                        text = selectedFolderName ?: "",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isScanningFiles) {
                                        Text(
                                            text = "Scanning files...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        val totalSizeMB = scannedFiles.sumOf { it.sizeBytes } / (1024.0 * 1024.0)
                                        Text(
                                            text = "${scannedFiles.size} files ready (${String.format("%.2f MB", totalSizeMB)})",
                                            fontSize = 12.sp,
                                            color = GitGreenLight,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Tap to Choose Folder",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Internal Storage, Downloads, or SD Card",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = "Choose",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (scannedFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showFileListDialog = true },
                                modifier = Modifier.testTag("preview_files_button")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview ${scannedFiles.size} files", fontSize = 13.sp)
                            }

                            TextButton(
                                onClick = { viewModel.scanFolder() }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rescan", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = ignoreBuildDirs,
                            onCheckedChange = {
                                viewModel.ignoreBuildDirs.value = it
                                viewModel.scanFolder()
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ignore build/cache (.git, node_modules, build)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Prevents pushing unnecessary gigantic build folders",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // CARD 4: Commit Message & Destination Path
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Commit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "4. Commit Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = commitMsgText,
                        onValueChange = { viewModel.commitMessageInput.value = it },
                        label = { Text("Commit Message") },
                        placeholder = { Text("e.g. Upload project folder") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("commit_message_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = targetSubfolderText,
                        onValueChange = { viewModel.targetSubfolderInput.value = it },
                        label = { Text("Repo Subfolder (Optional)") },
                        placeholder = { Text("Leave blank for repo root, or 'my-folder'") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_subfolder_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // BIG ACTION BUTTON: PUSH TO GITHUB
            val isReadyToPush = ownerText.isNotBlank() &&
                    repoNameText.isNotBlank() &&
                    scannedFiles.isNotEmpty() &&
                    pushUiState !is PushUiState.Pushing

            Button(
                onClick = { viewModel.pushFiles() },
                enabled = isReadyToPush,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("push_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (scannedFiles.isNotEmpty()) "Push ${scannedFiles.size} Files to GitHub" else "Push to GitHub",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // MODAL DIALOGS
    if (showRepoSelectorDialog) {
        RepoSelectorDialog(
            repos = userRepos,
            isLoading = isLoadingRepos,
            onSelectRepo = { repo ->
                viewModel.selectRepo(repo)
            },
            onCreateNewRepoClick = {
                showCreateRepoDialog = true
            },
            onDismiss = { showRepoSelectorDialog = false }
        )
    }

    if (showCreateRepoDialog) {
        val isCreating by viewModel.isCreatingRepo.collectAsStateWithLifecycle()
        CreateRepoDialog(
            isCreating = isCreating,
            onCreate = { name, desc, isPriv, autoInit ->
                viewModel.createRepository(name, desc, isPriv, autoInit) {
                    showCreateRepoDialog = false
                }
            },
            onDismiss = { showCreateRepoDialog = false }
        )
    }

    if (showFileListDialog) {
        FileListPreviewDialog(
            files = scannedFiles,
            folderName = selectedFolderName ?: "Folder",
            onDismiss = { showFileListDialog = false }
        )
    }

    // PUSH IN-PROGRESS DIALOG
    if (pushUiState is PushUiState.Pushing) {
        val state = pushUiState as PushUiState.Pushing
        Dialog(onDismissRequest = { /* Cannot dismiss while uploading */ }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        progress = { state.progressPercent.coerceIn(0.05f, 1f) },
                        modifier = Modifier.size(64.dp),
                        strokeWidth = 6.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Pushing to GitHub...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = state.message,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    if (state.currentFile.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.currentFile,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { state.progressPercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${(state.progressPercent * 100).toInt()}% completed",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // PUSH SUCCESS DIALOG
    if (pushUiState is PushUiState.Success) {
        val state = pushUiState as PushUiState.Success
        Dialog(onDismissRequest = { viewModel.dismissPushResult() }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GitGreen.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = GitGreenLight,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Successfully Pushed!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "সব ফাইল গিটহাবে সফলভাবে আপলোড হয়েছে",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Uploaded Files:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${state.fileCount} files", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Commit SHA:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = state.commitSha.take(7),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (state.commitUrl != null) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.commitUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Commit on GitHub")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedButton(
                        onClick = { viewModel.dismissPushResult() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    // PUSH ERROR DIALOG
    if (pushUiState is PushUiState.Error) {
        val state = pushUiState as PushUiState.Error
        AlertDialog(
            onDismissRequest = { viewModel.dismissPushResult() },
            icon = {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Push Failed") },
            text = { Text(state.message) },
            confirmButton = {
                Button(onClick = { viewModel.dismissPushResult() }) {
                    Text("OK")
                }
            }
        )
    }
}
