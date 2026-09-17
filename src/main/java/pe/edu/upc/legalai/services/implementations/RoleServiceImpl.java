package pe.edu.upc.legalai.services.implementations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.role.RoleRequestDTO;
import pe.edu.upc.legalai.dtos.role.RoleResponseDTO;
import pe.edu.upc.legalai.entities.Role;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.RoleRepository;
import pe.edu.upc.legalai.services.interfaces.RoleService;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public RoleResponseDTO create(RoleRequestDTO request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Ya existe un rol con el nombre: " + request.getName());
        }
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponseDTO> findAll() {
        return roleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponseDTO findById(Long id) {
        return toResponse(getRole(id));
    }

    @Override
    @Transactional
    public RoleResponseDTO update(Long id, RoleRequestDTO request) {
        Role role = getRole(id);
        if (!role.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Ya existe un rol con el nombre: " + request.getName());
        }
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = getRole(id);
        roleRepository.delete(role);
    }

    private Role getRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + id));
    }

    private RoleResponseDTO toResponse(Role role) {
        return new RoleResponseDTO(role.getRoleId(), role.getName(), role.getDescription());
    }
}
