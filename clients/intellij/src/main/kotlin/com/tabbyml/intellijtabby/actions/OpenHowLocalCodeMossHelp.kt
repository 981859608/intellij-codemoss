package com.tabbyml.intellijtabby.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenHowLocalCodeMossHelp: AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    BrowserUtil.browse("https://chatmoss.feishu.cn/wiki/JAR4wCzHFi85XEkTe8bc18Qcnch")
  }
}