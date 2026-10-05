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