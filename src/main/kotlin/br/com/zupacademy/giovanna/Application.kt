package br.com.zupacademy.giovanna

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("br.com.zupacademy.giovanna")
		.start()
}

