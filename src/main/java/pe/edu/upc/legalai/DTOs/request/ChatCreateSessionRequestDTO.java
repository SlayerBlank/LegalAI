package pe.edu.upc.legalai.DTOs.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ChatCreateSessionRequestDTO {

    @NotNull(message = "caseId es obligatorio")
    @Positive(message = "caseId debe ser un identificador valido")
    @Schema(description = "Expediente propio del usuario autenticado", example = "1")
    private Long caseId;

    @Positive(message = "documentId debe ser un identificador valido")
    @Schema(description = "Documento opcional del mismo expediente. Si se omite, la sesion usa todos los documentos del expediente",
            example = "5")
    private Long documentId;

    @Size(max = 150, message = "El titulo no puede superar 150 caracteres")
    @Schema(description = "Titulo de la conversacion. Si se omite se usa uno por defecto", example = "Analisis inicial del expediente")
    private String title;

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }

    public Long getDocumentId() { return documentId; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Solo se permiten caseId, documentId y title");
    }
}