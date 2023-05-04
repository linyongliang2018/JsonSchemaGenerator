package com.project.projectHelper.jsonschemagenerator.constant

import org.jetbrains.annotations.NonNls
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

/**
 * 类型常量
 */
object TypeConstant {
    /**
     * 基础类型
     */
    val BASE_TYPES: @NonNls MutableMap<String, Any> = HashMap()

    /**
     * 携带包名
     */
    val BASE_TYPES_BY_PACKAGES: MutableMap<String, Any> = HashMap()

    /**
     * 集合类型
     */
    val COLLECT_TYPES: MutableMap<String, Any> = HashMap()

    /**
     * 携带包名
     */
    val COLLECT_TYPES_BY_PACKAGES: MutableMap<String, Any> = HashMap()

    /**
     * 支持的泛型列表
     */
    val GENERIC_LIST: MutableList<String> = ArrayList()

    /**
     * 初始化 NORMAL_TYPES
     */
    init {
        BASE_TYPES!!["boolean"] = false
        BASE_TYPES["byte"] = 1
        BASE_TYPES["short"] = 1
        BASE_TYPES["int"] = 1
        BASE_TYPES["long"] = 1L
        BASE_TYPES["float"] = 1.0f
        BASE_TYPES["double"] = 1.0
        BASE_TYPES["char"] = 'a'
        BASE_TYPES["Boolean"] = false
        BASE_TYPES["Byte"] = 0
        BASE_TYPES["Short"] = 0.toShort()
        BASE_TYPES["Integer"] = 0
        BASE_TYPES["Long"] = 0L
        BASE_TYPES["Float"] = 0.0f
        BASE_TYPES["Double"] = 0.0
        BASE_TYPES["BigDecimal"] = 0
        BASE_TYPES["String"] = "String"
        BASE_TYPES["Date"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        BASE_TYPES["Timestamp"] = Timestamp(System.currentTimeMillis())
    }

    /**
     * 初始化 NORMAL_TYPES_PACKAGES
     */
    init {
        BASE_TYPES_BY_PACKAGES["java.lang.Boolean"] = false
        BASE_TYPES_BY_PACKAGES["java.lang.Byte"] = 0
        BASE_TYPES_BY_PACKAGES["java.lang.Short"] = 0.toShort()
        BASE_TYPES_BY_PACKAGES["java.lang.Integer"] = 1
        BASE_TYPES_BY_PACKAGES["java.lang.Long"] = 1L
        BASE_TYPES_BY_PACKAGES["java.lang.Float"] = 1L
        BASE_TYPES_BY_PACKAGES["java.lang.Double"] = 1.0
        BASE_TYPES_BY_PACKAGES["java.sql.Timestamp"] = Timestamp(System.currentTimeMillis())
        BASE_TYPES_BY_PACKAGES["java.util.Date"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        BASE_TYPES_BY_PACKAGES["java.lang.String"] = "String"
        BASE_TYPES_BY_PACKAGES["java.math.BigDecimal"] = 1
    }

    /**
     * 初始化 COLLECT_TYPES
     */
    init {
        COLLECT_TYPES["Map"] = "Map"
        COLLECT_TYPES["HashMap"] = "HashMap"
        COLLECT_TYPES["LinkedHashMap"] = "LinkedHashMap"
    }

    /**
     * 初始化 COLLECT_TYPES_PACKAGES
     */
    init {
        COLLECT_TYPES_BY_PACKAGES["java.util.Map"] = "Map"
        COLLECT_TYPES_BY_PACKAGES["java.util.HashMap"] = "HashMap"
        COLLECT_TYPES_BY_PACKAGES["java.util.LinkedHashMap"] = "LinkedHashMap"
    }

    /**
     * 初始化 GENERIC_LIST
     */
    init {
        GENERIC_LIST.add("T")
        GENERIC_LIST.add("E")
        GENERIC_LIST.add("A")
        GENERIC_LIST.add("B")
        GENERIC_LIST.add("K")
        GENERIC_LIST.add("V")
    }

    fun isNormalType(typeName: String): Boolean {
        return BASE_TYPES!!.containsKey(typeName)
    }
}