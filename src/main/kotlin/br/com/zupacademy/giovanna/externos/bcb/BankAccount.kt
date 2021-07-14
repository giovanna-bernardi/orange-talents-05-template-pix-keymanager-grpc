package br.com.zupacademy.giovanna.externos.bcb

import br.com.zupacademy.giovanna.pix.TipoConta

data class BankAccount(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountType
) {

    // Tipos encontrados no swagger http://localhost:8082/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config#/
    enum class AccountType {
        CACC, // conta corrente
        SVGS; // conta poupança

        companion object {
            fun of(domainType: TipoConta) : AccountType {
                return when (domainType) {
                    TipoConta.CONTA_CORRENTE -> CACC
                    TipoConta.CONTA_POUPANCA -> SVGS
                }
            }
        }
    }
}