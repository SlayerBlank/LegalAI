package pe.edu.upc.legalai.controllers;

import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.response.CitacionIAResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.ICitacionesIAService;
import java.util.List;

@RestController
@RequestMapping("/api/chat/sessions/{sessionId}/messages/{messageId}/citations")
public class CitacionesIAController {
    private final ICitacionesIAService citations;

    public CitacionesIAController(ICitacionesIAService citations) { this.citations = citations; }

    @GetMapping
    public List<CitacionIAResponseDTO> listar(@PathVariable Long sessionId, @PathVariable Long messageId) {
        return citations.listarPorMensaje(sessionId, messageId);
    }
}
