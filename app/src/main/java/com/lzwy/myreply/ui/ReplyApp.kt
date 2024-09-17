package com.lzwy.myreply.ui

import android.util.Log
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.lzwy.myreply.ui.component.conversation.Conversation
import com.lzwy.myreply.ui.navigation.ReplyNavigationActions
import com.lzwy.myreply.ui.navigation.ReplyNavigationWrapper
import com.lzwy.myreply.ui.navigation.ReplyRoute
import com.lzwy.myreply.ui.utils.ReplyContentType
import com.lzwy.myreply.ui.utils.ReplyNavigationType
import kotlinx.coroutines.flow.Flow

@Composable
fun ReplyApp(
    replyHomeUIState: ReplyHomeUIState,
    llmState: String,
    closeDetailScreen: () -> Unit = {},
    accessLLM: () -> Unit,
    navigateToDetail: (Long) -> Unit = { _ -> },
    navigateToWrite: () -> Unit,
    toggleMessageSelection: (Long) -> Unit = { }
) {
    val navController = rememberNavController()
    val navigationActions = remember(navController) {
        ReplyNavigationActions(navController)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination =
        navBackStackEntry?.destination?.route ?: ReplyRoute.INBOX

    Surface {
        ReplyNavigationWrapper(
            selectedDestination = selectedDestination,
            navigateToTopLevelDestination = navigationActions::navigateTo
        ) {
            ReplyNavHost(
                navController = navController,
                replyHomeUIState = replyHomeUIState,
                llmState = llmState,
                closeDetailScreen = closeDetailScreen,
                accessLLM = accessLLM,
                navigateToDetail = navigateToDetail,
                navigateToWrite = navigateToWrite,
                toggleMessageSelection = toggleMessageSelection,
            )
        }
    }
}

@Composable
private fun ReplyNavHost(
    navController: NavHostController,
    replyHomeUIState: ReplyHomeUIState,
    llmState: String,
    closeDetailScreen: () -> Unit,
    accessLLM: () -> Unit,
    navigateToDetail: (Long) -> Unit,
    navigateToWrite: () -> Unit,
    toggleMessageSelection: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = ReplyRoute.INBOX,
    ) {
        composable(ReplyRoute.INBOX) {
            ReplyInboxScreen(
                replyHomeUIState = replyHomeUIState,
                closeDetailScreen = closeDetailScreen,
                navigateToDetail = navigateToDetail,
                navigateToWrite = navigateToWrite,
                toggleMessageSelection = toggleMessageSelection
            )
        }
        composable(ReplyRoute.CHAT) {
            fun onBackPressed() {
                navController.popBackStack()
            }
            Conversation(uiState = replyHomeUIState,
                llmState = llmState,
                accessLLM = accessLLM,
                onBackPressed = { onBackPressed() },
                onChannelChanged = { channel -> Log.i("LZWY", "onChannelChanged: $channel") }
                )
        }
        composable(ReplyRoute.ABOUT_ME) {
            EmptyComingSoon()
        }
    }
}