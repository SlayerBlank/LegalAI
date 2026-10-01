package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.dtos.response.RolResponseDTO;

import java.util.List;

public interface IRolService {

    List<RolResponseDTO> listar();
}
