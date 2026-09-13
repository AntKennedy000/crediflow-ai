package br.com.crediflow.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "lancamentos")
public class Lancamento {
    @Id private UUID id;
    @Column(nullable = false) private String tipo;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal valor;
    @Column(nullable = false) private LocalDate data;
    @Column(nullable = false) private String categoria;
    @Column(nullable = false, length = 160) private String descricao;
    @Column(nullable = false) private String estado;
    @Column(nullable = false) private Instant criadoEm;
    private Instant confirmadoEm;
    @Version private Long versao;

    protected Lancamento() { }
    public Lancamento(String tipo, BigDecimal valor, LocalDate data, String categoria, String descricao) {
        this.id = UUID.randomUUID(); this.tipo = tipo; this.valor = valor;
        this.data = data; this.categoria = categoria; this.descricao = descricao;
        this.estado = "PENDENTE"; this.criadoEm = Instant.now();
    }
    public void confirmar() { estado = "CONFIRMADO"; confirmadoEm = Instant.now(); }
    public UUID getId() { return id; }
    public String getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getData() { return data; }
    public String getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public String getEstado() { return estado; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getConfirmadoEm() { return confirmadoEm; }
}
