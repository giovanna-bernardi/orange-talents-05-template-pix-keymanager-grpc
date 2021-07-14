package br.com.zupacademy.giovanna.externos.bcb

import br.com.zupacademy.giovanna.conta.ContaEntity
import java.time.LocalDateTime

data class DeletePixKeyResponse(
    val key: String,
    val participant: String,
    val deletedAt: LocalDateTime
)
