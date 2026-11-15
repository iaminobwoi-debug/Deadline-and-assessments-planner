package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AssessmentStatus
import com.example.data.model.StatusFilter
import com.example.data.repository.AssessmentRepository
import com.example.ui.auth.SignInScreen
import com.example.ui.auth.signOutUser
import com.example.ui.screens.AddEditDeadlineScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.theme.ScholarPaceTheme
import com.example.ui.viewmodel.PlannerUiState
import com.example.ui.viewmodel.PlannerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object AppRoutes {
    const val OVERVIEW = "overview"
    const val PLANNER = "planner"
    const val EDITOR = "editor"
    const val EDITOR_ROUTE = "editor?assessmentId={assessmentId}"
    fun editorFor(assessmentId: String? = null): String =
        if (assessmentId.isNullOrBlank()) EDITOR else "editor?assessmentId=$assessmentId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScholarPaceTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        SignInScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedPlannerShell(
                currentUserId = uid,
                userDisplayName = user.displayName ?: "",
                userEmail = user.email ?: ""
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedPlannerShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: PlannerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PlannerViewModel(
                    repository = AssessmentRepository(db),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppRoutes.OVERVIEW
    val isEditorRoute = currentRoute.startsWith(AppRoutes.EDITOR)

    var showMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedWidth = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (!isEditorRoute) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        actions = {
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.testTag("top_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "Planner options"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(id = R.string.action_load_sample_plan)) },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Refresh, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            viewModel.loadSamplePlan()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(stringResource(id = R.string.action_sign_out))
                                                if (userEmail.isNotBlank()) {
                                                    Text(
                                                        text = userEmail,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            signOutUser(credentialManager, scope)
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!isExpandedWidth && !isEditorRoute) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.OVERVIEW,
                            onClick = {
                                navController.navigate(AppRoutes.OVERVIEW) {
                                    popUpTo(AppRoutes.OVERVIEW) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == AppRoutes.OVERVIEW) {
                                        Icons.Filled.Dashboard
                                    } else {
                                        Icons.Outlined.Dashboard
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_overview)) },
                            modifier = Modifier.testTag("nav_overview")
                        )
                        NavigationBarItem(
                            selected = currentRoute == AppRoutes.PLANNER,
                            onClick = {
                                navController.navigate(AppRoutes.PLANNER) {
                                    popUpTo(AppRoutes.OVERVIEW) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == AppRoutes.PLANNER) {
                                        Icons.AutoMirrored.Filled.EventNote
                                    } else {
                                        Icons.AutoMirrored.Outlined.EventNote
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_planner)) },
                            modifier = Modifier.testTag("nav_planner")
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!isEditorRoute) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate(AppRoutes.editorFor(null)) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(id = R.string.action_add_deadline)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_deadline_fab")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedWidth && !isEditorRoute) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.weight(0.1f))
                        NavigationRailItem(
                            selected = currentRoute == AppRoutes.OVERVIEW,
                            onClick = {
                                navController.navigate(AppRoutes.OVERVIEW) {
                                    popUpTo(AppRoutes.OVERVIEW) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == AppRoutes.OVERVIEW) {
                                        Icons.Filled.Dashboard
                                    } else {
                                        Icons.Outlined.Dashboard
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_overview)) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == AppRoutes.PLANNER,
                            onClick = {
                                navController.navigate(AppRoutes.PLANNER) {
                                    popUpTo(AppRoutes.OVERVIEW) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute == AppRoutes.PLANNER) {
                                        Icons.AutoMirrored.Filled.EventNote
                                    } else {
                                        Icons.AutoMirrored.Outlined.EventNote
                                    },
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(id = R.string.nav_planner)) }
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.OVERVIEW,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppRoutes.OVERVIEW) {
                        when (val s = uiState) {
                            is PlannerUiState.Loading -> LoadingPane()
                            is PlannerUiState.Error -> ErrorPane(s.message)
                            is PlannerUiState.Ready -> {
                                OverviewScreen(
                                    state = s,
                                    userName = userDisplayName.substringBefore(" "),
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onSelectCourse = viewModel::selectCourse,
                                    onClearCourseSearch = {
                                        viewModel.setSearchQuery("")
                                        if (s.filterState.selectedCourse != null) {
                                            viewModel.selectCourse(null)
                                        }
                                    },
                                    onSelectRoleLens = viewModel::selectRoleLens,
                                    onOpenOverdueInPlanner = {
                                        viewModel.clearFilters()
                                        viewModel.selectStatusFilter(StatusFilter.OVERDUE)
                                        navController.navigate(AppRoutes.PLANNER) {
                                            popUpTo(AppRoutes.OVERVIEW) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenTypeInPlanner = { type ->
                                        viewModel.clearFilters()
                                        viewModel.selectType(type)
                                        navController.navigate(AppRoutes.PLANNER) {
                                            popUpTo(AppRoutes.OVERVIEW) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenPlanner = {
                                        navController.navigate(AppRoutes.PLANNER) {
                                            popUpTo(AppRoutes.OVERVIEW) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onEditAssessment = { item ->
                                        navController.navigate(AppRoutes.editorFor(item.id))
                                    },
                                    onCycleStatus = viewModel::toggleComplete,
                                    onLoadSamplePlan = viewModel::loadSamplePlan,
                                    onAddDeadline = {
                                        navController.navigate(AppRoutes.editorFor(null))
                                    }
                                )
                            }
                        }
                    }

                    composable(AppRoutes.PLANNER) {
                        when (val s = uiState) {
                            is PlannerUiState.Loading -> LoadingPane()
                            is PlannerUiState.Error -> ErrorPane(s.message)
                            is PlannerUiState.Ready -> {
                                PlannerScreen(
                                    state = s,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onSelectCourse = viewModel::selectCourse,
                                    onSelectType = viewModel::selectType,
                                    onSelectStatusFilter = viewModel::selectStatusFilter,
                                    onClearFilters = viewModel::clearFilters,
                                    onEditAssessment = { item ->
                                        navController.navigate(AppRoutes.editorFor(item.id))
                                    },
                                    onCycleStatus = viewModel::toggleComplete,
                                    onLoadSamplePlan = viewModel::loadSamplePlan,
                                    onAddDeadline = {
                                        navController.navigate(AppRoutes.editorFor(null))
                                    }
                                )
                            }
                        }
                    }

                    composable(
                        route = AppRoutes.EDITOR_ROUTE,
                        arguments = listOf(
                            navArgument("assessmentId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { entry ->
                        val assessmentId = entry.arguments?.getString("assessmentId")
                        val readyState = uiState as? PlannerUiState.Ready
                        val existing = readyState?.allAssessments?.firstOrNull { it.id == assessmentId }
                        val courses = readyState?.availableCourses ?: emptyList()

                        AddEditDeadlineScreen(
                            existing = existing,
                            availableCourses = courses,
                            isSaving = isMutating,
                            onNavigateBack = { navController.popBackStack() },
                            onSave = { draft ->
                                viewModel.saveAssessment(existing, draft) {
                                    navController.popBackStack()
                                }
                            },
                            onReopenCompleted = { newStatus: AssessmentStatus ->
                                if (existing != null) {
                                    viewModel.setStatus(existing.id, newStatus)
                                }
                            },
                            onDelete = existing?.let { target ->
                                {
                                    viewModel.deleteAssessment(target.id) {
                                        navController.popBackStack()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingPane() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading your academic plan…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorPane(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}
