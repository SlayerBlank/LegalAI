package pe.edu.upc.legalai.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Summaries;

@Repository
public interface SummariesRepository extends JpaRepository<Summaries, Long> {
}