package pe.edu.upc.legalai.dtos.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import pe.edu.upc.legalai.configs.RAGSettings;

public class RAGRequestDTO {
    @NotBlank @Size(max = RAGSettings.MAX_QUESTION_CHARS)
    private String question;
    @Min(1) @Max(10)
    @Schema(description = "Si se omite, utiliza legalai.rag.default-top-k (5 por defecto)", example = "5")
    private Integer topK;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Integer getTopK() { return topK; }
    @JsonSetter(nulls = Nulls.FAIL)
    public void setTopK(Integer topK) { this.topK = topK; }
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Solo se permiten question y topK");
    }
}
