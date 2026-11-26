package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoTextResponseDTO;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;

import java.time.Duration;
import java.time.LocalDateTime;

public interface DocumentoProcessingService {

    /** HU-058: tiempo tras el cual un documento en PROCESSING se considera trabado y se puede reintentar. */
    Duration TIEMPO_MAXIMO_PROCESAMIENTO = Duration.ofMinutes(10);

    DocumentoResponseDTO procesar(Long documentId);

    DocumentoTextResponseDTO obtenerTexto(Long documentId);

    /** true si el documento esta en PROCESSING y se actualizo hace menos de TIEMPO_MAXIMO_PROCESAMIENTO. */
    static boolean procesamientoEnCurso(EstadoProcesamiento status, LocalDateTime updatedAt) {
        return status == EstadoProcesamiento.PROCESSING
                && updatedAt != null
                && updatedAt.isAfter(LocalDateTime.now().minus(TIEMPO_MAXIMO_PROCESAMIENTO));
    }
}
