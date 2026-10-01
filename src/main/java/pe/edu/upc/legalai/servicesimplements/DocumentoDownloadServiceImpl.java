package pe.edu.upc.legalai.servicesimplements;

import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IDocumentoRepository;
import pe.edu.upc.legalai.servicesinterfaces.DocumentoDownloadService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;

@Service
public class DocumentoDownloadServiceImpl implements DocumentoDownloadService {
    private final IDocumentoRepository documents;
    private final IUsuarioService users;
    private final DocumentStorage storage;

    public DocumentoDownloadServiceImpl(IDocumentoRepository documents,
            IUsuarioService users, DocumentStorage storage) {
        this.documents = documents;
        this.users = users;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public Download descargar(Long documentId) {
        Long userId = users.obtenerUsuarioAutenticado().getUserId();
        var document = documents.findByDocumentIdAndExpedienteOwnerUserId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        storage.verifyUpload(document);
        try {
            var path = storage.resolve(document.getStorageUrl());
            long size = Files.size(path);
            String name = document.getFileName().replaceAll("[\\p{Cntrl}\\\\/\"]", "_");
            var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS);
            return new Download(new InputStreamResource(input), name, size);
        } catch (IOException | InvalidPathException | SecurityException ex) {
            throw new ResourceNotFoundException("Archivo del documento no disponible");
        }
    }
}
