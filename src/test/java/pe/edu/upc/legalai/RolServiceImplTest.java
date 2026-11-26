package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.dtos.request.RolRequestDTO;
import pe.edu.upc.legalai.entities.Rol;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.DuplicateResourceException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IRolRepository;
import pe.edu.upc.legalai.servicesimplements.RolServiceImplement;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    // ---------- HU-009: consultar rol

    @Test
    void buscarPorIdDevuelveLosDatosDelRol() {
        when(rolRepository.findById(3L)).thenReturn(Optional.of(rol(3L, "REVIEWER", "Revisor")));

        var result = service.buscarPorId(3L);

        assertEquals(3L, result.getRoleId());
        assertEquals("REVIEWER", result.getName());
        assertEquals("Revisor", result.getDescription());
    }

    @Test
    void buscarPorIdInexistenteLanzaResourceNotFound() {
        when(rolRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(99L));
        assertEquals("Rol no encontrado", ex.getMessage());
    }

    @Test
    void buscarPorIdNoPositivoLanzaBadRequest() {
        assertThrows(BadRequestException.class, () -> service.buscarPorId(0L));
        verifyNoInteractions(rolRepository);
    }

    // ---------- HU-010: actualizar rol

    @Test
    void actualizarCambiaNombreYDescripcionDeUnRolSinUsuarios() {
        Rol existente = rol(3L, "REVIEWER", "Revisor");
        when(rolRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(existente));
        when(rolRepository.existsByNameIgnoreCaseAndRoleIdNot("AUDITOR", 3L)).thenReturn(false);
        when(rolRepository.saveAndFlush(existente)).thenReturn(existente);

        var result = service.actualizar(3L, new RolRequestDTO("auditor", "Audita"));

        assertEquals("AUDITOR", result.getName());
        assertEquals("Audita", result.getDescription());
    }

    @Test
    void actualizarConNombreDuplicadoLanzaConflictoYNoGuarda() {
        when(rolRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(rol(3L, "REVIEWER", null)));
        when(rolRepository.existsByNameIgnoreCaseAndRoleIdNot("AUDITOR", 3L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.actualizar(3L, new RolRequestDTO("AUDITOR", null)));
        verify(rolRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarRenombrandoUnRolFundamentalLanzaConflicto() {
        when(rolRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(rol(1L, "ADMIN", "Administrador")));

        assertThrows(DuplicateResourceException.class, () -> service.actualizar(1L, new RolRequestDTO("JEFE", null)));
        verify(rolRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarRolInexistenteLanzaResourceNotFound() {
        when(rolRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(99L, new RolRequestDTO("AUDITOR", null)));
    }

    // ---------- HU-011: eliminar rol

    @Test
    void eliminarBorraUnRolSinUsuarios() {
        Rol existente = rol(3L, "REVIEWER", null);
        when(rolRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(existente));

        service.eliminar(3L);

        verify(rolRepository).delete(existente);
    }

    @Test
    void eliminarRolConUsuariosLanzaConflictoYNoBorra() {
        Rol existente = rol(3L, "REVIEWER", null);
        existente.getUsuarios().add(new Usuario());
        when(rolRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(existente));

        assertThrows(DuplicateResourceException.class, () -> service.eliminar(3L));
        verify(rolRepository, never()).delete(any(Rol.class));
    }

    @Test
    void eliminarRolFundamentalLanzaConflicto() {
        when(rolRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(rol(2L, "USER", null)));

        assertThrows(DuplicateResourceException.class, () -> service.eliminar(2L));
        verify(rolRepository, never()).delete(any(Rol.class));
    }

    @Test
    void eliminarRolInexistenteLanzaResourceNotFound() {
        when(rolRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(99L));
    }
}
