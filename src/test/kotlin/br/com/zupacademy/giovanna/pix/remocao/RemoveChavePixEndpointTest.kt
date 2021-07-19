package br.com.zupacademy.giovanna.pix.remocao

import br.com.zupacademy.giovanna.PixKeyExclusionManagerServiceGrpc
import br.com.zupacademy.giovanna.RemoveChavePixRequest
import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.externos.bcb.BcbClient
import br.com.zupacademy.giovanna.externos.bcb.DeletePixKeyRequest
import br.com.zupacademy.giovanna.externos.bcb.DeletePixKeyResponse
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import br.com.zupacademy.giovanna.pix.TipoChave
import br.com.zupacademy.giovanna.util.PixKeyGenerator
import br.com.zupacademy.giovanna.util.violations
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RemoveChavePixEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: PixKeyExclusionManagerServiceGrpc.PixKeyExclusionManagerServiceBlockingStub
) {

    @Inject
    lateinit var bcbClient: BcbClient

    private val chaveFake = PixKeyGenerator(
        tipoChave = TipoChave.CPF,
        valorChave = "86135457004"
    ).generateKey()

    @BeforeEach
    internal fun setUp() {
        repository.deleteAll()
        repository.save(chaveFake)
    }

    /* Happy path :) */

    @Test
    fun `deve excluir chave quando chave existir e pertencer ao cliente`() {
        // cenário
        Mockito.`when`(bcbClient.delete(key = "86135457004", request = deletePixKeyRequestFake()))
            .thenReturn(HttpResponse.ok(deletePixKeyResponseFake()))

        // ação
        val response = grpcClient.remove(
            RemoveChavePixRequest.newBuilder()
                .setClienteId(chaveFake.clienteId.toString())
                .setPixId(chaveFake.id.toString())
                .build()
        )

        // validação
        with(response) {
            assertTrue(removido)
            assertTrue(repository.findById(chaveFake.id!!).isEmpty)
        }
    }

    @Test
    fun `nao deve excluir chave quando chave nao encontrada`() {
        // cenário
        val pixIdInexistente = UUID.randomUUID().toString()

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(
                RemoveChavePixRequest.newBuilder()
                    .setClienteId(chaveFake.clienteId.toString())
                    .setPixId(pixIdInexistente)
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada para este cliente", status.description)
        }
    }

    @Test
    fun `nao deve excluir chave quando chave nao pertence ao cliente`() {
        // cenário
        val clienteDiferenteId = UUID.randomUUID().toString()

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(
                RemoveChavePixRequest.newBuilder()
                    .setClienteId(clienteDiferenteId)
                    .setPixId(chaveFake.id.toString())
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada para este cliente", status.description)
        }
    }

    @Test
    fun `nao deve excluir chave quando os parametros forem invalidos`() {

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(
                RemoveChavePixRequest.newBuilder()
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            MatcherAssert.assertThat(
                violations(), Matchers.containsInAnyOrder(
                    Pair("clienteId", "não deve estar em branco"),
                    Pair("clienteId", "clienteId com formato inválido"),
                    Pair("pixId", "não deve estar em branco"),
                    Pair("pixId", "pixId com formato inválido")
                )
            )
        }
    }

    @Test
    fun `nao deve excluir chave quando proibido deletar no sistema externo`() {
        // cenário
        Mockito.`when`(bcbClient.delete(key = "86135457004", request = deletePixKeyRequestFake()))
            .thenThrow(
                HttpClientResponseException(
                    "Proibido realizar operação",
                    HttpResponse.status<HttpStatus>(HttpStatus.FORBIDDEN)
                )
            )

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(
                RemoveChavePixRequest.newBuilder()
                    .setClienteId(chaveFake.clienteId.toString())
                    .setPixId(chaveFake.id.toString())
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertTrue(repository.existsByValorChave(chaveFake.valorChave))
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Proibido realizar operação no Banco Central do Brasil (BCB)", status.description)
        }
    }

    @Test
    fun `deve excluir chave quando chave nao encontrada no sistema externo`() {
        // cenário
        Mockito.`when`(bcbClient.delete(key = "86135457004", request = deletePixKeyRequestFake()))
            .thenThrow(
                HttpClientResponseException(
                    "Chave pix não encontrada",
                    HttpResponse.status<HttpStatus>(HttpStatus.NOT_FOUND)
                )
            )

        // ação
        grpcClient.remove(
            RemoveChavePixRequest.newBuilder()
                .setClienteId(chaveFake.clienteId.toString())
                .setPixId(chaveFake.id.toString())
                .build()
        )

        // validação
        assertFalse(repository.existsById(chaveFake.id!!))

    }

    @Test
    fun `nao deve excluir chave quando excecao inesperada no sistema externo`() {
        // cenário
        Mockito.`when`(bcbClient.delete(key = "86135457004", request = deletePixKeyRequestFake()))
            .thenThrow(
                HttpClientResponseException(
                    "Erro inesperado",
                    HttpResponse.status<HttpStatus>(HttpStatus.EXPECTATION_FAILED)
                )
            )

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.remove(
                RemoveChavePixRequest.newBuilder()
                    .setClienteId(chaveFake.clienteId.toString())
                    .setPixId(chaveFake.id.toString())
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals("Erro ao remover chave Pix no Banco Central do Brasil (BCB)", status.description)
        }
    }


    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return Mockito.mock(BcbClient::class.java)
    }

    // Criando um Client para consumir a resposta do endpoint gRPC
    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PixKeyExclusionManagerServiceGrpc.PixKeyExclusionManagerServiceBlockingStub {
            return PixKeyExclusionManagerServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun deletePixKeyRequestFake() = DeletePixKeyRequest(key = "86135457004")

    private fun deletePixKeyResponseFake(): DeletePixKeyResponse {
        return DeletePixKeyResponse(
            key = "86135457004",
            participant = ContaEntity.ITAU_UNIBANCO_ISPB,
            deletedAt = LocalDateTime.now()
        )
    }
}