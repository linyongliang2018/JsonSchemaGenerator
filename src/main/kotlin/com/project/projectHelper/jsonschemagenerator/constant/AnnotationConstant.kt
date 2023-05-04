package com.project.projectHelper.jsonschemagenerator.constant

import org.springframework.web.bind.annotation.RequestBody
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

/**
 * 注解常量
 *
 */
val NOT_NULL = NotNull::class.java.name
val NOT_EMPTY = NotEmpty::class.java.name
val NOT_BLANK = NotBlank::class.java.name
val REQUEST_BODY = RequestBody::class.java.name
