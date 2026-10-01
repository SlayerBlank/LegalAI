package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import pe.edu.upc.legalai.entities.Summaries;
import pe.edu.upc.legalai.repositories.ISummariesRepository;
import pe.edu.upc.legalai.servicesinterfaces.ISummariesService;
import java.util.List;

@Service
public class SummariesServiceImplement implements ISummariesService {
    private final ISummariesRepository sR;

    public SummariesServiceImplement(ISummariesRepository sR) {
        this.sR = sR;
    }

    @Override
    public void insert(Summaries summaries) { sR.save(summaries); }

    @Override
    public List<Summaries> list() { return sR.findAll(); }
}