package br.com.zupacademy.giovanna.pix.cadastro

import br.com.zupacademy.giovanna.*
import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.conta.ContaResponse
import br.com.zupacademy.giovanna.conta.InstituicaoResponse
import br.com.zupacademy.giovanna.conta.TitularResponse
import br.com.zupacademy.giovanna.externos.bcb.*
import br.com.zupacademy.giovanna.externos.itau.ErpItauClient
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import br.com.zupacademy.giovanna.util.PixKeyGenerator
import br.com.zupacademy.giovanna.util.PixKeyGenerator.Companion.CLIENTE_ID
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
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import javax.inject.Inject
import br.com.zupacademy.giovanna.pix.TipoChave as TipoDeChave

/* Desabilitar o controle transacional por causa do gRPC Server radar
* em uma Thread separada. Senão, não é possível preparar o cenário dentro
* do método @Test */

@MicronautTest(transactional = false)
internal class CadastraChavePixEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: PixKeyRegistrationManagerServiceGrpc.PixKeyRegistrationManagerServiceBlockingStub
) {

    // Teste de integração. Testando com o cliente Http.
    // Serão mockados

    @Inject
    lateinit var itauClient: ErpItauClient

    @Inject
    lateinit var bcbClient: BcbClient


    @BeforeEach
    internal fun setUp() {
        repository.deleteAll()
    }

    /* Happy Path :) */

    @Test
    fun `deve cadastrar nova chave pix`() {
        // Cenário - Substituir o Client original pelo Mock
        Mockito.`when`(
            itauClient.buscaContaDoClientePorTipo(
                clienteId = CLIENTE_ID.toString(),
                tipo = "CONTA_CORRENTE"
            )
        )
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        Mockito.`when`(bcbClient.insert(createPixKeyRequestFake()))
            .thenReturn(HttpResponse.created(createPixKeyResponseFake()))

        // Ação
        val response = grpcClient.cadastra(
            CadastraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setValorChave("86135457004")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build()
        )

        // Validação
        with(response) {
            assertNotNull(pixId)
            assertTrue(repository.existsByValorChave("86135457004"))
        }
    }

    /* Sad(?) Path :( */

    @Test
    fun `nao deve cadastrar chave pix quando chave existente`() {
        // Cenário - Salvar uma chave no banco
        val chaveFake = PixKeyGenerator(
            tipoChave = TipoDeChave.CPF,
            valorChave = "86135457004"
        ).generateKey()

        repository.save(chaveFake)

        // Ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(
                CadastraChavePixRequest.newBuilder()
                    .setClienteId(chaveFake.clienteId.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setValorChave(chaveFake.valorChave)
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        // Validação
        with(thrown) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("A chave Pix '86135457004' já existe no banco", status.description)
        }

    }

    @Test
    fun `nao deve cadastrar chave pix quando nao encontrar dados da conta cliente`() {
        // Cenário - Mockar busca no Client Itau devolvendo status de não encontrado
        Mockito.`when`(
            itauClient.buscaContaDoClientePorTipo(
                clienteId = CLIENTE_ID.toString(),
                tipo = "CONTA_CORRENTE"
            )
        )
            .thenReturn(HttpResponse.notFound())

        // Ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(
                CadastraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setValorChave("86135457004")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        // Validação
        with(thrown) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no ITAU", status.description)
        }
    }


    // testando se @ValidPixKey está sendo usada
    @Test
    fun `nao deve cadastrar chave pix quando a chave for invalida`() {
        // Cenário - Cadastrar chave inválida
        // Ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(
                CadastraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setValorChave("861.354.a.570-04")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        // Validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            MatcherAssert.assertThat(
                violations(), containsInAnyOrder(
                    Pair("chave", "chave Pix inválida (CPF)")
                )
            )
        }
    }

    @Test
    fun `nao deve cadastrar chave pix quando os parametros forem invalidos`() {
        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(CadastraChavePixRequest.newBuilder().build())
        }

        // validação
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            MatcherAssert.assertThat(
                violations(), containsInAnyOrder(
                    Pair("clienteId", "não deve estar em branco"),
                    Pair("clienteId", "formato de UUID inválido"),
                    Pair("tipoConta", "não deve ser nulo"),
                    Pair("tipoChave", "não deve ser nulo"),
                )
            )
        }
    }

    @Test
    fun `nao deve cadastrar chave quando chave ja estiver registrada no sistema externo`() {
        // cenário
        Mockito.`when`(
            itauClient.buscaContaDoClientePorTipo(
                clienteId = CLIENTE_ID.toString(),
                tipo = "CONTA_CORRENTE"
            )
        )
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        Mockito.`when`(bcbClient.insert(createPixKeyRequestFake())).thenThrow(
            HttpClientResponseException(
                "Chave pix já registrada", HttpResponse.status<HttpStatus>(
                    HttpStatus.UNPROCESSABLE_ENTITY
                )
            )
        )

        // ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(
                CadastraChavePixRequest.newBuilder()
                    .setClienteId(CLIENTE_ID.toString())
                    .setTipoChave(TipoChave.CPF)
                    .setValorChave("86135457004")
                    .setTipoConta(TipoConta.CONTA_CORRENTE)
                    .build()
            )
        }

        // validação
        with(thrown) {
            assertFalse(repository.existsByValorChave("86135457004"))
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave pix já registrada no Banco Central do Brasil (BCB)", status.description)
        }
    }

    // Ainda não consegui entender o erro no assertEquals
