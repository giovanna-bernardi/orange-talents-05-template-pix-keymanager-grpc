package br.com.zupacademy.giovanna.pix

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

// Teste de Unidade, não precisa subir nada, deve ser independente

internal class TipoChaveTest {

    @Nested
    inner class CPF {

        // deve ser obrigatório esse formato (ex: 12345678901)
        @Test
        fun `deve ser valido se tipo CPF e chave tiver CPF valido`() {
            with(TipoChave.CPF) {
                assertTrue(valida("86135457004"))
            }
        }

        @Test
        fun `nao deve ser valido se tipo CPF e chave for nula ou vazia`() {
            with(TipoChave.CPF) {
                assertFalse(valida(null))
                assertFalse(valida(""))
            }
        }

        @Test
        fun `nao deve ser valido se tipo CPF e chave tiver algo diferente de numeros`() {
            with(TipoChave.CPF) {
                assertFalse(valida("861.354.570-04"))
                assertFalse(valida("861A354b570-04"))
            }
        }

        @Test
        fun `nao deve ser valido se tipo CPF e chave tiver mais que onze numeros`() {
            with(TipoChave.CPF) {
                assertFalse(valida("861354570049"))
                assertFalse(valida("11111111113"))
            }
        }

        @Test
        fun `nao deve ser valido se tipo CPF e chave for um CPF invalido`() {
            with(TipoChave.CPF) {
                assertFalse(valida("11111111113"))
            }
        }
    }


    @Nested
    inner class CELULAR {

        // deve ser obrigatório esse formato (ex: +5585988714077)
        @Test
        fun `deve ser valido se tipo CELULAR e chave tiver numero valido`() {
            with(TipoChave.CELULAR) {
                assertTrue(valida("+5535999999999"))
            }
        }

        @Test
        fun `nao deve ser valido se tipo CELULAR e chave for nula ou vazia`() {
            with(TipoChave.CELULAR) {
                assertFalse(valida(null))
                assertFalse(valida(""))
            }
        }

        @Test
        fun `nao deve ser valido se tipo CELULAR e chave tiver valor invalido`() {
            with(TipoChave.CELULAR) {
                assertFalse(valida("35999999999"))
                assertFalse(valida("5535999999999"))
                assertFalse(valida("+55a3599999999"))
            }
        }
    }


    @Nested
    inner class EMAIL {
        @Test
        fun `deve ser valido se tipo EMAIL e chave for valida`() {
            with(TipoChave.EMAIL) {
                assertTrue(valida("teste@teste.com"))
            }
        }

        @Test
        fun `nao deve ser valido se tipo EMAIL e chave for nula ou vazia`() {
            with(TipoChave.EMAIL) {
                assertFalse(valida(null))
                assertFalse(valida(""))
            }
        }

        @Test
        fun `nao deve ser valido se tipo EMAIL e chave for endereco invalido`() {
            with(TipoChave.EMAIL) {
                assertFalse(valida("teste"))
                assertFalse(valida("teste.teste.com.br"))
                assertFalse(valida("teste.teste@com."))
                assertFalse(valida("teste.@teste.com"))
            }
        }

    }

    @Nested
    inner class ALEATORIA {
        @Test
        fun `deve ser valido se tipo ALEATORIA e chave for nula ou vazia`() {
            with(TipoChave.ALEATORIA) {
                assertTrue(valida(null))
                assertTrue(valida(""))
            }
        }

        @Test
        fun `nao deve ser valido se tipo ALEATORIA e chave preenchida`() {
            with(TipoChave.ALEATORIA) {
                assertFalse(valida("teste"))
            }
        }
    }
}