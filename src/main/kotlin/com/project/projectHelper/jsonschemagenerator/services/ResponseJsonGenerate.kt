package com.project.projectHelper.jsonschemagenerator.services

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.project.projectHelper.jsonschemagenerator.pojo.ApiDto

/**
 * 入口
 *
 */
class ResponseJsonGenerate : AnAction() {
    override fun actionPerformed(actionEvent: AnActionEvent) {
        //获得api 需上传的接口列表 参数对象
        val generatorJsonSchema = GeneratorJsonSchema()
        val apiDtos: List<ApiDto?>? = generatorJsonSchema.actionPerformedList(actionEvent)
        val responseBodies = apiDtos?.map { it?.response } ?: emptyList()
        for (response in responseBodies) {
            Messages.showMessageDialog(response, "Method Return Value JSON Schema", Messages.getInformationIcon())
        }
    }

    companion object {
        private var notificationGroup: NotificationGroup? = null

        init {
            notificationGroup = NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true)
        }
    }
}
