package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.request.DocumentoUpdateRequestDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoPendienteResponseDTO;

import pe.edu.upc.legalai.dtos.request.DocumentoRequestDTO;
import pe.edu.upc.legalai.dtos.response.DocumentoResponseDTO;
import pe.edu.upc.legalai.entities.EstadoProcesamiento;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface IDocumentoService {

    DocumentoResponseDTO subirArchivo(Long caseId, MultipartFile file, String category);

    DocumentoResponseDTO registrar(Long caseId, DocumentoRequestDTO request);

    List<DocumentoResponseDTO> listarPorExpediente(Long caseId);

    List<DocumentoPendienteResponseDTO> listarPendientesRevision(Long abogadoId, EstadoProcesamiento status);

    DocumentoResponseDTO buscarPorId(Long id);

    DocumentoResponseDTO actualizar(Long id, DocumentoUpdateRequestDTO request);

    void eliminar(Long id);
}
