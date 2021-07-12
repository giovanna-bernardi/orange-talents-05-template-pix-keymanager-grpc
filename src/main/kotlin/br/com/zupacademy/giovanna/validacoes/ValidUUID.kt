package br.com.zupacademy.giovanna.validacoes

import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Pattern
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

@ReportAsSingleViolation
@MustBeDocumented
@Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\$", flags = [Pattern.Flag.CASE_INSENSITIVE])
@Target(FIELD, CONSTRUCTOR, PROPERTY, VALUE_PARAMETER)
@Retention(RUNTIME)
@Constraint(validatedBy = [])
annotation class ValidUUID(
    val message: String = "Formato de UUID inválido",
    val groups: Array<KClass<Any>> = [],
    val payload: Array<KClass<Payload>> = [],
)
