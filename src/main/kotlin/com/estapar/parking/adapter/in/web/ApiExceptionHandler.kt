package com.estapar.parking.adapter.`in`.web

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Respostas de erro no formato Problem Details (RFC 9457), com `detail` dizendo o que corrigir.
 * A classe base cuida das demais exceções do Spring MVC.
 */
@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {

    /** JSON malformado, campo ausente ou com tipo errado, `event_type` desconhecido. */
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val detail = when (val cause = ex.cause) {
            is InvalidTypeIdException -> "event_type ausente ou desconhecido; esperado ENTRY, PARKED ou EXIT"
            is JsonMappingException -> cause.fieldPath()?.let { "Campo ausente ou inválido: $it" } ?: "Corpo da requisição inválido"
            else -> "JSON malformado"
        }
        return badRequest(ex, detail, headers, request)
    }

    /** Falha de Bean Validation no corpo (placa em branco, placa longa demais...). */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val detail = ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return badRequest(ex, detail, headers, request)
    }

    /** Payloads rejeitados não chegam ao controller; o WARN garante que também fiquem no log. */
    private fun badRequest(ex: Exception, detail: String, headers: HttpHeaders, request: WebRequest): ResponseEntity<Any>? {
        logger.warn("Requisição rejeitada com 400 em ${request.getDescription(false)}: $detail")
        return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), headers, HttpStatus.BAD_REQUEST, request)
    }

    /** Caminho do campo no JSON, com os nomes do contrato (ex.: "entry_time"). */
    private fun JsonMappingException.fieldPath(): String? =
        path.mapNotNull { it.fieldName ?: it.index.takeIf { i -> i >= 0 }?.let { i -> "[$i]" } }
            .joinToString(".")
            .ifEmpty { null }
}
