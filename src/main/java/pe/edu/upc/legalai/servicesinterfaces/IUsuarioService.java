package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.dtos.response.UsuarioResponseDTO;

public interface IUsuarioService {

    Usuario obtenerUsuarioAutenticado();

    UsuarioResponseDTO obtenerPerfilAutenticado();
}
