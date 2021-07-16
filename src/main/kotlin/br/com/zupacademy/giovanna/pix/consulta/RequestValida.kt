package br.com.zupacademy.giovanna.pix.consulta

import br.com.zupacademy.giovanna.excecoes.ChavePixNaoEncontradaException
import br.com.zupacademy.giovanna.externos.bcb.BcbClient
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import br.com.zupacademy.giovanna.validacoes.ValidUUID
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

// sealed classes: can't be extended in code
@Introspected
sealed class RequestValida {
    abstract fun busca(repository: ChavePixRepository, bcbClient: BcbClient): DetalheChaveResponse

    @Introspected
    data class ComPixId(
        @field:NotBlank @field:ValidUUID
        val pixId: String,

        @field:NotBlank @field:ValidUUID
        val clienteId: String
    ) : RequestValida() {
        override fun busca(repository: ChavePixRepository, bcbClient: BcbClient): DetalheChaveResponse {
            return repository.findById(UUID.fromString(pixId))
                .filter { it.belongsTo(UUID.fromString(clienteId)) }
                .map(DetalheChaveResponse::of)
                .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada") }
        }

    }

    @Introspected
    data class ComValorChave(
        @field:NotBlank @field:Size(max = 77)
        val valorChave: String
    ) : RequestValida() {

        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        override fun busca(repository: ChavePixRepository, bcbClient: BcbClient): DetalheChaveResponse {
            return repository.findByValorChave(valorChave)
                .map(DetalheChaveResponse::of)
                .orElseGet {
                    LOGGER.info("Consultando chave Pix '$valorChave' no Banco Central do Brasil (BCB)")
                    val response = bcbClient.findByKeyValue(valorChave)
                    when (response.status) {
                        HttpStatus.OK -> response.body()?.toDetalheChaveResponse()
                        else -> throw ChavePixNaoEncontradaException("Chave Pix não encontrada")
                    }
                }
        }
    }

    @Introspected
    class Invalido() : RequestValida() {
        override fun busca(repository: ChavePixRepository, bcbClient: BcbClient): DetalheChaveResponse {
            throw IllegalArgumentException("Chave Pix inválida ou não informada")
        }
    }

}