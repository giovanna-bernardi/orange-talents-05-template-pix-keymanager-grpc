package br.com.zupacademy.giovanna.pix.lista

import br.com.zupacademy.giovanna.ListaChavePixRequest
import br.com.zupacademy.giovanna.PixKeyListManagerServiceGrpc
import br.com.zupacademy.giovanna.PixKeyRegistrationManagerServiceGrpc
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import br.com.zupacademy.giovanna.pix.TipoChave
import br.com.zupacademy.giovanna.util.PixKeyGenerator
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class ListaChavePixEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: PixKeyListManagerServiceGrpc.PixKeyListManagerServiceBlockingStub
) {

    @BeforeEach
    internal fun setUp() {
        repository.save(
            PixKeyGenerator(tipoChave = TipoChave.EMAIL, valorChave = "yuri.oliveira@zup.com.br").generateKey()
        )
        repository.save(
            PixKeyGenerator(tipoChave = TipoChave.CPF, valorChave = "63657520325").generateKey()
        )
        repository.save(
            PixKeyGenerator(
                tipoChave = TipoChave.ALEATORIA,
                valorChave = "e46c15df-3af2-4b0d-bda9-16b348e3d4cf"
            ).generateKey()
        )
        repository.save(
            PixKeyGenerator(tipoChave = TipoChave.CELULAR, valorChave = "+5599999999999").generateKey()
        )
    }

    @Test
    fun `deve listar todas as chaves do cliente`() {
        // ação
        val response = grpcClient.lista(
            ListaChavePixRequest.newBuilder()
                .setClienteId(PixKeyGenerator.CLIENTE_ID.toString())
                .build()
        )

        // validação
        with(response) {
            MatcherAssert.assertThat(this.chavesList, hasSize(4))
            MatcherAssert.assertThat(
                this.chavesList.map { Pair(it.tipoChave, it.valorChave) }.toList(),
                containsInAnyOrder(
                    Pair(br.com.zupacademy.giovanna.TipoChave.EMAIL, "yuri.oliveira@zup.com.br"),
                    Pair(br.com.zupacademy.giovanna.TipoChave.CPF, "63657520325"),
                    Pair(br.com.zupacademy.giovanna.TipoChave.ALEATORIA, "e46c15df-3af2-4b0d-bda9-16b348e3d4cf"),
                    Pair(br.com.zupacademy.giovanna.TipoChave.CELULAR, "+5599999999999")
                )
            )
        }
    }

    @Test
    fun `deve retornar uma colecao vazia se cliente nao possuir chaves`() {
        // ação
        val response = grpcClient.lista(ListaChavePixRequest.newBuilder()
            .setClienteId(UUID.randomUUID().toString()).build())

        // validação
        assertTrue(response.chavesList.isEmpty())
    }

    @Test
    fun `nao deve listar chaves quando cliente nao encontrado ou com id invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.lista(ListaChavePixRequest.newBuilder()
                .build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("ClienteId não pode ser nulo nem vazio", status.description)
        }
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Factory
    class CLients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PixKeyListManagerServiceGrpc.PixKeyListManagerServiceBlockingStub {
            return PixKeyListManagerServiceGrpc.newBlockingStub(channel)
        }
    }
}