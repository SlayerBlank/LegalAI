package pe.edu.upc.legalai.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.legalai.schemas.dtos.response.UsuarioResponseDTO;
import pe.edu.upc.legalai.servicesinterfaces.UsuarioService;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios", description = "Perfil del usuario autenticado")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Obtener mi perfil", description = "Devuelve los datos publicos del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Perfil obtenido")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> me() {
        return ResponseEntity.ok(usuarioService.obtenerPerfilAutenticado());
    }
}
