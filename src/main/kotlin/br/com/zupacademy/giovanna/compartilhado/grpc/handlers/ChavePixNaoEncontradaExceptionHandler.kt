package br.com.zupacademy.giovanna.compartilhado.grpc.handlers

import br.com.zupacademy.giovanna.compartilhado.grpc.ExceptionHandler
import br.com.zupacademy.giovanna.excecoes.ChavePixNaoEncontradaException
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class ChavePixNaoEncontradaExceptionHandler: ExceptionHandler<ChavePixNaoEncontradaException> {
    override fun handle(e: ChavePixNaoEncontradaException): ExceptionHandler.StatusWithDetails {
        return ExceptionHandler.StatusWithDetails(
            Status.NOT_FOUND
                .withDescription(e.message)
                .withCause(e)
        )
    }

    override fun supports(e: Exception): Boolean {
        return e is ChavePixNaoEncontradaException
    }
}