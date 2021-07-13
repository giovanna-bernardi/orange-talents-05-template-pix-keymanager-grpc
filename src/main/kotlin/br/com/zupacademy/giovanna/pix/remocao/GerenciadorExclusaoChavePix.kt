package br.com.zupacademy.giovanna.pix.remocao

import br.com.zupacademy.giovanna.excecoes.ChavePixNaoEncontradaException
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class GerenciadorExclusaoChavePix(val chavePixRepository: ChavePixRepository) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun tentaExcluir(@Valid chaveRemocaoRequest: ChaveRemocaoRequest) {

        val pixId = UUID.fromString(chaveRemocaoRequest.pixId)
        val clienteId = UUID.fromString(chaveRemocaoRequest.clienteId)

        // buscar chave no banco (se não existir, retorna not_found com mensagem amigável)
        val chaveEncontrada = chavePixRepository.existsByIdAndClienteId(pixId, clienteId)
        if (!chaveEncontrada) throw ChavePixNaoEncontradaException("Chave Pix não encontrada para este cliente")
        else chavePixRepository.deleteById(pixId)
    }
}