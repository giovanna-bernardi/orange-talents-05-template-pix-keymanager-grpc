package br.com.zupacademy.giovanna.externos.bcb

import br.com.zupacademy.giovanna.pix.TipoChave
import java.lang.IllegalArgumentException

enum class PixKeyType(val domainType: TipoChave?) {
    CPF(TipoChave.CPF),
    CNPJ(null),
    PHONE(TipoChave.CELULAR),
    EMAIL(TipoChave.EMAIL),
    RANDOM(TipoChave.ALEATORIA);

    companion object {
        // prestar atenção para elementos iguais (aqui não vai ter problema)
        // associateWith : (chave: PixKeyType, valor: o que for passado na função)
        // associateBy : (chave: o que for passado na função, valor: PixKeyType)

        private val transformation = PixKeyType.values().associateBy(PixKeyType::domainType)

        fun of(domainType: TipoChave) : PixKeyType {
            return transformation[domainType] ?: throw IllegalArgumentException("PixKeyType invalid or not found for $domainType")
        }
    }
}
