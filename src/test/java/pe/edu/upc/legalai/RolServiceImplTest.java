package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.entities.Rol;
import pe.edu.upc.legalai.repositories.IRolRepository;
import pe.edu.upc.legalai.servicesimplements.RolServiceImplement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RolServiceImplTest {

    private IRolRepository rolRepository;
    private RolServiceImplement service;

    @BeforeEach
    void setup() {
        rolRepository = mock(IRolRepository.class);
        service = new RolServiceImplement(rolRepository);
    }

    private Rol rol(Long id, String name, String description) {
        Rol rol = new Rol();
        rol.setRoleId(id);
        rol.setName(name);
        rol.setDescription(description);
        return rol;
    }

    @Test
    void listarMapeaTodosLosRolesDelRepositorio() {
        when(rolRepository.findAll()).thenReturn(List.of(
                rol(1L, "ADMIN", "Administrador"),
                rol(2L, "USER", "Usuario regular")));

        var result = service.listar();

        assertEquals(2, result.size());
        assertEquals("ADMIN", result.get(0).getName());
        assertEquals("Usuario regular", result.get(1).getDescription());
    }

    @Test
    void listarSinRolesDevuelveListaVacia() {
        when(rolRepository.findAll()).thenReturn(List.of());

        assertTrue(service.listar().isEmpty());
    }
}
