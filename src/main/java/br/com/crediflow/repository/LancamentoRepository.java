package br.com.crediflow.repository;

import br.com.crediflow.domain.Lancamento;
import java.time.LocalDate;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {
    List<Lancamento> findByEstadoAndDataGreaterThanEqualAndDataLessThanOrderByDataAscCriadoEmAsc(
        String estado, LocalDate inicio, LocalDate fim);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lancamento l where l.id = :id")
    Optional<Lancamento> buscarParaConfirmar(@Param("id") UUID id);
}
