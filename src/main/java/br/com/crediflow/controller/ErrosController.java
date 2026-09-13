package br.com.crediflow.controller;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ErrosController {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalido(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "DADOS_INVALIDOS",
            "mensagem", "Confira os campos, formatos e limites informados. Use somente dados ficticios."));
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> ausente(Exception e) {
        return ResponseEntity.status(404).body(Map.of("erro", "NAO_ENCONTRADO"));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> tamanho(Exception e) {
        return ResponseEntity.status(413).body(Map.of("erro", "ARQUIVO_MUITO_GRANDE"));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> indisponivel(Exception e) {
        return ResponseEntity.status(503).body(Map.of("erro", "SERVICO_LOCAL_INDISPONIVEL",
            "mensagem", "Verifique os servicos locais. Nenhum resultado deve ser presumido; consulte os registros antes de repetir."));
    }
}
