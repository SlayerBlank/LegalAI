package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.response.CitacionIAResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.ICitacionesIAService;
import java.util.List;

@RestController
@RequestMapping("/api/chat/sessions/{sessionId}/messages/{messageId}/citations")
@Tag(name = "Chat")
public class CitacionesIAController {
    private final ICitacionesIAService citations;

    public CitacionesIAController(ICitacionesIAService citations) { this.citations = citations; }

    @GetMapping
    @Operation(summary = "Consultar referencias persistidas de una respuesta propia",
            description = "Los mensajes anteriores a la persistencia de citaciones pueden no tener filas; "
                    + "sus fuentes historicas permanecen en los metadatos del mensaje.")
    public List<CitacionIAResponseDTO> listar(@PathVariable Long sessionId, @PathVariable Long messageId) {
        return citations.listarPorMensaje(sessionId, messageId);
    }
}
