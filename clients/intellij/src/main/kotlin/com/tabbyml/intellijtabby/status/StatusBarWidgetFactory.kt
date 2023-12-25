package com.tabbyml.intellijtabby.status

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory
import com.intellij.ui.AnimatedIcon
import com.tabbyml.intellijtabby.agent.Agent
import com.tabbyml.intellijtabby.agent.AgentService
import com.tabbyml.intellijtabby.editor.CompletionProvider
import com.tabbyml.intellijtabby.settings.ApplicationSettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.swing.Icon

class StatusBarWidgetFactory : StatusBarEditorBasedWidgetFactory() {
  override fun getId(): String {
    return StatusBarWidgetFactory::class.java.name
  }

  override fun getDisplayName(): String {
    return "CodeMoss"
  }

  override fun createWidget(project: Project): StatusBarWidget {
    data class CombinedState(
      val settings: ApplicationSettingsState.State,
      val agentStatus: Enum<*>,
      val currentIssue: String?,
      val ongoingCompletion: CompletionProvider.CompletionContext?,
    )

    return object : EditorBasedStatusBarPopup(project, false) {
      val updateStatusScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
      val text = "CodeMoss"
      var icon: Icon = AnimatedIcon.Default()
      var tooltip = "CodeMoss: Initializing"

      init {
        val settings = service<ApplicationSettingsState>()
        val agentService = service<AgentService>()
        val completionProvider = service<CompletionProvider>()
        updateStatusScope.launch {
          combine(
            settings.state,
            agentService.status,
            agentService.currentIssue,
            completionProvider.ongoingCompletion
          ) { settings, agentStatus, currentIssue, ongoingCompletion ->
            CombinedState(settings, agentStatus, currentIssue, ongoingCompletion)
          }.collect {
            updateStatus(it)
          }
        }
      }

      override fun ID(): String {
        return "${StatusBarWidgetFactory::class.java.name}.widget"
      }

      override fun createInstance(project: Project): StatusBarWidget {
        return createWidget(project)
      }

      override fun getWidgetState(file: VirtualFile?): WidgetState {
        return WidgetState(tooltip, text, true).also {
          it.icon = icon
        }
      }

      override fun createPopup(context: DataContext?): ListPopup? {
        if (context == null) {
          return null
        }
        return JBPopupFactory.getInstance().createActionGroupPopup(
          tooltip,
          ActionManager.getInstance().getAction("Tabby.StatusBarPopupMenu") as ActionGroup,
          context,
          false,
          null,
          10,
        )
      }

      private fun updateStatus(state: CombinedState) {
        when (state.agentStatus) {
          AgentService.Status.INITIALIZING, Agent.Status.NOT_INITIALIZED -> {
            icon = AnimatedIcon.Default()
            tooltip = "CodeMoss: Initializing"
          }

          AgentService.Status.INITIALIZATION_FAILED -> {
            icon = AllIcons.General.Error
            tooltip = "CodeMoss: Initialization failed"
          }

          Agent.Status.READY -> {
            val muted = mutableListOf<String>()
            if (state.settings.notificationsMuted.contains("completionResponseTimeIssues")) {
              muted += listOf("slowCompletionResponseTime", "highCompletionTimeoutRate")
            }
            if (state.currentIssue != null && state.currentIssue !in muted) {
              icon = AllIcons.General.Warning
              tooltip = when (state.currentIssue) {
                "slowCompletionResponseTime" -> "CodeMoss: Completion requests appear to take too much time"
                "highCompletionTimeoutRate" -> "CodeMoss: Most completion requests timed out"
                else -> "CodeMoss: Issues exist"
              }
            } else {
              when (state.settings.completionTriggerMode) {
                ApplicationSettingsState.TriggerMode.AUTOMATIC -> {
                  if (state.ongoingCompletion == null) {
                    icon = AllIcons.Actions.Checked
                    tooltip = "CodeMoss: Welcome"
                  } else {
                    icon = AnimatedIcon.Default()
                    tooltip = "CodeMoss: Welcome"
                  }
                }

                ApplicationSettingsState.TriggerMode.MANUAL -> {
                  if (state.ongoingCompletion == null) {
                    icon = AllIcons.General.ChevronRight
                    tooltip = "CodeMoss: Standing by, please manually trigger code completion."
                  } else {
                    icon = AnimatedIcon.Default()
                    tooltip = "CodeMoss: Generating code completions"
                  }
                }
              }
            }
          }

          Agent.Status.DISCONNECTED -> {
            icon = AllIcons.General.Error
            tooltip = "CodeMoss: Cannot connect to Server, please check settings"
          }

          Agent.Status.UNAUTHORIZED -> {
            icon = AllIcons.General.Warning
            tooltip = "CodeMoss: Authorization required, please set your personal token in settings"
          }
        }
        invokeLater {
          update { myStatusBar?.updateWidget(ID()) }
        }
      }
    }
  }

  override fun disposeWidget(widget: StatusBarWidget) {
    // Nothing to do
  }
}
