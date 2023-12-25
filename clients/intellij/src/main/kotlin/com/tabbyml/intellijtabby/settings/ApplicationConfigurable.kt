package com.tabbyml.intellijtabby.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class ApplicationConfigurable : Configurable {
  private lateinit var settingsPanel: ApplicationSettingsPanel

  override fun getDisplayName(): String {
    return "CodeMoss"
  }

  override fun createComponent(): JComponent {
    settingsPanel = ApplicationSettingsPanel()
    return settingsPanel.mainPanel
  }

  override fun isModified(): Boolean {
    val settings = service<ApplicationSettingsState>()
    val keymapSettings = service<KeymapSettings>()
    return settingsPanel.completionTriggerMode != settings.completionTriggerMode ||
        settingsPanel.serverEndpoint != settings.serverEndpoint ||
        settingsPanel.serverToken != settings.serverToken
  }

  override fun apply() {
    val settings = service<ApplicationSettingsState>()
    val keymapSettings = service<KeymapSettings>()
    settings.completionTriggerMode = settingsPanel.completionTriggerMode
    settings.serverEndpoint = settingsPanel.serverEndpoint
    settings.serverToken = settingsPanel.serverToken
  }

  override fun reset() {
    val settings = service<ApplicationSettingsState>()
    val keymapSettings = service<KeymapSettings>()
    settingsPanel.completionTriggerMode = settings.completionTriggerMode
    settingsPanel.serverEndpoint = settings.serverEndpoint
    settingsPanel.serverToken = settings.serverToken
  }
}