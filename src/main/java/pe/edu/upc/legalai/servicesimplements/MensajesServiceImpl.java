package pe.edu.upc.legalai.servicesimplements;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.DTOs.request.MensajeRequestDTO;
import pe.edu.upc.legalai.DTOs.response.MensajeResponseDTO;
import pe.edu.upc.legalai.entities.Mensajes;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.CitacionesIARepository;
import pe.edu.upc.legalai.repositories.MensajesRepository;
import pe.edu.upc.legalai.servicesinterfaces.MensajesService;

import java.util.List;

@Service
public class MensajesServiceImpl implements MensajesService {

    private final MensajesRepository mensajesRepository;
    private final CitacionesIARepository citacionesIARepository;
    private final ModelMapper modelMapper;

    public MensajesServiceImpl(MensajesRepository mensajesRepository,
                               CitacionesIARepository citacionesIARepository,
                               ModelMapper modelMapper) {
        this.mensajesRepository = mensajesRepository;
        this.citacionesIARepository = citacionesIARepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public MensajeResponseDTO crear(MensajeRequestDTO request) {
        Mensajes mensaje = new Mensajes();
        modelMapper.map(request, mensaje);
        return toResponse(mensajesRepository.save(mensaje));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MensajeResponseDTO> listar() {
        return mensajesRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MensajeResponseDTO buscarPorId(Long id) {
        return toResponse(getMensaje(id));
    }

    @Override
    @Transactional
    public MensajeResponseDTO actualizar(Long id, MensajeRequestDTO request) {
        Mensajes mensaje = getMensaje(id);
        modelMapper.map(request, mensaje);
        return toResponse(mensajesRepository.save(mensaje));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Mensajes mensaje = getMensaje(id);
        citacionesIARepository.deleteByMensajeMessageId(mensaje.getMessageId());
        mensajesRepository.delete(mensaje);
    }

    private Mensajes getMensaje(Long id) {
        return mensajesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));
    }

    private MensajeResponseDTO toResponse(Mensajes mensaje) {
        MensajeResponseDTO response = modelMapper.map(mensaje, MensajeResponseDTO.class);
        response.setMessageId(mensaje.getMessageId());
        response.setCreatedAt(mensaje.getCreatedAt());
        return response;
    }
}
