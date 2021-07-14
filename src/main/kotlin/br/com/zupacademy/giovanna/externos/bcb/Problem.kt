package br.com.zupacademy.giovanna.externos.bcb

data class Problem(
    val type: String,
    val status: Int,
    val title: String,
    val detai: String,
    val violations: Set<Violation>,
)

data class Violation(
    val field: String,
    val message: String
)