package pe.edu.upc.legalai.DTOs.request;

import jakarta.validation.constraints.*;

public class SemanticSearchRequestDTO {
    @NotBlank
    private String query;
    @NotNull @Min(1) @Max(20)
    private Integer topK = 5;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}
