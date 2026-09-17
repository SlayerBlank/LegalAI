package pe.edu.upc.legalai.services.implementations;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.dtos.user.UserRequestDTO;
import pe.edu.upc.legalai.dtos.user.UserResponseDTO;
import pe.edu.upc.legalai.entities.Role;
import pe.edu.upc.legalai.entities.User;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.RoleRepository;
import pe.edu.upc.legalai.repositories.UserRepository;
import pe.edu.upc.legalai.services.interfaces.UserService;

import java.util.List;

@Service
public class    UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo: " + request.getEmail());
        }
        Role role = getRole(request.getRoleId());
        User user = new User();
        user.setRole(role);
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setActive(request.getActive() != null ? request.getActive() : true);
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        return toResponse(getUser(id));
    }

    @Override
    @Transactional
    public UserResponseDTO update(Long id, UserRequestDTO request) {
        User user = getUser(id);
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo: " + request.getEmail());
        }
        Role role = getRole(request.getRoleId());
        user.setRole(role);
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = getUser(id);
        userRepository.delete(user);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private Role getRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id: " + roleId));
    }

    private UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(
                user.getUserId(),
                user.getRole().getRoleId(),
                user.getRole().getName(),
                user.getFullName(),
                user.getEmail(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
