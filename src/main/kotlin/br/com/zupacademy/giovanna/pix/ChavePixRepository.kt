package br.com.zupacademy.giovanna.pix

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository: JpaRepository<ChavePixEntity, UUID> {
    fun existsByValorChave(valorChave: String?) : Boolean
//    fun findByValorChave(valorChave: String?) : Optional<ChavePixEntity>
    fun findByIdAndClienteId(pixId: UUID?, clienteId: UUID?): Optional<ChavePixEntity>
}