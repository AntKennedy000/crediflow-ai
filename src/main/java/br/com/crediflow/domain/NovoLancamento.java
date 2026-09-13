package br.com.crediflow.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record NovoLancamento(
    @NotNull @Pattern(regexp="RECEITA|DESPESA") String tipo,
    @NotNull @DecimalMin("0.01") @DecimalMax("999999999.99") @Digits(integer=9, fraction=2) BigDecimal valor,
    @NotNull LocalDate data,
    @NotNull @Pattern(regexp="SALARIO|MORADIA|ALIMENTACAO|TRANSPORTE|SAUDE|EDUCACAO|DIVIDAS|OUTROS") String categoria,
    @NotBlank @Size(max=160) String descricao
) { }
