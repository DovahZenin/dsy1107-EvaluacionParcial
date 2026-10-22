package com.duoc.adminrabbit.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Traduce las excepciones del dominio a respuestas HTTP claras:
 *   400  datos de entrada invalidos (DTOs con @Valid o JSON malformado)
 *   404  recurso inexistente en el broker
 *   409  conflicto: ya existe o pertenece al sistema
 *   500  cualquier otra falla, con mensaje legible
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> camposInvalidos(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiError.conCampos(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "los datos enviados no son validos",
                campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoMalformado(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiError.de(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "el cuerpo del request no es JSON valido"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> noEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.de(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> conflicto(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.de(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception e) {
        log.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.de(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "error interno: " + e.getMessage()));
    }
}
