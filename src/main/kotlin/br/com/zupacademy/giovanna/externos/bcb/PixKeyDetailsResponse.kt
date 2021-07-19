package br.com.zupacademy.giovanna.externos.bcb

import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.pix.Instituicoes
import br.com.zupacademy.giovanna.pix.TipoConta
import br.com.zupacademy.giovanna.pix.consulta.DetalheChaveResponse
import java.time.LocalDateTime

data class PixKeyDetailsResponse(
    val keyType: PixKeyType,
    val key: String,
    val bankAccount: BankAccountResponse,
    val owner: OwnerResponse,
    val createdAt: LocalDateTime
) {

    fun toDetalheChaveResponse(): DetalheChaveResponse {
        return DetalheChaveResponse(
            tipoChave = keyType.domainType!!,
            valorChave = this.key,
            tipoConta = when (this.bankAccount.accountType) {
                BankAccount.AccountType.CACC -> TipoConta.CONTA_CORRENTE
                BankAccount.AccountType.SVGS -> TipoConta.CONTA_POUPANCA
            },
            conta = ContaEntity(
                nomeInstituicao = Instituicoes.nome(bankAccount.participant),
                nomeTitular = owner.name,
                cpfTitular = owner.taxIdNumber,
                agencia = bankAccount.branch,
                numeroConta = bankAccount.accountNumber
            ),
            dataCadastro = createdAt
        )
    }


    data class BankAccountResponse(
        val participant: String,
        val branch: String,
        val accountNumber: String,
        val accountType: BankAccount.AccountType
    )

    data class OwnerResponse(
        val type: Owner.OwnerType,
        val name: String,
        val taxIdNumber: String
    )
}
