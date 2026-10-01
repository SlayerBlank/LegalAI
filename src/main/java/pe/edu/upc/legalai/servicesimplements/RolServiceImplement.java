package pe.edu.upc.legalai.servicesimplements;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.legalai.entities.Rol;
import pe.edu.upc.legalai.repositories.IRolRepository;
import pe.edu.upc.legalai.dtos.response.RolResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.IRolService;

import java.util.List;

@Service
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

    private RolResponseDTO toResponse(Rol rol) {
        return new RolResponseDTO(rol.getRoleId(), rol.getName(), rol.getDescription());
    }
}
