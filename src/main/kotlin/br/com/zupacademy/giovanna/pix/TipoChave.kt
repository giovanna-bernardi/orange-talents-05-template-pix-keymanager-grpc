package br.com.zupacademy.giovanna.pix

import br.com.caelum.stella.validation.CPFValidator
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator

enum class TipoChave {
    CPF{
        override fun valida(chave: String?): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }

            // deve ser obrigatório esse formato (ex: 12345678901)
            if (!chave.matches("^[0-9]{11}\$".toRegex())) {
                return false
            }

            return CPFValidator(false).invalidMessagesFor(chave).isEmpty()
        }
    },

    CELULAR{
        override fun valida(chave: String?): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }

            // deve ser obrigatório esse formato (ex: +5585988714077)
            return chave.matches("^\\+[1-9][0-9]\\d{1,14}\$".toRegex())
        }
    },

    EMAIL{
        override fun valida(chave: String?): Boolean {
            if (chave.isNullOrBlank()) {
                return false
            }

            return EmailValidator().run {
                initialize(null)
                isValid(chave, null)
            }
        }
    },

    ALEATORIA{
        override fun valida(chave: String?): Boolean = chave.isNullOrBlank() // deve estar vazia, o sistema deve gerar um UUID
    };

    abstract fun valida(chave: String?): Boolean
}