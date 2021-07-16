package br.com.zupacademy.giovanna.pix.remocao

import br.com.zupacademy.giovanna.PixKeyExclusionManagerServiceGrpc
import br.com.zupacademy.giovanna.RemoveChavePixRequest
import br.com.zupacademy.giovanna.RemoveChavePixResponse
import br.com.zupacademy.giovanna.compartilhado.grpc.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Singleton

@Singleton
@ErrorHandler
class RemoveChavePixEndpoint (private val gerenciadorExclusaoChavePix: GerenciadorExclusaoChavePix) : PixKeyExclusionManagerServiceGrpc.PixKeyExclusionManagerServiceImplBase() {

    override fun remove(request: RemoveChavePixRequest, responseObserver: StreamObserver<RemoveChavePixResponse>) {
        val chaveRequest = request.toChaveRemocaoRequest()

        // validar e excluir do banco
        gerenciadorExclusaoChavePix.tentaExcluir(chaveRequest)

        responseObserver.onNext(RemoveChavePixResponse.newBuilder()
            .setRemovido(true)
            .build())
        responseObserver.onCompleted()

    }
}

/*
 *  Extension function para transformar o Request do gRPC
 *  no Request intermediário para validações
 */
fun RemoveChavePixRequest.toChaveRemocaoRequest() = ChaveRemocaoRequest(pixId, clienteId)