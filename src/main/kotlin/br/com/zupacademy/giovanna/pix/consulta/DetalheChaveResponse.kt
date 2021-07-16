package br.com.zupacademy.giovanna.pix.consulta

import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.pix.ChavePixEntity
import br.com.zupacademy.giovanna.pix.TipoChave
import br.com.zupacademy.giovanna.pix.TipoConta
import java.time.LocalDateTime
import java.util.*

data class DetalheChaveResponse(
    val pixId: UUID? = null, // pode ou não ser preenchido dependendo do tipo de consulta
    val clienteId: UUID? = null,
    val tipoChave: TipoChave,
    val valorChave: String,
    val tipoConta: TipoConta,
    val conta: ContaEntity,
    val dataCadastro: LocalDateTime
){

    companion object {
        fun of(chave: ChavePixEntity): DetalheChaveResponse {
            return DetalheChaveResponse(
                pixId = chave.id,
                clienteId = chave.clienteId,
                tipoChave = chave.tipoChave,
                valorChave = chave.valorChave,
                tipoConta = chave.tipoConta,
                conta = chave.conta,
                dataCadastro = chave.criadaEm
            )
        }
    }
}
