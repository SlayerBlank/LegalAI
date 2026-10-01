package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import pe.edu.upc.legalai.entities.Drafts;
import pe.edu.upc.legalai.repositories.IDraftsRepository;
import pe.edu.upc.legalai.servicesinterfaces.IDraftsService;
import java.util.List;

@Service
public class DraftsServiceImplement implements IDraftsService {
    private final IDraftsRepository dR;

    public DraftsServiceImplement(IDraftsRepository dR) {
        this.dR = dR;
    }

    @Override
    public void insert(Drafts drafts) { dR.save(drafts); }

    @Override
    public List<Drafts> list() { return dR.findAll(); }
}