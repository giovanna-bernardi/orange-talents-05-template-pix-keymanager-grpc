package br.com.zupacademy.giovanna.pix.cadastro

import br.com.zupacademy.giovanna.CadastraChavePixRequest
import br.com.zupacademy.giovanna.PixKeymanagerServiceGrpc
import br.com.zupacademy.giovanna.pix.TipoChave as TipoDeChave
import br.com.zupacademy.giovanna.TipoChave
import br.com.zupacademy.giovanna.TipoConta
import br.com.zupacademy.giovanna.conta.ContaEntity
import br.com.zupacademy.giovanna.conta.ContaResponse
import br.com.zupacademy.giovanna.conta.InstituicaoResponse
import br.com.zupacademy.giovanna.conta.TitularResponse
import br.com.zupacademy.giovanna.externos.itau.ErpItauClient
import br.com.zupacademy.giovanna.pix.ChavePixEntity
import br.com.zupacademy.giovanna.pix.ChavePixRepository
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
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.util.*
import javax.inject.Inject
import javax.validation.ConstraintViolationException

/* Desabilitar o controle transacional por causa do gRPC Server radar
* em uma Thread separada. Senão, não é possível preparar o cenário dentro
* do método @Test */

@MicronautTest(transactional = false)
internal class CadastraChavePixEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: PixKeymanagerServiceGrpc.PixKeymanagerServiceBlockingStub
) {

    // Teste de integração. Testando com o cliente Http.
    @Inject
    lateinit var itauClient: ErpItauClient

    companion object {
        val CLIENTE_ID = UUID.randomUUID()
    }

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
        ).thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        // Ação
        val response = grpcClient.cadastra(
            CadastraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.EMAIL)
                .setValorChave("teste@teste.com")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build()
        )

        // Validação
        with(response) {
            assertNotNull(pixId)
        }
    }


    /* Sad(?) Path :( */

    @Test
    fun `nao deve cadastrar chave pix quando chave existente`() {
        // Cenário - Salvar uma chave no banco
        repository.save(
            chaveFake(
                tipo = TipoDeChave.CPF,
                chave = "86135457004",
                clienteId = CLIENTE_ID
            )
        )

        // Ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(CadastraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setValorChave("86135457004")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }

        // Validação
        with(thrown){
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("A chave Pix '86135457004' já existe no banco", status.description)
        }

    }

    @Test
    fun `nao deve cadastrar chave pix quando nao encontrar dados da conta cliente`() {
        // Cenário - Mockar busca no Client Itau devolvendo status de não encontrado
        Mockito.`when`(itauClient.buscaContaDoClientePorTipo(clienteId = CLIENTE_ID.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        // Ação
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.cadastra(CadastraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setValorChave("86135457004")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
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
            grpcClient.cadastra(CadastraChavePixRequest.newBuilder()
                .setClienteId(CLIENTE_ID.toString())
                .setTipoChave(TipoChave.CPF)
                .setValorChave("861.354.a.570-04")
                .setTipoConta(TipoConta.CONTA_CORRENTE)
                .build())
        }

        // Validação
        with(thrown){
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            MatcherAssert.assertThat(violations(), Matchers.containsInAnyOrder(
                Pair("chave", "chave Pix inválida (CPF)")
            ))
        }
    }

    private fun dadosDaContaResponse(): ContaResponse {
        return ContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse("UNIBANCO ITAU SA", "60701190"),
            agencia = "0001",
            numero = "123455",
            titular = TitularResponse("Yuri Matheus", "86135457004")
        )
    }

    private fun chaveFake(
        tipo: TipoDeChave,
        chave: String = UUID.randomUUID().toString(),
        clienteId: UUID = UUID.randomUUID()
    ): ChavePixEntity {
        return ChavePixEntity(
            clienteId = clienteId,
            tipoChave = tipo,
            valorChave = chave,
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

    // Mock do client Http (para não ter que levantar todo o ambiente externo)
    @MockBean(ErpItauClient::class)
    fun itauClient(): ErpItauClient? {
        return Mockito.mock(ErpItauClient::class.java)
    }

    // Criando um Client para consumir a resposta do endpoint gRPC
    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): PixKeymanagerServiceGrpc.PixKeymanagerServiceBlockingStub {
            return PixKeymanagerServiceGrpc.newBlockingStub(channel)
        }
    }

}