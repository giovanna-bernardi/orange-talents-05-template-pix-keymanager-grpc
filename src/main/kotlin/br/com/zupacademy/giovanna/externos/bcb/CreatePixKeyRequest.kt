package br.com.zupacademy.giovanna.externos.bcb

import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.pix.ChavePixEntity

data class CreatePixKeyRequest(
    val keyType: PixKeyType,
    val key: String,
    val bankAccount: BankAccountRequest,
    val owner: OwnerRequest
) {

    companion object {
        fun of(chave: ChavePixEntity): CreatePixKeyRequest {
            return CreatePixKeyRequest(
                keyType = PixKeyType.of(chave.tipoChave),
                key = chave.valorChave,
                bankAccount = BankAccountRequest(
                    participant = ContaEntity.ITAU_UNIBANCO_ISPB,
                    branch = chave.conta.agencia,
                    accountNumber = chave.conta.numeroConta,
                    accountType = BankAccount.AccountType.of(chave.tipoConta)
                ),
                owner = OwnerRequest(
                    type = Owner.OwnerType.NATURAL_PERSON,
                    name = chave.conta.nomeTitular,
                    taxIdNumber = chave.conta.cpfTitular
                )
            )
        }
    }
}

data class BankAccountRequest(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: BankAccount.AccountType
)

data class OwnerRequest(
    val type: Owner.OwnerType,
    val name: String,
    val taxIdNumber: String
)
