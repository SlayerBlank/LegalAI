package pe.edu.upc.legalai.services.interfaces;

import pe.edu.upc.legalai.dtos.role.RoleRequestDTO;
import pe.edu.upc.legalai.dtos.role.RoleResponseDTO;

import java.util.List;

public interface RoleService {

    RoleResponseDTO create(RoleRequestDTO request);

    List<RoleResponseDTO> findAll();

    RoleResponseDTO findById(Long id);

    RoleResponseDTO update(Long id, RoleRequestDTO request);

    void delete(Long id);
}
