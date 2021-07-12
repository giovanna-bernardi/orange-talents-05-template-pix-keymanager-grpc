package br.com.zupacademy.giovanna.pix.cadastro

import br.com.zupacademy.giovanna.excecoes.ChavePixExistenteException
import br.com.zupacademy.giovanna.pix.ChavePixEntity
import br.com.zupacademy.giovanna.externos.itau.ErpItauClient
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class GerenciadorCadastroChavePix(val chavePixRepository: ChavePixRepository,
                                  val itauClient: ErpItauClient) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun tentaCadastrar(@Valid novaChavePixRequest: NovaChavePixRequest): ChavePixEntity {

        /* Criei uma anotação "UniqueValue" sobre o valor da chave, mas
        * não gostei da forma como ficou no handler (usando PropertyNode
        * do ConstraintViolation). Fica ruim de separar as mensagens quando
        * tem erro de validação e de chave já existente. */

        // Verifica se chave já existe no banco
        if (chavePixRepository.existsByValorChave(novaChavePixRequest.valorChave))
            throw ChavePixExistenteException("A chave Pix '${novaChavePixRequest.valorChave}' já existe no banco")

        // Busca dados da conta no ERP do ITAU
        val contaResponse = itauClient.buscaContaDoClientePorTipo(novaChavePixRequest.clienteId!!, novaChavePixRequest.tipoConta!!.name)
        val conta = contaResponse.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no ITAU")


        // Salva no banco de dados
        val chavePix = novaChavePixRequest.toModel(conta)
        chavePixRepository.save(chavePix)

        return chavePix
    }
}