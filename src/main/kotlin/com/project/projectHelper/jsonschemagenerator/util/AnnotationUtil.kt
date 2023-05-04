package com.project.projectHelper.jsonschemagenerator.util

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.impl.source.SourceJavaCodeReference
import org.apache.commons.lang3.ObjectUtils

/**
 * @author 40696
 */
object AnnotationUtil {
    /**
     * 用于查找特定全限定名的注解
     *
     * @param psiModifierListOwner
     * @param annotationFqn
     * @return
     */
    fun findAnnotation(psiModifierListOwner: PsiModifierListOwner, annotationFqn: String): PsiAnnotation? {
        // 从psiModifierListOwner中获取注解所有者（PsiAnnotationOwner）
        val annotationOwner: PsiAnnotationOwner? = psiModifierListOwner.modifierList
        // 从注解所有者中获取所有注解
        val annotations = annotationOwner!!.annotations
        // 如果注解所有者为空，则返回null
        if (annotations.isEmpty()) {
            return null
        }
        // 获取注解全限定名的短名称（即类名）
        val shortName = StringUtil.getShortName(annotationFqn)
        // 遍历所有注解
        for (annotation in annotations) {
            // 获取注解的名称引用元素
            val referenceElement = annotation.nameReferenceElement
            // 如果引用元素不为null
            if (ObjectUtils.isNotEmpty(referenceElement)) {
                // 获取引用名称
                val referenceName = referenceElement!!.referenceName
                // 如果短名称与引用名称相同
                if (shortName == referenceName) {
                    // 如果引用元素是限定的且为SourceJavaCodeReference类型
                    if (referenceElement.isQualified && referenceElement is SourceJavaCodeReference) {
                        // 获取可能的全限定名
                        val possibleFullQualifiedName = (referenceElement as SourceJavaCodeReference).classNameText
                        // 如果全限定名与可能的全限定名相同，则返回此注解
                        if (annotationFqn == possibleFullQualifiedName) {
                            return annotation
                        }
                    }
                    // 获取注解的全限定名
                    val annotationQualifiedName = annotation.qualifiedName
                    // 如果注解全限定名不为空且与给定的全限定名相同，则返回此注解
                    if (null != annotationQualifiedName && annotationFqn.endsWith(annotationQualifiedName)) {
                        return annotation
                    }
                }
            }
        }
        // 如果没有找到匹配的注解，则返回null
        return null
    }
}
