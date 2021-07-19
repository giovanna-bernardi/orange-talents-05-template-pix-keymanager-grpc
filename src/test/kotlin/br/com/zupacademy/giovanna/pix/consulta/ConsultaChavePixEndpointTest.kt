package br.com.zupacademy.giovanna.pix.consulta

import br.com.zupacademy.giovanna.DetalheChavePixRequest
import br.com.zupacademy.giovanna.PixKeyDetailManagerServiceGrpc
import br.com.zupacademy.giovanna.externos.bcb.*
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
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class ConsultaChavePixEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: PixKeyDetailManagerServiceGrpc.PixKeyDetailManagerServiceBlockingStub
) {

    @Inject
    lateinit var bcbClient: BcbClient

    @BeforeEach
    fun setup() {
        repository.save(
            PixKeyGenerator(
                tipoChave = TipoChave.EMAIL,
                valorChave = "yuri.oliveira@zup.com.br"
            ).generateKey()
        )
        repository.save(PixKeyGenerator(tipoChave = TipoChave.CPF, valorChave = "63657520325").generateKey())
        repository.save(
            PixKeyGenerator(
                tipoChave = TipoChave.ALEATORIA,
                valorChave = "e46c15df-3af2-4b0d-bda9-16b348e3d4cf"
            ).generateKey()
        )
        repository.save(PixKeyGenerator(tipoChave = TipoChave.CELULAR, valorChave = "+5599999999999").generateKey())
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PixKeyDetailManagerServiceGrpc.PixKeyDetailManagerServiceBlockingStub {
            return PixKeyDetailManagerServiceGrpc.newBlockingStub(channel)
        }
    }

    @Test
    fun `deve trazer a chave quando consultado com pixId e clienteId`() {
        // cenário
        val chaveExistente = repository.findByValorChave("yuri.oliveira@zup.com.br").get()

        // ação
        val response = grpcClient.consulta(
            DetalheChavePixRequest.newBuilder()
                .setSistemaInterno(
                    DetalheChavePixRequest.KeyManager.newBuilder()
                        .setPixId(chaveExistente.id.toString())
                        .setClienteId(chaveExistente.clienteId.toString())
                        .build()
                )
                .build()
        )

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), pixId.toString())
            assertEquals(chaveExistente.clienteId.toString(), clienteId.toString())
            assertEquals(chaveExistente.tipoChave.name, chave.tipoChave.name)
            assertEquals(chaveExistente.valorChave, chave.valorChave)
            assertEquals(chaveExistente.tipoConta.name, chave.conta.tipoConta.name)
            assertEquals(chaveExistente.conta.nomeTitular, chave.conta.nomeTitular)
            assertEquals(chaveExistente.conta.cpfTitular, chave.conta.cpfTitular)
            assertEquals(chaveExistente.conta.nomeInstituicao, chave.conta.instituicao)
            assertEquals(chaveExistente.conta.agencia, chave.conta.agencia)
            assertEquals(chaveExistente.conta.numeroConta, chave.conta.numeroConta)
        }
    }

    @Test
    fun `nao deve trazer a chave por pixId e clienteId quando request for invalido`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                DetalheChavePixRequest.newBuilder()
                    .setSistemaInterno(
                        DetalheChavePixRequest.KeyManager.newBuilder()
                            .setPixId("")
                            .setClienteId("")
                            .build()
                    )
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            MatcherAssert.assertThat(
                violations(), Matchers.containsInAnyOrder(
                    Pair("clienteId", "não deve estar em branco"),
                    Pair("clienteId", "formato de UUID inválido"),
                    Pair("pixId", "não deve estar em branco"),
                    Pair("pixId", "formato de UUID inválido")
                )
            )
        }
    }

    @Test
    fun `nao deve trazer uma chave por pixId e clienteId quando registro nao existir`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                DetalheChavePixRequest.newBuilder()
                    .setSistemaInterno(
                        DetalheChavePixRequest.KeyManager.newBuilder()
                            .setPixId(UUID.randomUUID().toString())
                            .setClienteId(UUID.randomUUID().toString())
                            .build()
                    )
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve trazer a chave por seu valor quando existir no banco`() {
        // cenário
        val chaveExistente = repository.findByValorChave("yuri.oliveira@zup.com.br").get()
        val detalheResponsePeloValorChave = DetalheChaveResponse.of(chaveExistente)

        // ação
        val response = grpcClient.consulta(
            DetalheChavePixRequest.newBuilder()
                .setValorChave(chaveExistente.valorChave)
                .build()
        )

        // validação
        with(response) {
            assertEquals(chaveExistente.id.toString(), pixId.toString())
            assertEquals(chaveExistente.clienteId.toString(), clienteId.toString())
            assertEquals(chaveExistente.tipoChave.name, chave.tipoChave.name)
            assertEquals(chaveExistente.valorChave, chave.valorChave)
            assertEquals(chaveExistente.tipoConta.name, chave.conta.tipoConta.name)
            assertEquals(chaveExistente.conta.nomeTitular, chave.conta.nomeTitular)
            assertEquals(chaveExistente.conta.cpfTitular, chave.conta.cpfTitular)
            assertEquals(chaveExistente.conta.nomeInstituicao, chave.conta.instituicao)
            assertEquals(chaveExistente.conta.agencia, chave.conta.agencia)
            assertEquals(chaveExistente.conta.numeroConta, chave.conta.numeroConta)

            assertEquals(detalheResponsePeloValorChave.pixId.toString(), pixId.toString())
            assertEquals(detalheResponsePeloValorChave.clienteId.toString(), clienteId.toString())
            assertEquals(detalheResponsePeloValorChave.tipoChave.name, chave.tipoChave.name)
            assertEquals(detalheResponsePeloValorChave.valorChave, chave.valorChave)
            assertEquals(detalheResponsePeloValorChave.tipoConta.name, chave.conta.tipoConta.name)
            assertEquals(detalheResponsePeloValorChave.conta.nomeTitular, chave.conta.nomeTitular)
            assertEquals(detalheResponsePeloValorChave.conta.cpfTitular, chave.conta.cpfTitular)
            assertEquals(detalheResponsePeloValorChave.conta.nomeInstituicao, chave.conta.instituicao)
            assertEquals(detalheResponsePeloValorChave.conta.agencia, chave.conta.agencia)
            assertEquals(detalheResponsePeloValorChave.conta.numeroConta, chave.conta.numeroConta)
        }
    }

    @Test
    fun `deve trazer a chave por seu valor quando nao existir no banco mas existir no bcb`() {
        // cenário
        val detailsReponse = pixKeyDetailsResponse()
        val detalheResponse = detailsReponse.toDetalheChaveResponse()
        Mockito.`when`(bcbClient.findByKeyValue("teste@teste.com"))
            .thenReturn(HttpResponse.ok(detailsReponse))

        // ação
        val response = grpcClient.consulta(
            DetalheChavePixRequest.newBuilder()
                .setValorChave("teste@teste.com")
                .build()
        )

        // validação
        with(response) {
            assertEquals("", pixId.toString())
            assertEquals("", clienteId.toString())
            assertEquals(detalheResponse.tipoChave.name, chave.tipoChave.name)
            assertEquals(detalheResponse.valorChave, chave.valorChave)
            assertEquals(detalheResponse.tipoConta.name, chave.conta.tipoConta.name)
            assertEquals(detalheResponse.conta.nomeTitular, chave.conta.nomeTitular)
            assertEquals(detalheResponse.conta.cpfTitular, chave.conta.cpfTitular)
            assertEquals(detalheResponse.conta.nomeInstituicao, chave.conta.instituicao)
            assertEquals(detalheResponse.conta.agencia, chave.conta.agencia)
            assertEquals(detalheResponse.conta.numeroConta, chave.conta.numeroConta)
        }
    }

    @Test
    fun `nao deve trazer uma chave por seu valor quando registro nao existir nem localmente nem no bcb`() {
        // cenário
        Mockito.`when`(bcbClient.findByKeyValue("inexistente"))
            .thenReturn(HttpResponse.notFound())

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                DetalheChavePixRequest.newBuilder()
                    .setValorChave("inexistente")
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `nao deve trazer uma chave por seu valor quando request for invalida`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(
                DetalheChavePixRequest.newBuilder()
                    .setValorChave("")
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            MatcherAssert.assertThat(
                violations(), Matchers.containsInAnyOrder(
                    Pair("valorChave", "não deve estar em branco")
                )
            )
        }
    }

    @Test
    fun `nao deve trazer uma chave quando request for invalida`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consulta(DetalheChavePixRequest.newBuilder().build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
        }
    }

    private fun pixKeyDetailsResponse(): PixKeyDetailsResponse {
        return PixKeyDetailsResponse(
            keyType = PixKeyType.EMAIL,
            key = "teste@teste.com",
            bankAccount = PixKeyDetailsResponse.BankAccountResponse(
                participant = "90400888",
                branch = "0001",
                accountNumber = "123456",
                accountType = BankAccount.AccountType.SVGS
            ),
            owner = PixKeyDetailsResponse.OwnerResponse(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "Fulano de Tal",
                taxIdNumber = "11111111111"
            ),
            createdAt = LocalDateTime.now()
        )
    }
}
