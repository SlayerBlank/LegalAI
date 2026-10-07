package pe.edu.upc.legalai.controllers;

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
public class DraftsController {

    private final IDraftsService draftsService;

    public DraftsController(IDraftsService draftsService) {
        this.draftsService = draftsService;
    }

    @PostMapping
    public ResponseEntity<DraftsResponseDTO> registrar(
            @Valid @RequestBody DraftsRequestDTO request
    ) {
        DraftsResponseDTO response = draftsService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<DraftsResponseDTO>> listar() {
        return ResponseEntity.ok(
                draftsService.listarPorUsuarioAutenticado()
        );
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<DraftsResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                draftsService.buscarPorId(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<DraftsResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DraftsRequestDTO request
    ) {
        return ResponseEntity.ok(
                draftsService.actualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        draftsService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}