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
import pe.edu.upc.legalai.dtos.request.SummariesRequestDTO;
import pe.edu.upc.legalai.dtos.response.SummariesResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.ISummariesService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import pe.edu.upc.legalai.dtos.response.SummariesConsultaResponseDTO;

import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/summaries")
@Tag(
        name = "Summaries",
        description = "Gestión de resúmenes del usuario autenticado"
)
public class SummariesController {

    private final ISummariesService summariesService;

    public SummariesController(ISummariesService summariesService) {
        this.summariesService = summariesService;
    }

    @Operation(summary = "Crear un resumen")
    @ApiResponse(
            responseCode = "201",
            description = "Resumen creado"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Documento o expediente inexistente o ajeno"
    )
    @PostMapping
    public ResponseEntity<SummariesResponseDTO> registrar(
            @Valid @RequestBody SummariesRequestDTO request
    ) {
        SummariesResponseDTO response = summariesService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(summary = "Listar resúmenes propios")
    @ApiResponse(
            responseCode = "200",
            description = "Listado obtenido"
    )
    @GetMapping
    public ResponseEntity<List<SummariesResponseDTO>> listar() {
        return ResponseEntity.ok(
                summariesService.listarPorUsuarioAutenticado()
        );
    }

    @Operation(
            summary = "Consultar resúmenes por filtros",
            description = "Consulta resúmenes propios de un expediente por "
                    + "tipo y rango de fechas de creación. Incluye los "
                    + "asociados directamente al expediente o a sus documentos. "
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
    public ResponseEntity<List<SummariesConsultaResponseDTO>>
    consultarPorExpedienteTipoYFecha(
            @Parameter(
                    description = "ID del expediente propio",
                    required = true
            )
            @RequestParam(name = "caseId", required = false)
            Long caseId,

            @Parameter(
                    description = "Tipo de resumen",
                    required = true
            )
            @RequestParam(name = "summaryType", required = false)
            String summaryType,

            @Parameter(
                    description = "Fecha inicial, formato yyyy-MM-dd",
                    required = true,
                    example = "2026-10-01"
            )
            @RequestParam(name = "createdFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdFrom,

            @Parameter(
                    description = "Fecha final, formato yyyy-MM-dd",
                    required = true,
                    example = "2026-10-05"
            )
            @RequestParam(name = "createdTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdTo
    ) {
        return ResponseEntity.ok(
                summariesService.consultarPorExpedienteTipoYFecha(
                        caseId,
                        summaryType,
                        createdFrom,
                        createdTo
                )
        );
    }

    @Operation(summary = "Buscar un resumen por ID")
    @ApiResponse(
            responseCode = "200",
            description = "Resumen obtenido"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Resumen inexistente o inaccesible"
    )
    @GetMapping("/{id}")
    public ResponseEntity<SummariesResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                summariesService.buscarPorId(id)
        );
    }

    @Operation(summary = "Actualizar un resumen")
    @ApiResponse(
            responseCode = "200",
            description = "Resumen actualizado"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Resumen, documento o expediente inexistente o ajeno"
    )
    @PutMapping("/{id}")
    public ResponseEntity<SummariesResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SummariesRequestDTO request
    ) {
        return ResponseEntity.ok(
                summariesService.actualizar(id, request)
        );
    }

    @Operation(summary = "Eliminar un resumen")
    @ApiResponse(
            responseCode = "204",
            description = "Resumen eliminado"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Resumen inexistente o inaccesible"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        summariesService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}