package com.tabbyml.intellijtabby.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenHowGetCodeMossHelp: AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    BrowserUtil.browse("https://chatmoss.feishu.cn/wiki/TQ6iwdCO4i9wZekrTnncgtYUnqh")
  }
}