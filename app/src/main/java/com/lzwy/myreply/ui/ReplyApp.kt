package com.lzwy.myreply.ui

import android.net.Uri
import android.util.Log
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lzwy.myreply.data.llm.Model
import com.lzwy.myreply.ui.component.conversation.Conversation
import com.lzwy.myreply.ui.navigation.ReplyNavigationActions
import com.lzwy.myreply.ui.navigation.ReplyNavigationWrapper
import com.lzwy.myreply.ui.navigation.ReplyRoute

@Composable
fun ReplyApp(
    replyHomeUIState: ReplyHomeUIState,
    conversationState: ConversationState,
    llmLastReply: String,
    closeDetailScreen: () -> Unit = {},
    setLlmModel: (String) -> Unit,
    chatWithLLM: (String, Model) -> Unit,
    navigateToDetail: (Long) -> Unit = { _ -> },
    navigateToWrite: (Boolean) -> Unit,
    finishWriting: (String, String, List<Uri>?, String?) -> Unit,
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
                conversationState = conversationState,
                llmLastReply = llmLastReply,
                closeDetailScreen = closeDetailScreen,
                setLlmModel = setLlmModel,
                chatWithLLM = chatWithLLM,
                navigateToDetail = navigateToDetail,
                navigateToWrite = navigateToWrite,
                finishWriting = finishWriting,
                toggleMessageSelection = toggleMessageSelection,
            )
        }
    }
}

@Composable
private fun ReplyNavHost(
    navController: NavHostController,
    replyHomeUIState: ReplyHomeUIState,
    conversationState: ConversationState,
    llmLastReply: String,
    closeDetailScreen: () -> Unit,
    setLlmModel: (String) -> Unit,
    chatWithLLM: (String, Model) -> Unit,
    navigateToDetail: (Long) -> Unit,
    navigateToWrite: (Boolean) -> Unit,
    finishWriting: (String, String, List<Uri>?, String?) -> Unit,
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
                navController = navController,
                replyHomeUIState = replyHomeUIState,
                closeDetailScreen = closeDetailScreen,
                navigateToDetail = navigateToDetail,
                navigateToWrite = navigateToWrite,
                finishWriting = finishWriting,
                toggleMessageSelection = toggleMessageSelection
            )
        }
        composable(ReplyRoute.CHAT) {
            Conversation(
                conversationState = conversationState,
                llmLastReply = llmLastReply,
                setLlmModel = setLlmModel,
                chatWithLLM = chatWithLLM,
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable(ReplyRoute.ABOUT_ME) {
            EmptyComingSoon()
        }
    }
}