package br.com.zupacademy.giovanna.pix.consulta

import br.com.zupacademy.giovanna.DetalheChavePixRequest
import br.com.zupacademy.giovanna.DetalheChavePixResponse
import br.com.zupacademy.giovanna.PixKeyDetailManagerServiceGrpc
import br.com.zupacademy.giovanna.compartilhado.grpc.ErrorHandler
import br.com.zupacademy.giovanna.externos.bcb.BcbClient
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import io.grpc.stub.StreamObserver
import javax.inject.Singleton
import javax.validation.ConstraintViolationException
import javax.validation.Validator

@ErrorHandler
@Singleton
class ConsultaChavePixEndpoint(
    private val repository: ChavePixRepository,
    private val validator: Validator,
    private val bcbClient: BcbClient
) : PixKeyDetailManagerServiceGrpc.PixKeyDetailManagerServiceImplBase() {
    override fun consulta(
        request: DetalheChavePixRequest,
        responseObserver: StreamObserver<DetalheChavePixResponse>
    ) {

        // Validar dados de entrada
        // Se Request for com pixId com clienteId, fazer a consulta no banco
        // Se for com valor da chave, consultar sistema externo

        // verificar o tipo da request(banco ou bcb) e validar os dados de entrada de acordo
        val requestValida = request.toRequestValida(validator)

        // fazer a consulta e retornar os detalhes da chave
        val detalheChave = requestValida.busca(repository, bcbClient)

        responseObserver.onNext(ConsultaConverter().converteDetalhe(detalheChave))
        responseObserver.onCompleted()
    }
}


fun DetalheChavePixRequest.toRequestValida(validator: Validator): RequestValida {
    val requestValida = when (tipoConsultaCase!!) {
        DetalheChavePixRequest.TipoConsultaCase.SISTEMAINTERNO -> RequestValida.ComPixId(pixId = sistemaInterno.pixId, clienteId = sistemaInterno.clienteId)
        DetalheChavePixRequest.TipoConsultaCase.VALORCHAVE -> RequestValida.ComValorChave(valorChave)
        DetalheChavePixRequest.TipoConsultaCase.TIPOCONSULTA_NOT_SET -> RequestValida.Invalido()
    }

    val violations = validator.validate(requestValida)
    if(violations.isNotEmpty()) {
        throw ConstraintViolationException(violations)
    }

    return requestValida
}