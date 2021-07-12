package br.com.zupacademy.giovanna.compartilhado.handlers

import io.micronaut.aop.Around
import javax.validation.Constraint
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@MustBeDocumented
@Retention(RUNTIME)
@Target(CLASS, FIELD, TYPE)
@Around
annotation class ExceptionHandler
