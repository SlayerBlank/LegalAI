package pe.edu.upc.legalai.servicesinterfaces;
import pe.edu.upc.legalai.entities.Drafts;
import java.util.List;

public interface DraftsService {
    void insert(Drafts drafts);
    List<Drafts> list();
}