package com.project.projectHelper.jsonschemagenerator.services

import com.google.common.base.Strings
import com.google.gson.JsonObject
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.project.projectHelper.jsonschemagenerator.constant.NOT_BLANK
import com.project.projectHelper.jsonschemagenerator.constant.NOT_EMPTY
import com.project.projectHelper.jsonschemagenerator.constant.NOT_NULL
import com.project.projectHelper.jsonschemagenerator.constant.TypeConstant.COLLECT_TYPES
import com.project.projectHelper.jsonschemagenerator.constant.TypeConstant.GENERIC_LIST
import com.project.projectHelper.jsonschemagenerator.constant.TypeConstant.isNormalType
import com.project.projectHelper.jsonschemagenerator.pojo.ApiDto
import com.project.projectHelper.jsonschemagenerator.util.AnnotationUtil.findAnnotation
import com.project.projectHelper.jsonschemagenerator.util.DescUtil
import com.project.projectHelper.jsonschemagenerator.util.JsonUtils.convertToJsonSchema
import com.project.projectHelper.jsonschemagenerator.util.JsonUtils.getRequest
import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * 为了yapi 创建的
 */
class GeneratorJsonSchema {
    /**
     * 批量生成 接口数据
     *
     * @param actionEvent
     * @return
     */
    fun actionPerformedList(actionEvent: AnActionEvent): MutableList<ApiDto?>? {
        // 获取编辑器对象
        val editor = actionEvent.dataContext.getData(CommonDataKeys.EDITOR)
        //
        val psiFile = actionEvent.dataContext.getData(CommonDataKeys.PSI_FILE)
        val selectedText = actionEvent.getRequiredData(CommonDataKeys.EDITOR).selectionModel.selectedText
        // 从编辑器里面获取当前的项目
        val project = editor!!.project
        if (validSelected(selectedText, project)) {
            return null
        }
        // 获取当前光标位置的元素
        val referenceAt = psiFile!!.findElementAt(editor.caretModel.offset)
        val selectedClass = PsiTreeUtil.getContextOfType(referenceAt, PsiClass::class.java)
        val apiDtos: MutableList<ApiDto?> = ArrayList()
        // 判断是否直接作用于整个controller类上面
        if (StringUtils.equals(selectedText, selectedClass!!.name)) {
            // 获取光标选择的方法
            dealAllMethod(project, selectedClass, apiDtos)
        } else {
            // 找到特定的method
            dealSelectMethod(selectedText, project, selectedClass, apiDtos)
        }
        return apiDtos
    }

