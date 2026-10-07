package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import pe.edu.upc.legalai.dtos.response.DraftsResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.IDraftsService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.upc.legalai.dtos.response.DraftsConsultaResponseDTO;

import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/drafts")
@Tag(
        name = "Drafts",
        description = "Gestión de borradores del usuario autenticado"
)
public class DraftsController {

    private final IDraftsService draftsService;

    public DraftsController(IDraftsService draftsService) {
        this.draftsService = draftsService;
    }

    @Operation(summary = "Crear un borrador")
    @ApiResponse(
            responseCode = "201",
            description = "Borrador creado"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Expediente inexistente o ajeno"
    )
    @PostMapping
    public ResponseEntity<DraftsResponseDTO> registrar(
            @Valid @RequestBody DraftsRequestDTO request
    ) {
        DraftsResponseDTO response = draftsService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Listar borradores propios")
    @ApiResponse(
            responseCode = "200",
            description = "Listado obtenido"
    )
    @GetMapping
    public ResponseEntity<List<DraftsResponseDTO>> listar() {
        return ResponseEntity.ok(
                draftsService.listarPorUsuarioAutenticado()
        );
    }

    @Operation(
            summary = "Consultar borradores por filtros",
            description = "Consulta borradores propios de un expediente por "
                    + "estado y rango de fechas de modificación. "
                    + "Los cuatro filtros son obligatorios y el rango "
                    + "incluye ambos días completos."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Resultados obtenidos; la lista puede estar vacía"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Filtros ausentes o inválidos"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Expediente inexistente o ajeno"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Autenticación ausente o inválida"
    )
    @GetMapping("/filter")
    public ResponseEntity<List<DraftsConsultaResponseDTO>>
    consultarPorExpedienteEstadoYFecha(
            @Parameter(
                    description = "ID del expediente propio",
                    required = true
            )
            @RequestParam(name = "caseId", required = false)
            Long caseId,

            @Parameter(
                    description = "Estado del borrador",
                    required = true
            )
            @RequestParam(name = "status", required = false)
            String status,

            @Parameter(
                    description = "Fecha inicial, formato yyyy-MM-dd",
                    required = true,
                    example = "2026-10-01"
            )
            @RequestParam(name = "updatedFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedFrom,

            @Parameter(
                    description = "Fecha final, formato yyyy-MM-dd",
                    required = true,
                    example = "2026-10-05"
            )
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

    @Operation(summary = "Buscar un borrador por ID")
    @ApiResponse(
            responseCode = "200",
            description = "Borrador obtenido"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Borrador inexistente o ajeno"
    )
    @GetMapping("/{id}")
    public ResponseEntity<DraftsResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                draftsService.buscarPorId(id)
        );
    }

    @Operation(summary = "Actualizar un borrador")
    @ApiResponse(
            responseCode = "200",
            description = "Borrador actualizado"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Borrador o expediente inexistente o ajeno"
    )
    @PutMapping("/{id}")
    public ResponseEntity<DraftsResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DraftsRequestDTO request
    ) {
        return ResponseEntity.ok(
                draftsService.actualizar(id, request)
        );
    }

    @Operation(summary = "Eliminar un borrador")
    @ApiResponse(
            responseCode = "204",
            description = "Borrador eliminado"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Borrador inexistente o ajeno"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        draftsService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}