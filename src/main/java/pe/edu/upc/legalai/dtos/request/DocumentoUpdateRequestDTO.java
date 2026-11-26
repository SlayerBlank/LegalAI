package pe.edu.upc.legalai.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * HU-025: metadatos editables de un documento. Solo la categoria: el nombre y la ubicacion del
 * archivo los fija la carga (HU-022) y se usan para verificar el archivo al descargarlo o procesarlo.
 */
@Schema(description = "Datos editables de un documento. Una categoria vacia o nula la elimina.")
public class DocumentoUpdateRequestDTO {

    @Size(max = 80, message = "La categoria no debe superar los 80 caracteres")
    @Schema(example = "Contratos")
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
