package com.project.projectHelper.jsonschemagenerator.util

import com.google.common.base.Strings
import com.intellij.psi.javadoc.PsiDocComment
import java.util.*

/**
 * 描述工具
 *
 */
object DescUtil {
    /**
     * 去除字符串首尾出现的某个字符.
     */
    fun trimFirstAndLastChar(source: String, element: Char): String {
        var source = source
        var beginIndexFlag: Boolean
        var endIndexFlag: Boolean
        do {
            if (Strings.isNullOrEmpty(source.trim { it <= ' ' }) || source == element.toString()) {
                source = ""
                break
            }
            val beginIndex = if (source.indexOf(element) == 0) 1 else 0
            val endIndex =
                if (source.lastIndexOf(element) + 1 == source.length) source.lastIndexOf(element) else source.length
            source = source.substring(beginIndex, endIndex)
            beginIndexFlag = source.indexOf(element) == 0
            endIndexFlag = source.lastIndexOf(element) + 1 == source.length
        } while (beginIndexFlag || endIndexFlag)
        return source
    }

    /**
     * 获得属性注释
     */
    fun getFiledDesc(psiDocComment: PsiDocComment): String {
        if (Objects.nonNull(psiDocComment)) {
            val fileText = psiDocComment.text
            if (!Strings.isNullOrEmpty(fileText)) {
                val trim = fileText.replace("/", "").replace("*", "")
                val trimWhiteSpace = trim.replace(" ", "")
                val trimEnter = trimWhiteSpace.replace("\n", ",")
                val trimTab = trimEnter.replace("\t", "")
                return trimFirstAndLastChar(trimTab, ',')
            }
        }
        return ""
    }
}
