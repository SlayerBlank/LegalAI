package pe.edu.upc.legalai.servicesinterfaces;

import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.dtos.response.UsuarioResponseDTO;
import pe.edu.upc.legalai.dtos.request.UsuarioUpdateRequestDTO;
import java.util.List;

public interface IUsuarioService {

    Usuario obtenerUsuarioAutenticado();

    UsuarioResponseDTO obtenerPerfilAutenticado();

    List<UsuarioResponseDTO> listar();

    UsuarioResponseDTO buscarPorId(Long id);

    UsuarioResponseDTO actualizar(Long id, UsuarioUpdateRequestDTO request);

    void eliminar(Long id);
}
