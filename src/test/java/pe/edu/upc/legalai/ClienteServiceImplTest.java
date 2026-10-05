package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.dtos.request.ClienteRequestDTO;
import pe.edu.upc.legalai.entities.Cliente;
import pe.edu.upc.legalai.entities.TipoCliente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IClienteRepository;
import pe.edu.upc.legalai.servicesimplements.ClienteServiceImplement;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClienteServiceImplTest {

    private IClienteRepository clienteRepository;
    private IUsuarioService usuarioService;
    private AuditLogService auditLogService;
    private ClienteServiceImplement service;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        clienteRepository = mock(IClienteRepository.class);
        usuarioService = mock(IUsuarioService.class);
        auditLogService = mock(AuditLogService.class);
        service = new ClienteServiceImplement(clienteRepository, usuarioService, auditLogService);

        usuario = new Usuario();
        usuario.setUserId(1L);
        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clienteRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ClienteRequestDTO request(String nombre) {
        ClienteRequestDTO request = new ClienteRequestDTO();
        request.setClientType(TipoCliente.PERSON);
        request.setFullNameOrCompany(nombre);
        request.setEmail("cliente@example.com");
        return request;
    }

    @Test
    void registrarAsociaElClienteAlUsuarioAutenticadoYAuditoria() {
        var response = service.registrar(request("Juan Perez"));

        assertEquals("Juan Perez", response.getFullNameOrCompany());
        assertEquals(TipoCliente.PERSON, response.getClientType());
        verify(auditLogService).registrar(eq(usuario), eq("CREATE_CLIENT"), eq("Cliente"), any(), eq("Juan Perez"));
    }

    @Test
    void buscarPorIdInexistenteLanzaResourceNotFound() {
        when(clienteRepository.findByClientIdAndOwnerUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(99L));
    }

    @Test
    void actualizarModificaLosDatosYRegistraAuditoria() {
        Cliente existente = new Cliente();
        existente.setClientId(5L);
        existente.setFullNameOrCompany("Nombre viejo");
        when(clienteRepository.findByClientIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(existente));

        var response = service.actualizar(5L, request("Nombre nuevo"));

        assertEquals("Nombre nuevo", response.getFullNameOrCompany());
        verify(auditLogService).registrar(eq(usuario), eq("UPDATE_CLIENT"), eq("Cliente"), eq(5L), eq("Nombre nuevo"));
    }

    @Test
    void eliminarBorraElClientePropioYRegistraAuditoria() {
        Cliente existente = new Cliente();
        existente.setClientId(5L);
        existente.setFullNameOrCompany("Juan Perez");
        when(clienteRepository.findByClientIdAndOwnerUserId(5L, 1L)).thenReturn(Optional.of(existente));

        service.eliminar(5L);

        verify(clienteRepository).delete(existente);
        verify(auditLogService).registrar(eq(usuario), eq("DELETE_CLIENT"), eq("Cliente"), eq(5L), eq("Juan Perez"));
    }
}
