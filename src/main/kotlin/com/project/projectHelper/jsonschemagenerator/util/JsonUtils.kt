package com.project.projectHelper.jsonschemagenerator.util

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.project.projectHelper.jsonschemagenerator.constant.REQUEST_BODY
import com.project.projectHelper.jsonschemagenerator.services.GeneratorJsonSchema
import com.project.projectHelper.jsonschemagenerator.util.AnnotationUtil.findAnnotation

/**
 * 使用 object 定义的类只有一个实例，类似于 Java 中的单例模式
 *
 */
object JsonUtils {
    /**
     *  获得请求参数
     */
    fun getRequest(project: Project?, psiMethodTarget: PsiMethod): String? {
        // 获取所有的参数列表
        val psiParameters = psiMethodTarget.parameterList.parameters
        for (psiParameter in psiParameters) {
            // 找到@RequestBody注解
            val psiAnnotation = findAnnotation(psiParameter, REQUEST_BODY)
            if (psiAnnotation != null) {
                return convertToJsonSchema(project, psiParameter.type)
            }
        }
        return null
    }

    /**
     * 获取给定PsiType的JSON表示
     *
     * @param project
     * @param psiType
     * @return
     */
    fun convertToJsonSchema(project: Project?, psiType: PsiType?): String {
        // 判断是否带有泛型
        val types = psiType?.canonicalText?.split("<".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()

        // 如果拆分后的数组长度大于1，表示泛型类型
        return if (types!!.size > 1) {
            dealGenerics(project, psiType, types)
        } else {
            // 非泛型类型的情况
            doFillCommonType(project, psiType)
        }
    }

    /**
     * 填充普通类型
     *
     * @param project
     * @param psiType
     * @return
     */
    private fun doFillCommonType(project: Project?, psiType: PsiType?): String {
        // 通过JavaPsiFacade查找子类类型
        val psiClassChild = psiType?.let {
            JavaPsiFacade.getInstance(project).findClass(
                it.canonicalText, GlobalSearchScope.allScope(
                    project!!
                )
            )
        }

        // 用于存储结果
        val result: MutableMap<String, Any> = LinkedHashMap()

        // 创建一个列表，用于存储必需字段
        val requiredList: MutableList<String> = ArrayList()

        // 获取子类的字段
        val filedMap = GeneratorJsonSchema.getFields(psiClassChild, project, null, null, requiredList)

        // 向结果中添加相关属性
        result["type"] = "object"
        result["required"] = requiredList
        if (psiType != null) {
            result["title"] = psiType.presentableText
        }
        if (psiType != null) {
            result["description"] = (psiType.presentableText + " :" + psiClassChild!!.name).trim { it <= ' ' }
        }
        result["properties"] = filedMap

        // 使用GsonBuilder将结果转换为格式化的JSON字符串并返回
        return GsonBuilder().setPrettyPrinting().create().toJson(result)
    }

    private fun dealGenerics(project: Project?, psiType: PsiType?, types: Array<String>?): String {
        // 通过JavaPsiFacade查找子类类型
        val psiClassChild = JavaPsiFacade.getInstance(project).findClass(
            types!![0], GlobalSearchScope.allScope(
                project!!
            )
        )

        // 用于存储结果
        val result: MutableMap<String, Any> = LinkedHashMap()

        // 创建一个列表，用于存储必需字段
        val requiredList: MutableList<String> = ArrayList()
        val filedMap = GeneratorJsonSchema.getFields(psiClassChild, project, types, 1, requiredList)

        // 向结果中添加相关属性
        result["type"] = "object"
        if (psiType != null) {
            result["title"] = psiType.presentableText
        }
        result["required"] = requiredList
        if (psiType != null) {
            result["description"] = (psiType.presentableText + " :" + psiClassChild!!.name).trim { it <= ' ' }
        }
        result["properties"] = filedMap

        // 使用GsonBuilder将结果转换为格式化的JSON字符串并返回
        return GsonBuilder().setPrettyPrinting().create().toJson(result)
    }
}
