package br.com.zupacademy.giovanna.pix.cadastro

import br.com.zupacademy.giovanna.excecoes.ChavePixExistenteException
import br.com.zupacademy.giovanna.externos.bcb.BcbClient
import br.com.zupacademy.giovanna.externos.bcb.CreatePixKeyRequest
import br.com.zupacademy.giovanna.externos.bcb.CreatePixKeyResponse
import br.com.zupacademy.giovanna.externos.itau.ErpItauClient
import br.com.zupacademy.giovanna.pix.ChavePixEntity
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class GerenciadorCadastroChavePix(
    val chavePixRepository: ChavePixRepository,
    val itauClient: ErpItauClient,
    val bcbClient: BcbClient
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun tentaCadastrar(@Valid novaChavePixRequest: NovaChavePixRequest): ChavePixEntity {

        /* Criei uma anotação "UniqueValue" sobre o valor da chave, mas
        * não gostei da forma como ficou no handler (usando PropertyNode
        * do ConstraintViolation). Fica ruim de separar as mensagens quando
        * tem erro de validação e de chave já existente. */

        // Verifica se chave já existe no banco
        if (chavePixRepository.existsByValorChave(novaChavePixRequest.valorChave))
            throw ChavePixExistenteException("A chave Pix '${novaChavePixRequest.valorChave}' já existe no banco")

        // Busca dados da conta no ERP do ITAU
        val contaResponse =
            itauClient.buscaContaDoClientePorTipo(novaChavePixRequest.clienteId!!, novaChavePixRequest.tipoConta!!.name)
        val conta = contaResponse.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no ITAU")
        val chavePix = novaChavePixRequest.toModel(conta)

        // Salva no banco de dados
        chavePixRepository.save(chavePix)

        try {
            // salva no Sistema Pix do BCB
            val bcbResponse = bcbClient.insert(CreatePixKeyRequest.of(chavePix))

            // Devolve 201 se sucesso
            if (bcbResponse.status != HttpStatus.CREATED)
                throw IllegalStateException("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)")

            // atualizar a chave, se ALEATORIA, com o valor vindo do BCB
            if (chavePix.updateKeyValue(bcbResponse.body().key))
                chavePixRepository.update(chavePix)

        } catch (e: HttpClientResponseException) {
            if (e.status.equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                // porque não quero usar @Transactional com chamada a sistema externo e
                // não acho que deva salvar na minha aplicação caso ocorra erro ao tentar salvar no sistema externo
                chavePixRepository.delete(chavePix)

                throw ChavePixExistenteException("Chave pix já registrada no Banco Central do Brasil (BCB)")
            }
        }

        return chavePix
    }
}