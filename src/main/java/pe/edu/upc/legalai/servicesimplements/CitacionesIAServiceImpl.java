package pe.edu.upc.legalai.servicesimplements;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.DTOs.request.CitacionIARequestDTO;
import pe.edu.upc.legalai.DTOs.response.CitacionIAResponseDTO;
import pe.edu.upc.legalai.entities.CitacionesIA;
import pe.edu.upc.legalai.entities.Documento;
import pe.edu.upc.legalai.entities.Mensajes;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.CitacionesIARepository;
import pe.edu.upc.legalai.repositories.DocumentoRepository;
import pe.edu.upc.legalai.repositories.MensajesRepository;
import pe.edu.upc.legalai.servicesinterfaces.CitacionesIAService;

import java.util.List;

@Service
public class CitacionesIAServiceImpl implements CitacionesIAService {

    private final CitacionesIARepository citacionesIARepository;
    private final MensajesRepository mensajesRepository;
    private final DocumentoRepository documentoRepository;
    private final ModelMapper modelMapper;

    public CitacionesIAServiceImpl(CitacionesIARepository citacionesIARepository,
                                   MensajesRepository mensajesRepository,
                                   DocumentoRepository documentoRepository,
                                   ModelMapper modelMapper) {
        this.citacionesIARepository = citacionesIARepository;
        this.mensajesRepository = mensajesRepository;
        this.documentoRepository = documentoRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public CitacionIAResponseDTO crear(CitacionIARequestDTO request) {
        CitacionesIA citacion = new CitacionesIA();
        applyRequest(citacion, request);
        return toResponse(citacionesIARepository.save(citacion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitacionIAResponseDTO> listar() {
        return citacionesIARepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CitacionIAResponseDTO> listarPorMensaje(Long messageId) {
        getMensaje(messageId);
        return citacionesIARepository.findByMensajeMessageIdOrderByCitationIdAsc(messageId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CitacionIAResponseDTO buscarPorId(Long id) {
        return toResponse(getCitacion(id));
    }

    @Override
    @Transactional
    public CitacionIAResponseDTO actualizar(Long id, CitacionIARequestDTO request) {
        CitacionesIA citacion = getCitacion(id);
        applyRequest(citacion, request);
        return toResponse(citacionesIARepository.save(citacion));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        citacionesIARepository.delete(getCitacion(id));
    }

    private void applyRequest(CitacionesIA citacion, CitacionIARequestDTO request) {
        modelMapper.map(request, citacion);
        citacion.setMensaje(getMensaje(request.getMessageId()));
        citacion.setDocumento(request.getDocumentId() == null
                ? null
                : documentoRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado")));
    }

    private Mensajes getMensaje(Long id) {
        return mensajesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));
    }

    private CitacionesIA getCitacion(Long id) {
        return citacionesIARepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Citación IA no encontrada"));
    }

    private CitacionIAResponseDTO toResponse(CitacionesIA citacion) {
        CitacionIAResponseDTO response = modelMapper.map(citacion, CitacionIAResponseDTO.class);
        response.setCitationId(citacion.getCitationId());
        response.setMessageId(citacion.getMensaje().getMessageId());
        response.setDocumentId(citacion.getDocumento() == null ? null : citacion.getDocumento().getDocumentId());
        response.setChunkId(citacion.getChunkId());
        response.setRelevanceScore(citacion.getRelevanceScore());
        return response;
    }
}
