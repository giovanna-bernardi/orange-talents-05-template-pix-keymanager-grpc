package br.com.zupacademy.giovanna.pix

import br.com.zupacademy.giovanna.TipoChave
import br.com.zupacademy.giovanna.TipoConta
import br.com.zupacademy.giovanna.conta.ContaEntity
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name = "chavePix")
class ChavePixEntity(
    @field:NotNull
    @Column(nullable = false)
    val clienteId: UUID,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoChave: TipoChave,

    @field:Size(max = 77) @field:NotBlank
    @Column(nullable = false, unique = true)
    val valorChave: String,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoConta: TipoConta,

    // Toda chave pix deve ter uma conta associada a ela
    @field:Valid
    @Embedded
    val conta: ContaEntity
) {

    @Id
    @GeneratedValue
    var id: UUID? = null

    @Column(nullable = false)
    val criadaEm: LocalDateTime = LocalDateTime.now()

    override fun toString(): String {
        return """ChavePixEntity(clienteId=$clienteId
tipoChave=$tipoChave
valorChave=$valorChave
tipoConta=$tipoConta
id=$id
criadaEm=$criadaEm)"""
    }
}