package br.com.zupacademy.giovanna.pix.consulta

import br.com.zupacademy.giovanna.DetalheChavePixResponse
import br.com.zupacademy.giovanna.TipoChave
import br.com.zupacademy.giovanna.TipoConta
import com.google.protobuf.Timestamp
import java.time.ZoneId

class ConsultaConverter {
    fun converteDetalhe(detalheChave: DetalheChaveResponse): DetalheChavePixResponse {
        return DetalheChavePixResponse.newBuilder()
            .setPixId(detalheChave.pixId?.toString() ?: "") // Protobuf usa "" como default value para String
            .setClienteId(detalheChave.clienteId?.toString() ?: "")
            .setChave(
                DetalheChavePixResponse.ChaveInfo
                    .newBuilder()
                    .setTipoChave(TipoChave.valueOf(detalheChave.tipoChave.name))
                    .setValorChave(detalheChave.valorChave)
                    .setConta(
                        DetalheChavePixResponse.ChaveInfo.ContaInfo.newBuilder()
                            .setNomeTitular(detalheChave.conta.nomeTitular)
                            .setCpfTitular(detalheChave.conta.cpfTitular)
                            .setInstituicao(detalheChave.conta.nomeInstituicao)
                            .setAgencia(detalheChave.conta.agencia)
                            .setNumeroConta(detalheChave.conta.numeroConta)
                            .setTipoConta(TipoConta.valueOf(detalheChave.tipoConta.name))
                            .build()
                    )
                    .setDataCadastro(detalheChave.dataCadastro.let {
                        val createdAt = it.atZone(ZoneId.of("UTC")).toInstant()
                        Timestamp.newBuilder()
                            .setSeconds(createdAt.epochSecond)
                            .setNanos(createdAt.nano)
                            .build()
                    })
            )
            .build()
    }
}