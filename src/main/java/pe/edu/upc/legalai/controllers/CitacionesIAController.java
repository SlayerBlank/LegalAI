package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import pe.edu.upc.legalai.exceptions.ErrorResponse;

import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.response.CitacionIAResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.ICitacionesIAService;
import java.util.List;

@RestController
@RequestMapping("/api/chat/sessions/{sessionId}/messages/{messageId}/citations")
@Tag(name = "citaciones-ia-controller", description = "Fuentes documentales de respuestas RAG; no equivalen a jurisprudencia o normas verificadas.")
public class CitacionesIAController {
    private final ICitacionesIAService citations;

    public CitacionesIAController(ICitacionesIAService citations) { this.citations = citations; }

    @GetMapping
    @Operation(summary = "Consultar fuentes de una respuesta IA",
            description = "Las referencias se generan y persisten al enviar POST /api/chat/sessions/{sessionId}/messages. "
                    + "Consulta exclusivamente un mensaje ASSISTANT de una sesion y expediente propios. "
                    + "Sin fuentes devuelve lista vacia. documentId puede ser null por retencion tras eliminar el documento. "
                    + "relevanceScore no es confianza ni autenticidad juridica.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fuentes consultadas", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CitacionIAResponseDTO.class)))),
        @ApiResponse(responseCode = "400", description = "Identificadores invalidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Se requiere JWT valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Acceso denegado", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Respuesta inexistente, no ASSISTANT, sesion incorrecta o recurso ajeno", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<CitacionIAResponseDTO> listar(
            @Parameter(description = "Identificador positivo de la sesion propia") @PathVariable Long sessionId,
            @Parameter(description = "Identificador positivo del mensaje ASSISTANT") @PathVariable Long messageId) {
        return citations.listarPorMensaje(sessionId, messageId);
    }
}
