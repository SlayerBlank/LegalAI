package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.edu.upc.legalai.exceptions.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.dtos.request.DraftsRequestDTO;
import pe.edu.upc.legalai.dtos.response.DraftsConsultaResponseDTO;
import pe.edu.upc.legalai.dtos.response.DraftsResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.IDraftsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/drafts")
@Tag(name = "drafts-controller", description = "Borradores del usuario autenticado. Un borrador ajeno responde 404.")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Entrada invalida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "401", description = "JWT ausente, invalido o usuario inactivo", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "Recurso inexistente o de otro usuario", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class DraftsController {

    private final IDraftsService draftsService;

    public DraftsController(IDraftsService draftsService) {
        this.draftsService = draftsService;
    }

    @Operation(summary = "Registrar borrador")
    @ApiResponse(responseCode = "201", description = "Borrador creado")
    @PostMapping
    public ResponseEntity<DraftsResponseDTO> registrar(
            @Valid @RequestBody DraftsRequestDTO request
    ) {
        DraftsResponseDTO response = draftsService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Listar mis borradores")
    @ApiResponse(responseCode = "200", description = "Borradores del usuario")
    @GetMapping
    public ResponseEntity<List<DraftsResponseDTO>> listar() {
        return ResponseEntity.ok(
                draftsService.listarPorUsuarioAutenticado()
        );
    }

    @Operation(summary = "Borradores de un expediente por estado y fecha de modificacion (HU-64)")
    @ApiResponse(responseCode = "200", description = "Borradores que cumplen el filtro")
    @GetMapping("/filter")
    public ResponseEntity<List<DraftsConsultaResponseDTO>>
    consultarPorExpedienteEstadoYFecha(
            @RequestParam(name = "caseId", required = false)
            Long caseId,

            @RequestParam(name = "status", required = false)
            String status,

            @RequestParam(name = "updatedFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedFrom,

            @RequestParam(name = "updatedTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedTo
    ) {
        return ResponseEntity.ok(
                draftsService.consultarPorExpedienteEstadoYFecha(
                        caseId,
                        status,
                        updatedFrom,
                        updatedTo
                )
        );
    }

    @Operation(summary = "Consultar borrador (HU-044)")
    @ApiResponse(responseCode = "200", description = "Borrador encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<DraftsResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                draftsService.buscarPorId(id)
        );
    }

    @Operation(summary = "Editar borrador (HU-045)", description = "Valida titulo, contenido y estado; un borrador ajeno o inexistente responde 404.")
    @ApiResponse(responseCode = "200", description = "Borrador actualizado")
    @PutMapping("/{id}")
    public ResponseEntity<DraftsResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DraftsRequestDTO request
    ) {
        return ResponseEntity.ok(
                draftsService.actualizar(id, request)
        );
    }

    @Operation(summary = "Eliminar borrador (HU-046)")
    @ApiResponse(responseCode = "204", description = "Borrador eliminado", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        draftsService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}