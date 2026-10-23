package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Rol;
import pe.edu.upc.legalai.repositories.IRolRepository;
import pe.edu.upc.legalai.dtos.response.RolResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.IRolService;

import java.util.List;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import pe.edu.upc.legalai.dtos.request.RolRequestDTO;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class RolServiceImplement implements IRolService {

    private final IRolRepository rolRepository;

    public RolServiceImplement(IRolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDTO> listar() {
        return rolRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RolResponseDTO registrar(RolRequestDTO request) {
        String name = normalizeName(request.name());
        if (isFundamental(name)) {
            throw new DuplicateResourceException("ADMIN y USER son roles reservados del sistema");
        }
        if (rolRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("El nombre del rol ya existe");
        }
        Rol rol = new Rol();
        rol.setName(name);
        rol.setDescription(request.description());
        return toResponse(rolRepository.saveAndFlush(rol));
    }

    @Override
    @Transactional(readOnly = true)
    public RolResponseDTO buscarPorId(Long id) {
        validateId(id);
        return toResponse(rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado")));
    }

    @Override
    @Transactional
    public RolResponseDTO actualizar(Long id, RolRequestDTO request) {
        Rol rol = getForUpdate(id);
        String name = normalizeName(request.name());
        if (!rol.getName().equals(name)) {
            if (isFundamental(rol.getName()) || isFundamental(name) || !rol.getUsuarios().isEmpty()) {
                throw new DuplicateResourceException("No se permite renombrar roles fundamentales o asociados a usuarios");
            }
        }
        if (rolRepository.existsByNameIgnoreCaseAndRoleIdNot(name, id)) {
            throw new DuplicateResourceException("El nombre del rol ya existe");
        }
        rol.setName(name);
        rol.setDescription(request.description());
        return toResponse(rolRepository.saveAndFlush(rol));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Rol rol = getForUpdate(id);
        if (isFundamental(rol.getName()) || !rol.getUsuarios().isEmpty()) {
            throw new DuplicateResourceException("No se puede eliminar un rol fundamental o asociado a usuarios");
        }
        rolRepository.delete(rol);
        rolRepository.flush();
    }

    private Rol getForUpdate(Long id) {
        validateId(id);
        return rolRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) throw new BadRequestException("El ID del rol debe ser positivo");
    }

    private String normalizeName(String name) {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]{0,49}")) {
            throw new BadRequestException("Nombre de rol invalido");
        }
        String normalized = name.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            throw new BadRequestException("El nombre debe omitir el prefijo ROLE_");
        }
        return normalized;
    }

    private boolean isFundamental(String name) {
        return "ADMIN".equalsIgnoreCase(name) || "USER".equalsIgnoreCase(name);
    }

    private RolResponseDTO toResponse(Rol rol) {
        return new RolResponseDTO(rol.getRoleId(), rol.getName(), rol.getDescription());
    }
}
