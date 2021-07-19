package br.com.zupacademy.giovanna.pix.lista

import br.com.zupacademy.giovanna.*
import br.com.zupacademy.giovanna.compartilhado.grpc.ErrorHandler
import br.com.zupacademy.giovanna.pix.ChavePixRepository
import com.google.protobuf.Timestamp
import io.grpc.stub.StreamObserver
import java.lang.IllegalArgumentException
import java.time.ZoneId
import java.util.*
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ListaChavePixEndpoint(private val repository: ChavePixRepository) : PixKeyListManagerServiceGrpc.PixKeyListManagerServiceImplBase() {
    override fun lista(request: ListaChavePixRequest, responseObserver: StreamObserver<ListaChavePixResponse>) {

        // Pesquisar por clienteId
        // Caso erro, retornar o erro específico e amigável
        if(request.clienteId.isNullOrBlank()) {
            throw IllegalArgumentException("ClienteId não pode ser nulo nem vazio")
        }

        // Caso sucesso, retornar uma lista de detalhes das chaves
        // Caso nenhuma chave seja encontrada, retornar uma coleção vazia
        val clienteId = UUID.fromString(request.clienteId)
        val chaves = repository.findAllByClienteId(clienteId).map {
            ListaChavePixResponse.ChavePix.newBuilder()
                .setPixId(it.id.toString())
                .setTipoChave(TipoChave.valueOf(it.tipoChave.name))
                .setValorChave(it.valorChave)
                .setTipoConta(TipoConta.valueOf(it.tipoConta.name))
                .setDataCadastro(it.criadaEm.let{
                    val dataCadastro = it.atZone(ZoneId.of("UTC")).toInstant()
                    Timestamp.newBuilder().setSeconds(dataCadastro.epochSecond)
                        .setNanos(dataCadastro.nano).build()
                })
                .build()
        }

        responseObserver.onNext(ListaChavePixResponse.newBuilder()
            .setClienteId(clienteId.toString())
            .addAllChaves(chaves).build())
        responseObserver.onCompleted()
    }
}