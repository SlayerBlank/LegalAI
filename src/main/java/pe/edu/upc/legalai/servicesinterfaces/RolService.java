package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.schemas.dtos.response.RolResponseDTO;

import java.util.List;

public interface RolService {

    List<RolResponseDTO> listar();
}
