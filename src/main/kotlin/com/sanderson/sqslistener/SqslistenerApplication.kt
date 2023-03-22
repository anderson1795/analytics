package com.sanderson.sqslistener

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SqslistenerApplication

fun main(args: Array<String>) {
	runApplication<SqslistenerApplication>(*args)
}
