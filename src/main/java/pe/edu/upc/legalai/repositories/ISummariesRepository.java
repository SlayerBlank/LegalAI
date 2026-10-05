package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Summaries;

import java.util.List;
import java.util.Optional;

@Repository
public interface ISummariesRepository
        extends JpaRepository<Summaries, Long> {

    List<Summaries> findByGeneratedByUserIdOrderBySummaryIdDesc(
            Long userId
    );

    Optional<Summaries> findBySummaryIdAndGeneratedByUserId(
            Long summaryId,
            Long userId
    );
}