package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.response.*;
import java.util.List;

public interface DocumentChunkService {
    ChunkGenerationResponseDTO generar(Long documentId);
    List<DocumentChunkResponseDTO> listar(Long documentId);
}
