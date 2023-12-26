package com.tabbyml.intellijtabby.settings

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.keymap.impl.ui.KeymapPanel
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.tabbyml.intellijtabby.agent.Agent
import com.tabbyml.intellijtabby.agent.AgentService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JPanel


private fun FormBuilder.addCopyableTooltip(text: String): FormBuilder {
  return this.addComponentToRightColumn(
    JBLabel(
      text,
      UIUtil.ComponentStyle.SMALL,
      UIUtil.FontColor.BRIGHTER
    ).apply {
      setBorder(JBUI.Borders.emptyLeft(10))
      setCopyable(true)
    },
    1,
  )
}

class ApplicationSettingsPanel {
  private val serverEndpointTextField = JBTextField()
  private val serverTokenPasswordField = JBPasswordField()
  private val serverEndpointCheckConnectionButton = JButton("检查链接").apply {
    addActionListener {
      val parentComponent = this@ApplicationSettingsPanel.mainPanel
      val agentService = service<AgentService>()
      val settings = service<ApplicationSettingsState>()

      val task = object : Task.Modal(
        null,
        parentComponent,
        "检查链接",
        true
      ) {
        lateinit var job: Job
        override fun run(indicator: ProgressIndicator) {
          job = agentService.scope.launch {
            indicator.isIndeterminate = true
            indicator.text = "检查链接..."
            settings.serverEndpoint = serverEndpoint
            settings.serverToken = serverToken
            var serverConfig = agentService.getConfig().server
            while (serverConfig?.endpoint != serverEndpoint || serverConfig.token != serverToken) {
              Thread.sleep(200)
              serverConfig = agentService.getConfig().server
            }
            when (agentService.status.value) {
              Agent.Status.READY -> {
                invokeLater(ModalityState.stateForComponent(parentComponent)) {
                  Messages.showInfoMessage(
                    parentComponent,
                    "成功连接至服务器",
                    "链接成功"
                  )
                }
              }

              Agent.Status.UNAUTHORIZED -> {
                val currentToken = agentService.getConfig().server?.token
                val message = if (currentToken?.isNotBlank() == true) {
                  "Tabby server requires authentication, but the current token is invalid."
                } else {
                  "Tabby server requires authentication, please set your personal token."
                }
                invokeLater(ModalityState.stateForComponent(parentComponent)) {
                  Messages.showErrorDialog(
                    parentComponent,
                    message,
                    "Check Connection Failed"
                  )
                }
              }

              else -> {
                val detail = agentService.getCurrentIssueDetail()
                if (detail?.get("name") == "connectionFailed") {
                  invokeLater(ModalityState.stateForComponent(parentComponent)) {
                    val errorMessage = (detail["message"] as String?)?.replace("\n", "<br/>") ?: ""
                    val messages = "<html>Failed to connect to the Tabby server:<br/>${errorMessage}</html>"
                    Messages.showErrorDialog(parentComponent, messages, "Check Connection Failed")
                  }
                }
              }
            }
          }
          while (job.isActive) {
            indicator.checkCanceled()
            Thread.sleep(100)
          }
        }

        override fun onCancel() {
          job.cancel()
        }
      }
      ProgressManager.getInstance().run(task)
    }
  }
  private val serverEndpointPanel = FormBuilder.createFormBuilder()
    .addLabeledComponent("授权码", serverTokenPasswordField, 0, false)
    .addCopyableTooltip(
      """
      <html>
      获取教程：
      <i>https://chatmoss.feishu.cn/wiki/TQ6iwdCO4i9wZekrTnncgtYUnqh</i>
      </html>
      """.trimIndent()
    )
    .addSeparator()
    .addLabeledComponent("服务端点(可不填)", serverEndpointTextField, 0, false)
    .addCopyableTooltip(
      """
      <html>
      接口请求地址（不填默认是CodeMoss的服务端点）<br/>
      如果想本地部署可以参考这个文档：<br/>
      <i>https://chatmoss.feishu.cn/wiki/JAR4wCzHFi85XEkTe8bc18Qcnch</i>
      </html>
      """.trimIndent()
    )
    .addSeparator()
    .addCopyableTooltip(
      """
      <html>
      首次安装完后，需重启下IDE已加载资源
      </html>
      """.trimIndent()
    )
    .panel

  private val completionTriggerModeAutomaticRadioButton = JBRadioButton("Automatic")
  private val completionTriggerModeManualRadioButton = JBRadioButton("Manual")
  private val completionTriggerModeRadioGroup = ButtonGroup().apply {
    add(completionTriggerModeAutomaticRadioButton)
    add(completionTriggerModeManualRadioButton)
  }
  private val completionTriggerModePanel: JPanel = FormBuilder.createFormBuilder()
    .addComponent(completionTriggerModeAutomaticRadioButton)
    .addCopyableTooltip("Trigger automatically when you stop typing")
    .addComponent(completionTriggerModeManualRadioButton)
    .addCopyableTooltip("Trigger on-demand by pressing a shortcut")
    .panel

  private val resetMutedNotificationsButton = JButton("Reset \"Don't Show Again\" Notifications").apply {
    addActionListener {
      val settings = service<ApplicationSettingsState>()
      settings.notificationsMuted = listOf()
      invokeLater(ModalityState.stateForComponent(this@ApplicationSettingsPanel.mainPanel)) {
        Messages.showInfoMessage("Reset \"Don't Show Again\" notifications successfully.", "Reset Notifications")
      }
    }
  }
  private val resetMutedNotificationsPanel: JPanel = FormBuilder.createFormBuilder()
    .addComponent(resetMutedNotificationsButton)
    .panel

  val mainPanel: JPanel = FormBuilder.createFormBuilder()
    .addLabeledComponent("", serverEndpointPanel, 5, false)
    .addSeparator(5)
    .apply {
      if (service<ApplicationSettingsState>().notificationsMuted.isNotEmpty()) {
        addSeparator(5)
        addLabeledComponent("Notifications", resetMutedNotificationsPanel, 5, false)
      }
    }
    .addComponentFillVertically(JPanel(), 0)
    .panel

  var serverEndpoint: String
    get() = serverEndpointTextField.text
    set(value) {
      serverEndpointTextField.text = value
    }

  var serverToken: String
    get() = String(serverTokenPasswordField.password)
    set(value) {
      serverTokenPasswordField.text = value
    }

  var completionTriggerMode: ApplicationSettingsState.TriggerMode
    get() = if (completionTriggerModeAutomaticRadioButton.isSelected) {
      ApplicationSettingsState.TriggerMode.AUTOMATIC
    } else {
      ApplicationSettingsState.TriggerMode.MANUAL
    }
    set(value) {
      when (value) {
        ApplicationSettingsState.TriggerMode.AUTOMATIC -> completionTriggerModeAutomaticRadioButton.isSelected = true
        ApplicationSettingsState.TriggerMode.MANUAL -> completionTriggerModeManualRadioButton.isSelected = true
      }
    }
}