package br.com.zupacademy.giovanna.pix.remocao

import br.com.zupacademy.giovanna.PixKeyExclusionManagerServiceGrpc
import br.com.zupacademy.giovanna.RemoveChavePixRequest
import br.com.zupacademy.giovanna.pix.TipoConta
import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.pix.ChavePixEntity
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import br.com.zupacademy.giovanna.pix.TipoChave
import br.com.zupacademy.giovanna.util.violations
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.annotation.TransactionMode
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

@MicronautTest(transactional = false)
internal class RemoveChavePixEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: PixKeyExclusionManagerServiceGrpc.PixKeyExclusionManagerServiceBlockingStub
) {

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

    val chave = chaveFake()

    @BeforeEach
    internal fun setUp() {
        repository.deleteAll()
        repository.save(chave)
    }

    /* Happy path :) */

    @Test
    fun `deve excluir chave quando chave existir e pertencer ao cliente`() {
        // ação
        val response = grpcClient.remove(
            RemoveChavePixRequest.newBuilder()
                .setClienteId(chave.clienteId.toString())
                .setPixId(chave.id.toString())
                .build()
        )

        // validação
        with(response) {
            assertTrue(removido)
            assertTrue(repository.findById(chave.id).isEmpty)
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
                    .setClienteId(chave.clienteId.toString())
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
                    .setPixId(chave.id.toString())
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

    private fun chaveFake(): ChavePixEntity {
        return ChavePixEntity(
            clienteId = CLIENTE_ID,
            tipoChave = TipoChave.CPF,
            valorChave = "86135457004",
            tipoConta = TipoConta.CONTA_CORRENTE,
            conta = ContaEntity(
                nomeInstituicao = "UNIBANCO ITAU SA",
                nomeTitular = "Yuri Matheus",
                cpfTitular = "86135457004",
                agencia = "0001",
                numeroConta = "123455"
            )
        )
    }

    // Criando um Client para consumir a resposta do endpoint gRPC
    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PixKeyExclusionManagerServiceGrpc.PixKeyExclusionManagerServiceBlockingStub {
            return PixKeyExclusionManagerServiceGrpc.newBlockingStub(channel)
        }
    }
}