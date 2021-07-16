package br.com.zupacademy.giovanna.externos.bcb

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("\${bcb.pix.url}")
interface BcbClient {

    @Post("/api/v1/pix/keys")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    fun insert(@Body request: CreatePixKeyRequest) : HttpResponse<CreatePixKeyResponse>

    @Delete("/api/v1/pix/keys/{key}")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    fun delete(@PathVariable key: String, @Body request: DeletePixKeyRequest) : HttpResponse<DeletePixKeyResponse>

    @Get("/api/v1/pix/keys/{key}")
    @Consumes(MediaType.APPLICATION_XML)
    fun findByKeyValue(@PathVariable key: String) : HttpResponse<PixKeyDetailsResponse>

}
