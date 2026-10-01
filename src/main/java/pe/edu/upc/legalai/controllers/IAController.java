package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.legalai.dtos.request.IARequestDTO;
import pe.edu.upc.legalai.dtos.response.IAResponseDTO;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import pe.edu.upc.legalai.servicesinterfaces.IAService;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "Inteligencia artificial", description = "Prueba de IA sin contexto de expedientes")
@SecurityRequirement(name = "Bearer Token")
public class IAController {
    private final IAService iaService;

    public IAController(IAService iaService) { this.iaService = iaService; }

    @Operation(summary = "Probar inteligencia artificial", description = "Responde al prompt sin consultar datos de la base de datos")
    @ApiResponse(responseCode = "200", description = "Respuesta generada")
    @ApiResponse(responseCode = "400", description = "Prompt invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "JWT ausente o invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Servicio de IA no disponible", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/test")
    public ResponseEntity<IAResponseDTO> generarRespuesta(@Valid @RequestBody IARequestDTO request) {
        return ResponseEntity.ok(iaService.generarRespuesta(request));
    }
}
