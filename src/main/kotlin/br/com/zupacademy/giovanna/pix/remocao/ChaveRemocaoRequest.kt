package br.com.zupacademy.giovanna.pix.remocao

import br.com.zupacademy.giovanna.validacoes.ValidUUID
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

@Introspected
data class ChaveRemocaoRequest(
    @field:ValidUUID(message = "pixId com formato inválido")
    @field:NotBlank
    val pixId: String?,

    @field:ValidUUID(message = "clienteId com formato inválido")
    @field:NotBlank
    val clienteId: String?
) {
}