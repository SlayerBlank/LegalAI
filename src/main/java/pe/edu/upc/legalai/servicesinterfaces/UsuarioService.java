package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.schemas.dtos.response.UsuarioResponseDTO;

public interface UsuarioService {

    Usuario obtenerUsuarioAutenticado();

    UsuarioResponseDTO obtenerPerfilAutenticado();
}
