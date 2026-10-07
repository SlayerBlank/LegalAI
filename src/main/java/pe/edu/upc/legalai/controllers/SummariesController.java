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
import pe.edu.upc.legalai.dtos.request.SummariesRequestDTO;
import pe.edu.upc.legalai.dtos.response.SummariesConsultaResponseDTO;
import pe.edu.upc.legalai.dtos.response.SummariesResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.ISummariesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/summaries")
public class SummariesController {

    private final ISummariesService summariesService;

    public SummariesController(ISummariesService summariesService) {
        this.summariesService = summariesService;
    }

    @PostMapping
    public ResponseEntity<SummariesResponseDTO> registrar(
            @Valid @RequestBody SummariesRequestDTO request
    ) {
        SummariesResponseDTO response = summariesService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<SummariesResponseDTO>> listar() {
        return ResponseEntity.ok(
                summariesService.listarPorUsuarioAutenticado()
        );
    }

    @GetMapping("/filter")
    public ResponseEntity<List<SummariesConsultaResponseDTO>>
    consultarPorExpedienteTipoYFecha(
            @RequestParam(name = "caseId", required = false)
            Long caseId,

            @RequestParam(name = "summaryType", required = false)
            String summaryType,

            @RequestParam(name = "createdFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdFrom,

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

    @GetMapping("/{id}")
    public ResponseEntity<SummariesResponseDTO> buscarPorId(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                summariesService.buscarPorId(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SummariesResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SummariesRequestDTO request
    ) {
        return ResponseEntity.ok(
                summariesService.actualizar(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id
    ) {
        summariesService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}