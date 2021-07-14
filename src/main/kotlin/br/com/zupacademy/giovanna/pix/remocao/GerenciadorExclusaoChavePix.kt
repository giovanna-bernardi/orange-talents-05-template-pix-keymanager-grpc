package br.com.zupacademy.giovanna.pix.remocao

import br.com.zupacademy.giovanna.excecoes.ChavePixNaoEncontradaException
import br.com.zupacademy.giovanna.externos.bcb.BcbClient
import br.com.zupacademy.giovanna.externos.bcb.DeletePixKeyRequest
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class GerenciadorExclusaoChavePix(
    val chavePixRepository: ChavePixRepository,
    val bcbClient: BcbClient
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun tentaExcluir(@Valid chaveRemocaoRequest: ChaveRemocaoRequest) {

        val pixId = UUID.fromString(chaveRemocaoRequest.pixId)
        val clienteId = UUID.fromString(chaveRemocaoRequest.clienteId)

        // buscar chave no banco (se não existir, retorna not_found com mensagem amigável)
        val chaveEncontrada = chavePixRepository.findByIdAndClienteId(pixId, clienteId)
            .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada para este cliente") }

        try {
            val key = chaveEncontrada.valorChave
            val bcbResponse = bcbClient.delete(key, DeletePixKeyRequest(key))
            if(bcbResponse.status.equals(HttpStatus.OK)) {
                // só remove se conseguir remover do BCB
                chavePixRepository.deleteById(pixId)
            }
        } catch (e: HttpClientResponseException){
            when(e.status) {
                HttpStatus.FORBIDDEN -> throw IllegalStateException("Proibido realizar operação no Banco Central do Brasil (BCB)")
                HttpStatus.NOT_FOUND -> throw ChavePixNaoEncontradaException("Chave pix não encontrada no Banco Central do Brasil (BCB) ")
                else -> throw IllegalStateException("Erro ao remover chave Pix no Banco Central do Brasil (BCB)")
            }
        }
    }
}