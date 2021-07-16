package br.com.zupacademy.giovanna.util

import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.pix.ChavePixEntity
import br.com.zupacademy.giovanna.pix.TipoChave
import br.com.zupacademy.giovanna.pix.TipoConta
import java.util.*

data class KeyGenerator(
    var clienteId: UUID = CLIENTE_ID,
    var tipoChave: TipoChave = TipoChave.EMAIL,
    var valorChave: String = "teste@teste.com.br",
    var tipoConta: TipoConta = TipoConta.CONTA_CORRENTE,
    var conta: ContaEntity? = null
) {
    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    fun geraChave(): ChavePixEntity {
        return ChavePixEntity(
            clienteId = clienteId,
            tipoChave = tipoChave,
            valorChave = valorChave,
            tipoConta = tipoConta,
            conta = ContaEntity(
                nomeInstituicao = "UNIBANCO ITAU SA",
                nomeTitular = "Yuri Matheus",
                cpfTitular = "86135457004",
                agencia = "0001",
                numeroConta = "123455"
            )
        )
    }
}