    companion object {
        const val JSON_TYPE = "type"
        const val JSON_DESCRIPTION = "description"
        const val JSON_PROPERTIES = "properties"
        const val JSON_REQUIRED = "required"
        private var notificationGroup: NotificationGroup? = null

        init {
            notificationGroup = NotificationGroup("Java2Json.NotificationGroup", NotificationDisplayType.BALLOON, true)
        }

        /**
         * @param psiMethodTarget 目标的方法
         * @param project         当前的项目
         * @return
         */
        fun actionPerformed(psiMethodTarget: PsiMethod, project: Project?): ApiDto? {
            val apiDto = ApiDto(null, null)
            try {
                // 生成响应参数
                val response: String = convertToJsonSchema(project, psiMethodTarget.returnType)
                apiDto.response = response
                val request: String? = getRequest(project, psiMethodTarget)
                apiDto.requestBody = request
                return apiDto
            } catch (ex: Exception) {
                val error = notificationGroup!!.createNotification("Convert to JSON failed.", NotificationType.ERROR)
                Notifications.Bus.notify(error, project)
            }
            return null
        }

        /**
         * 获得属性列表
         */
        fun getFields(
            psiClass: PsiClass?,
            project: Project?,
            childType: Array<String>?,
            index: Int?,
            requiredList: MutableList<String>
        ): Map<String, Any> {
            val filedMap: MutableMap<String, Any> = LinkedHashMap()
            if (psiClass != null) {
                if (Objects.nonNull(psiClass.superClass) && Objects.nonNull(COLLECT_TYPES[psiClass.superClass!!.name])) {
                    for (field in psiClass.fields) {
                        //如果是有notnull 和 notEmpty 注解就加入必填
                        addRequiredList(requiredList, field)
                        getField(field, project, filedMap, childType, index, psiClass.name!!)
                    }
                } else {
                    if (GENERIC_LIST.contains(psiClass.name) && childType != null && childType.size > index!!) {
                        return dealGenericByRecursion(project, childType, index, requiredList)
                    } else {
                        for (field in psiClass.allFields) {
                            //如果是有notnull 和 notEmpty 注解就加入必填
                            addRequiredList(requiredList, field)
                            getField(field, project, filedMap, childType, index, psiClass.name!!)
                        }
                    }
                }
            }
            return filedMap
        }

        /**
         * 添加必填字段
         *
         * @param requiredList
         * @param field
         */
        private fun addRequiredList(requiredList: MutableList<String>, field: PsiField) {
            if (Objects.nonNull(findAnnotation(field, NOT_BLANK))
                || Objects.nonNull(findAnnotation(field, NOT_NULL))
                || Objects.nonNull(findAnnotation(field, NOT_EMPTY))
            ) {
                requiredList.add(field.name)
            }
        }

        /**
         * 递归处理泛型
         *
         * @param project
         * @param childType
         * @param index
         * @param requiredList
         * @return
         */
        private fun dealGenericByRecursion(
            project: Project?,
            childType: Array<String>,
            index: Int?,
            requiredList: MutableList<String>
        ): Map<String, Any> {
            val child = childType[index!!].split(">".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            val psiClassChild = JavaPsiFacade.getInstance(project).findClass(
                child, GlobalSearchScope.allScope(
                    project!!
                )
            )
            return getFields(psiClassChild, project, childType, index + 1, requiredList)
        }

        /**
         * 填充单个属性
         */
        fun getField(
            field: PsiField,
            project: Project?,
            filedMap: MutableMap<String, Any>,
            childType: Array<String>?,
            index: Int?,
            pName: String
        ) {
            if (field.modifierList!!.hasModifierProperty(PsiModifier.FINAL)) {
                return
            }
            val type = field.type
            val name = field.name
            var remark = ""
            if (field.docComment != null) {
                remark = DescUtil.getFiledDesc(field.docComment!!)
            }
            // 如果是基本类型
            if (type is PsiPrimitiveType) {
                doFillBaseType(filedMap, name, remark, type.getPresentableText())
            } else {
                val fieldTypeName = type.presentableText
                if (isNormalType(fieldTypeName)) {
                    doFillBaseType(filedMap, name, remark, fieldTypeName)
                } else if (GENERIC_LIST.contains(fieldTypeName)) {
                    doFillGenericsListType(project, filedMap, childType, index, pName, name, remark)
                } else if (type is PsiArrayType) {
                    doFillArrayType(project, filedMap, pName, type, name, remark)
                } else {
                    val listFlag = fieldTypeName.startsWith(MutableList::class.java.simpleName) ||
                            fieldTypeName.startsWith(MutableSet::class.java.simpleName) ||
                            fieldTypeName.startsWith(HashSet::class.java.simpleName)
                    if (listFlag) {
                        doFillListType(project, filedMap, childType, index, pName, type, name, remark)
                    } else {
                        val mapFlag = fieldTypeName.startsWith(HashMap::class.java.simpleName) ||
                                fieldTypeName.startsWith(MutableMap::class.java.name) ||
                                fieldTypeName.startsWith(LinkedHashMap::class.java.name)
                        if (!mapFlag) {
                            doFillCommonObjType(project, filedMap, childType, index, pName, type, name, remark)
                        }
                    }
                }
            }
        }

        private fun doFillBaseType(
            filedMap: MutableMap<String, Any>,
            name: String,
            remark: String,
            fieldTypeName: String
        ) {
            val jsonObject = JsonObject()
            jsonObject.addProperty(JSON_TYPE, fieldTypeName)
            if (!Strings.isNullOrEmpty(remark)) {
                jsonObject.addProperty(JSON_DESCRIPTION, remark)
            }
            filedMap[name] = jsonObject
        }

        /**
         * @param project
         * @param filedMap
         * @param childType
         * @param index
         * @param pName
         * @param name
         * @param remark
         */
        private fun doFillGenericsListType(
            project: Project?,
            filedMap: MutableMap<String, Any>,
            childType: Array<String>?,
            index: Int?,
            pName: String,
            name: String,
            remark: String
        ) {
            var index = index
            if (childType != null) {
                val child = childType[index!!].split(">".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                if (child.contains(MutableList::class.java.name) || child.contains(MutableSet::class.java.name) ||
                    child.contains(HashSet::class.java.name)
                ) {
                    index = index + 1
                    val psiClassChild = JavaPsiFacade.getInstance(project).findClass(
                        childType[index].split(">".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0],
                        GlobalSearchScope.allScope(
                            project!!
                        )
                    )
                    doFillCollect(
                        filedMap,
                        psiClassChild!!.name!!,
                        remark,
                        psiClassChild,
                        project,
                        name,
                        pName,
                        childType,
                        index + 1
                    )
                } else {
                    val genericMap: MutableMap<String, Any> = LinkedHashMap()
                    genericMap[JSON_TYPE] = "object"
                    val psiClassChild = JavaPsiFacade.getInstance(project).findClass(
                        child, GlobalSearchScope.allScope(
                            project!!
                        )
                    )
                    val realRemark = if (Strings.isNullOrEmpty(remark)) psiClassChild!!.name!!
                        .trim { it <= ' ' } else remark + " ," + psiClassChild!!.name!!.trim { it <= ' ' }
                    genericMap[JSON_DESCRIPTION] = realRemark
                    if (pName != psiClassChild.name) {
                        val requiredList: MutableList<String> = ArrayList()
                        val fields = getFields(psiClassChild, project, childType, index + 1, requiredList)
                        genericMap[JSON_PROPERTIES] = fields
                        genericMap[JSON_REQUIRED] = requiredList
                    } else {
                        genericMap[JSON_TYPE] = pName
                    }
                    filedMap[name] = genericMap
                }
            }
        }

        /**
         * 填充普通兑现良性
         *
         * @param project
         * @param filedMap
         * @param childType
         * @param index
         * @param pName
         * @param type
         * @param name
         * @param remark
         */
        private fun doFillCommonObjType(
            project: Project?,
            filedMap: MutableMap<String, Any>,
            childType: Array<String>?,
            index: Int?,
            pName: String,
            type: PsiType,
            name: String,
            remark: String
        ) {
            val objMap: MutableMap<String, Any> = LinkedHashMap()
            val psiClass = PsiUtil.resolveClassInType(type)
            objMap[JSON_TYPE] = "object"
            val realRemark = if (Strings.isNullOrEmpty(remark)) psiClass!!.name!!
                .trim { it <= ' ' } else (remark + " ," + psiClass!!.name).trim { it <= ' ' }
            objMap[JSON_DESCRIPTION] = realRemark
            if (pName != (type as PsiClassReferenceType).className) {
                val requiredList: MutableList<String> = ArrayList()
                val fields = getFields(PsiUtil.resolveClassInType(type), project, childType, index, requiredList)
                objMap[JSON_PROPERTIES] = fields
                objMap[JSON_REQUIRED] = requiredList
            } else {
                objMap[JSON_TYPE] = pName
            }
            filedMap[name] = objMap
        }

        /**
         * 填充集合类型
         *
         * @param project
         * @param filedMap
         * @param childType
         * @param index
         * @param pName
         * @param type
         * @param name
         * @param remark
         */
        private fun doFillListType(
            project: Project?,
            filedMap: MutableMap<String, Any>,
            childType: Array<String>?,
            index: Int?,
            pName: String,
            type: PsiType,
            name: String,
            remark: String
        ) {
            val iterableType = PsiUtil.extractIterableTypeParameter(type, false)
            val iterableClass = PsiUtil.resolveClassInClassTypeOnly(iterableType)
            if (Objects.nonNull(iterableClass)) {
                val classTypeName = iterableClass!!.name!!
                doFillCollect(filedMap, classTypeName, remark, iterableClass, project, name, pName, childType, index)
            }
        }

        /**
         * 填充数组类型
         *
         * @param project
         * @param filedMap
         * @param pName
         * @param type
         * @param name
         * @param remark
         */
        private fun doFillArrayType(
            project: Project?,
            filedMap: MutableMap<String, Any>,
            pName: String,
            type: PsiType,
            name: String,
            remark: String
        ) {
            val deepType = type.deepComponentType
            val arrayList: MutableMap<String, Any> = LinkedHashMap()
            val deepTypeName = deepType.presentableText
            var cType = ""
            if (deepType is PsiPrimitiveType) {
                arrayList[JSON_TYPE] = type.presentableText
                if (!Strings.isNullOrEmpty(remark)) {
                    arrayList[JSON_DESCRIPTION] = remark
                }
            } else if (isNormalType(deepTypeName)) {
                arrayList[JSON_TYPE] = deepTypeName
                if (!Strings.isNullOrEmpty(remark)) {
                    arrayList[JSON_DESCRIPTION] = remark
                }
            } else {
                arrayList[JSON_TYPE] = "object"
                val psiClass = PsiUtil.resolveClassInType(deepType)
                cType = psiClass!!.name!!
                val realRemark = if (Strings.isNullOrEmpty(remark)) psiClass.name!!
                    .trim { it <= ' ' } else remark + " ," + psiClass.name!!.trim { it <= ' ' }
                arrayList[JSON_DESCRIPTION] = realRemark
                if (pName != PsiUtil.resolveClassInType(deepType)!!.name) {
                    val requiredList: MutableList<String> = ArrayList()
                    arrayList[JSON_PROPERTIES] = getFields(psiClass, project, null, null, requiredList)
                    arrayList[JSON_REQUIRED] = requiredList
                } else {
                    arrayList[JSON_TYPE] = pName
                }
            }
            val arrayMap: MutableMap<String, Any> = LinkedHashMap()
            arrayMap[JSON_TYPE] = "array"
            val trim = "$remark :$cType".trim { it <= ' ' }
            arrayMap[JSON_DESCRIPTION] = trim
            arrayMap["items"] = arrayList
            filedMap[name] = arrayMap
        }

        /**
         * 填充集合类型集合
         */
        fun doFillCollect(
            filedMap: MutableMap<String, Any>,
            classTypeName: String,
            remark: String,
            psiClass: PsiClass?,
            project: Project?,
            name: String,
            pName: String,
            childType: Array<String>?,
            index: Int?
        ) {
            val arrays: MutableMap<String, Any> = LinkedHashMap()
            if (isNormalType(classTypeName) || COLLECT_TYPES.containsKey(classTypeName)) {
                arrays[JSON_TYPE] = classTypeName
                if (!Strings.isNullOrEmpty(remark)) {
                    arrays[JSON_DESCRIPTION] = remark
                }
            } else {
                arrays[JSON_TYPE] = "object"
                val realRemark = if (Strings.isNullOrEmpty(remark)) psiClass!!.name!!
                    .trim { it <= ' ' } else remark + " ," + psiClass!!.name!!.trim { it <= ' ' }
                arrays[JSON_DESCRIPTION] = realRemark
                if (pName != psiClass.name) {
                    val requiredList: MutableList<String> = ArrayList()
                    arrays[JSON_PROPERTIES] = getFields(psiClass, project, childType, index, requiredList)
                    arrays[JSON_REQUIRED] = requiredList
                } else {
                    arrays[JSON_TYPE] = pName
                }
            }
            val collectMap: MutableMap<String, Any> = LinkedHashMap()
            collectMap[JSON_TYPE] = "array"
            val realRemark = if (Strings.isNullOrEmpty(remark)) psiClass!!.name!!
                .trim { it <= ' ' } else remark + " ," + psiClass!!.name!!.trim { it <= ' ' }
            collectMap[JSON_DESCRIPTION] = realRemark
            collectMap["items"] = arrays
            filedMap[name] = collectMap
        }

        private fun validSelected(selectedText: String?, project: Project?): Boolean {
            if (Strings.isNullOrEmpty(selectedText)) {
                val error =
                    notificationGroup!!.createNotification("please select method or class", NotificationType.ERROR)
                Notifications.Bus.notify(error, project)
                return true
            }
            return false
        }

        private fun dealAllMethod(project: Project?, selectedClass: PsiClass?, apiDtos: MutableList<ApiDto?>) {
            for (psiMethod in selectedClass!!.methods) {
                // 过滤私有方法
                if (!psiMethod.modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
                    val apiDto = actionPerformed(psiMethod, project)
                    apiDtos.add(apiDto)
                }
            }
        }

        private fun dealSelectMethod(
            selectedText: String?,
            project: Project?,
            selectedClass: PsiClass?,
            apiDtos: MutableList<ApiDto?>
        ) {
            for (psiMethod in selectedClass!!.methods) {
                if (!psiMethod.modifierList.hasModifierProperty(PsiModifier.PRIVATE)
                    && StringUtils.equals(psiMethod.name, selectedText)
                ) {
                    val apiDto = actionPerformed(psiMethod, project)
                    apiDtos.add(apiDto)
                    break
                }
            }
        }
    }
}
