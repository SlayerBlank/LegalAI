package pe.edu.upc.legalai.servicesimplements;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.upc.legalai.entities.Drafts;
import pe.edu.upc.legalai.repositories.DraftsRepository;
import pe.edu.upc.legalai.servicesinterfaces.DraftsService;
import java.util.List;

@Service
public class DraftsServiceImpl implements DraftsService {
    @Autowired
    private DraftsRepository dR;

    @Override
    public void insert(Drafts drafts) { dR.save(drafts); }

    @Override
    public List<Drafts> list() { return dR.findAll(); }
}