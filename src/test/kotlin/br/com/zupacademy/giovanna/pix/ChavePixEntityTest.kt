package br.com.zupacademy.giovanna.pix

import br.com.zupacademy.giovanna.util.PixKeyGenerator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

internal class ChavePixEntityTest {

    @Test
    fun `deve alterar o valor da chave quando ela for do tipo ALEATORIA`() {
        val chaveFake = PixKeyGenerator(
            tipoChave = TipoChave.ALEATORIA,
            valorChave = UUID.randomUUID().toString()
        ).generateKey()

        with(chaveFake) {
            val oldValue = chaveFake.valorChave
            assertTrue(updateKeyValue(UUID.randomUUID().toString()))
            assertNotEquals(oldValue, chaveFake.valorChave)
        }
    }

    @Test
    fun `nao deve alterar o valor da chave quando ela nao for do tipo ALEATORIA`() {
        val chaveFake = PixKeyGenerator(
            tipoChave = TipoChave.CPF,
            valorChave = "86135457004"
        ).generateKey()

        with(chaveFake) {
            val oldValue = chaveFake.valorChave
            assertFalse(updateKeyValue(UUID.randomUUID().toString()))
            assertEquals(oldValue, chaveFake.valorChave)
        }
    }


    @Test
    fun `deve retornar true se a chave for do tipo ALEATORIA`() {
        val chaveFake = PixKeyGenerator(
            tipoChave = TipoChave.ALEATORIA,
            valorChave = UUID.randomUUID().toString()
        ).generateKey()

        with(chaveFake) {
            assertTrue(isRandom())
        }
    }

    @Test
    fun `deve retornar false se a chave nao for do tipo ALEATORIA`() {
        val chaveFake = PixKeyGenerator().generateKey()

        with(chaveFake) {
            assertFalse(isRandom())
        }
    }

    @Test
    fun `deve retornar true se a chave pertence ao cliente`() {
        val cliente = UUID.randomUUID()
        val chaveFake = PixKeyGenerator(
            clienteId = cliente
        ).generateKey()

        assertTrue(chaveFake.belongsTo(cliente))
    }

    @Test
    fun `deve retornar false se a chave nao pertence ao cliente`() {
        val cliente = UUID.randomUUID()
        val chaveFake = PixKeyGenerator().generateKey()

        assertFalse(chaveFake.belongsTo(cliente))
    }
}

