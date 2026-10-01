package pe.edu.upc.legalai.controllers;

import org.modelmapper.ModelMapper;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.request.DraftsRequestDTO;
import pe.edu.upc.legalai.dtos.response.DraftsResponseDTO;
import pe.edu.upc.legalai.entities.Drafts;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.servicesinterfaces.IDraftsService;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/drafts")
public class DraftsController {

    private final IDraftsService dS;
    private final ModelMapper modelMapper;

    public DraftsController(IDraftsService dS, ModelMapper modelMapper) {
        this.dS = dS;
        this.modelMapper = modelMapper;
    }

    @PostMapping
    public void insert(@RequestBody DraftsRequestDTO dto) {
        Drafts d = modelMapper.map(dto, Drafts.class);

        Expediente e = new Expediente();
        e.setCaseId(dto.getExpediente_id());
        d.setExpediente(e);

        Usuario u = new Usuario();
        u.setUserId(dto.getUsuario_id());
        d.setCreatedBy(u);

        dS.insert(d);
    }

    @GetMapping
    public List<DraftsResponseDTO> list() {
        return dS.list().stream()
                .map(x -> modelMapper.map(x, DraftsResponseDTO.class))
                .collect(Collectors.toList());
    }
}