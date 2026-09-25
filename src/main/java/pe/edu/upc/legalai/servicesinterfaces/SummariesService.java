package pe.edu.upc.legalai.servicesinterfaces;
import pe.edu.upc.legalai.entities.Summaries;
import java.util.List;

public interface SummariesService {
    void insert(Summaries summaries);
    List<Summaries> list();
}