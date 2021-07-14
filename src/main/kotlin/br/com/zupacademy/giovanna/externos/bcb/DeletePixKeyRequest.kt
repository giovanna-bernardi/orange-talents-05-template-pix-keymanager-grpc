package br.com.zupacademy.giovanna.externos.bcb

import br.com.zupacademy.giovanna.conta.ContaEntity

data class DeletePixKeyRequest(
    val key: String,
    val participant: String = ContaEntity.ITAU_UNIBANCO_ISPB
)