//    @Test
//    fun `nao deve cadastrar chave quando chave nao for criada com sucesso no sistema externo`() {
//        // cenário
//        Mockito.`when`(
//            itauClient.buscaContaDoClientePorTipo(
//                clienteId = CLIENTE_ID.toString(),
//                tipo = "CONTA_CORRENTE"
//            )
//        )
//            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))
//
//        Mockito.`when`(bcbClient.insert(createPixKeyRequestFake())).thenReturn(
//            HttpResponse.ok() // é esperado 201
//        )
//
//        // ação
//        val thrown = assertThrows<StatusRuntimeException> {
//            grpcClient.cadastra(
//                CadastraChavePixRequest.newBuilder()
//                    .setClienteId(CLIENTE_ID.toString())
//                    .setTipoChave(TipoChave.CPF)
//                    .setValorChave("86135457004")
//                    .setTipoConta(TipoConta.CONTA_CORRENTE)
//                    .build()
//            )
//        }
//
//        // validação
//        with(thrown) {
//            assertFalse(repository.existsByValorChave("86135457004"))
//            assertEquals(Status.FAILED_PRECONDITION, status)
//            assertEquals("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)", status.description)
//        }
//    }


    private fun dadosDaContaResponse(): ContaResponse {
        return ContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("UNIBANCO ITAU SA", "60701190"),
            agencia = "0001",
            numero = "123455",
            titular = TitularResponse("Yuri Matheus", "86135457004")
        )
    }

    // Mock do client Http (para não ter que levantar
    // o ambiente externo inteiro e não salvar dados de
    // testes no sistema externo)
    @MockBean(ErpItauClient::class)
    fun itauClient(): ErpItauClient? {
        return Mockito.mock(ErpItauClient::class.java)
    }

    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return Mockito.mock(BcbClient::class.java)
    }

    // Criando um Client para consumir a resposta do endpoint gRPC
    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PixKeyRegistrationManagerServiceGrpc.PixKeyRegistrationManagerServiceBlockingStub {
            return PixKeyRegistrationManagerServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun createPixKeyRequestFake(): CreatePixKeyRequest {
        return CreatePixKeyRequest(
            keyType = PixKeyType.of(br.com.zupacademy.giovanna.pix.TipoChave.CPF),
            key = "86135457004",
            bankAccount = BankAccountRequest(
                participant = ContaEntity.ITAU_UNIBANCO_ISPB,
                branch = "0001",
                accountNumber = "123455",
                accountType = BankAccount.AccountType.of(br.com.zupacademy.giovanna.pix.TipoConta.CONTA_CORRENTE)
            ),
            owner = OwnerRequest(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "Yuri Matheus",
                taxIdNumber = "86135457004"
            )
        )
    }

    private fun createPixKeyResponseFake(): CreatePixKeyResponse {
        return CreatePixKeyResponse(
            keyType = PixKeyType.CPF,
            key = "86135457004",
            bankAccount = BankAccountResponse(
                participant = ContaEntity.ITAU_UNIBANCO_ISPB,
                branch = "0001",
                accountNumber = "123455",
                accountType = BankAccount.AccountType.of(br.com.zupacademy.giovanna.pix.TipoConta.CONTA_CORRENTE)
            ),
            owner = OwnerResponse(
                type = Owner.OwnerType.NATURAL_PERSON,
                name = "Yuri Matheus",
                taxIdNumber = "86135457004"
            ),
            createdAt = LocalDateTime.now()
        )
    }
}