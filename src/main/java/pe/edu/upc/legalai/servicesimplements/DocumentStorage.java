package pe.edu.upc.legalai.servicesimplements;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IAuditLogRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DocumentStorage {
    private final Path storageRoot;
    private final IAuditLogRepository audit;

    public DocumentStorage(@Value("${legalai.storage.path}") String storagePath, IAuditLogRepository audit) {
        this.storageRoot = Path.of(storagePath).toAbsolutePath().normalize();
        this.audit = audit;
    }

    public void verifyUpload(Documento document) {
        // The metadata endpoint accepts a client-supplied storageUrl. The server-written
        // multipart audit is the existing provenance evidence; a URL alone is not ownership.
        boolean uploaded = audit.existsByActionAndEntityTypeAndEntityIdAndUsuarioUserIdAndDetails(
                "UPLOAD_DOCUMENT", "Documento", document.getDocumentId(), document.getUploadedBy().getUserId(),
                "caseId=" + document.getExpediente().getCaseId() + "; fileName=" + document.getFileName());
        if (!uploaded) throw new ResourceNotFoundException("El documento no tiene un archivo de carga verificable");
    }

    public Path resolve(String reference) throws IOException {
        if (reference == null || reference.isBlank() || reference.contains("\\") || reference.contains(":")) {
            throw new IOException("Referencia de storage invalida");
        }
        Path relative = Path.of(reference);
        if (relative.isAbsolute() || relative.getNameCount() > 2) throw new IOException("Referencia de storage invalida");
        for (Path component : relative) {
            if (component.toString().equals("..") || component.toString().equals(".")) {
                throw new IOException("Referencia de storage invalida");
            }
        }
        if (relative.getNameCount() == 2) {
            if (!relative.getName(0).toString().equals("uploads")
                    && !relative.getName(0).equals(storageRoot.getFileName())) {
                throw new IOException("Prefijo de storage invalido");
            }
            relative = relative.getFileName();
        }
        Path root = storageRoot.toRealPath();
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new IOException("Archivo fuera del storage");
        Path real = target.toRealPath();
        if (!real.startsWith(root) || !Files.isRegularFile(real) || !Files.isReadable(real)) {
            throw new IOException("Archivo PDF no disponible en storage");
        }
        return real;
    }
}
