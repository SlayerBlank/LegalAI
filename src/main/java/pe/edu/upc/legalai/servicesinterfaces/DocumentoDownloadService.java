package pe.edu.upc.legalai.servicesinterfaces;

import org.springframework.core.io.Resource;

public interface DocumentoDownloadService {
    record Download(Resource resource, String fileName, long sizeBytes) { }
    Download descargar(Long documentId);
}
