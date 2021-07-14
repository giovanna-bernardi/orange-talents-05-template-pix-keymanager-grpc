package br.com.zupacademy.giovanna.externos.bcb

import br.com.zupacademy.giovanna.pix.TipoChave
import java.time.LocalDateTime

data class CreatePixKeyResponse(
  val keyType: TipoChave,
  val key: String,
  val bankAccount: BankAccountResponse,
  val owner: OwnerResponse,
  val createdAt: LocalDateTime

)

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