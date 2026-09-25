package pe.edu.upc.legalai.servicesimplements;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.upc.legalai.entities.Summaries;
import pe.edu.upc.legalai.repositories.SummariesRepository;
import pe.edu.upc.legalai.servicesinterfaces.SummariesService;
import java.util.List;

@Service
public class SummariesServiceImpl implements SummariesService {
    @Autowired
    private SummariesRepository sR;

    @Override
    public void insert(Summaries summaries) { sR.save(summaries); }

    @Override
    public List<Summaries> list() { return sR.findAll(); }
}