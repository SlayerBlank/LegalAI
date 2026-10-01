package pe.edu.upc.legalai.controllers;

import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.request.SummariesRequestDTO;
import pe.edu.upc.legalai.dtos.response.SummariesResponseDTO;
import pe.edu.upc.legalai.entities.Summaries;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.servicesinterfaces.ISummariesService;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/summaries")
public class SummariesController {

    private final ISummariesService sS;
    private final ModelMapper modelMapper;

    public SummariesController(ISummariesService sS, ModelMapper modelMapper) {
        this.sS = sS;
        this.modelMapper = modelMapper;
    }

    @PostMapping
    public void insert(@RequestBody SummariesRequestDTO dto) {
        Summaries s = modelMapper.map(dto, Summaries.class);

        Documento doc = new Documento();
        doc.setDocumentId(dto.getDocumento_id());
        s.setDocumento(doc);

        Expediente e = new Expediente();
        e.setCaseId(dto.getExpediente_id());
        s.setExpediente(e);

        Usuario u = new Usuario();
        u.setUserId(dto.getUsuario_id());
        s.setGeneratedBy(u);

        sS.insert(s);
    }

    @GetMapping
    public List<SummariesResponseDTO> list() {
        return sS.list().stream()
                .map(x -> modelMapper.map(x, SummariesResponseDTO.class))
                .collect(Collectors.toList());
    }
}