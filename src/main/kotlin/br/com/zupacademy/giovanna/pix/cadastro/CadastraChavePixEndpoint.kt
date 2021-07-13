package br.com.zupacademy.giovanna.pix.cadastro

import br.com.zupacademy.giovanna.*
import br.com.zupacademy.giovanna.compartilhado.grpc.ErrorHandler
import br.com.zupacademy.giovanna.pix.TipoChave as TipoDeChave
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
@ErrorHandler
class CadastraChavePixEndpoint(val gerenciadorCadastroChavePix: GerenciadorCadastroChavePix)
    : PixKeyRegistrationManagerServiceGrpc.PixKeyRegistrationManagerServiceImplBase() {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun cadastra(
        request: CadastraChavePixRequest,
        responseObserver: StreamObserver<CadastraChavePixResponse>
    ) {

        // converter request em modelo
        val novaChavePixRequest = request.toNovaChavePixRequest()
        logger.info("novaChavePixRequest: $novaChavePixRequest")

        // validar e salvar no banco
        val chavePixCadastrada = gerenciadorCadastroChavePix.tentaCadastrar(novaChavePixRequest)
        logger.info("chavePìxCadastrada: $chavePixCadastrada")

        // retornar uma resposta
        responseObserver.onNext(CadastraChavePixResponse.newBuilder()
            .setPixId(chavePixCadastrada.id.toString()).build())
        responseObserver.onCompleted()

    }
}

fun CadastraChavePixRequest.toNovaChavePixRequest(): NovaChavePixRequest {
    return NovaChavePixRequest(
        clienteId = clienteId,
        tipoChave = if(tipoChave.equals(TipoChave.UNKNOWN_CHAVE)) null else TipoDeChave.valueOf(tipoChave.name),
        valorChave = valorChave,
        tipoConta = if(tipoConta.equals(TipoConta.UNKNOWN_CONTA)) null else TipoConta.valueOf(tipoConta.name)
    )
}