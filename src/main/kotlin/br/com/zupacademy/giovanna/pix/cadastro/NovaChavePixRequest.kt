package br.com.zupacademy.giovanna.pix.cadastro

import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.pix.ChavePixEntity
import br.com.zupacademy.giovanna.pix.TipoChave
import br.com.zupacademy.giovanna.pix.TipoConta
import br.com.zupacademy.giovanna.pix.validacoes.ValidPixKey
import br.com.zupacademy.giovanna.validacoes.ValidUUID
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@ValidPixKey
@Introspected
data class NovaChavePixRequest(
    @field:ValidUUID
    @field:NotBlank
    val clienteId: String?,

    @field:NotNull
    val tipoChave: TipoChave?,

    @field:Size(max = 77)
    val valorChave: String?,

    @field:NotNull
    val tipoConta: TipoConta?
){

    fun toModel(conta: ContaEntity) : ChavePixEntity {
        return ChavePixEntity(
            clienteId = UUID.fromString(this.clienteId),
            tipoChave = TipoChave.valueOf(this.tipoChave!!.name),
            valorChave = if(this.tipoChave == TipoChave.ALEATORIA) UUID.randomUUID().toString() else this.valorChave!!,
            tipoConta = TipoConta.valueOf(this.tipoConta!!.name),
            conta = conta
        )
    }
}
