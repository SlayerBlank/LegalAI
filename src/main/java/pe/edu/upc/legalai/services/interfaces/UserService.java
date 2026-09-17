package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.user.UserRequestDTO;
import pe.edu.upc.legalai.dtos.user.UserResponseDTO;

import java.util.List;

public interface UserService {

    UserResponseDTO create(UserRequestDTO request);

    List<UserResponseDTO> findAll();

    UserResponseDTO findById(Long id);

    UserResponseDTO update(Long id, UserRequestDTO request);

    void delete(Long id);
}
