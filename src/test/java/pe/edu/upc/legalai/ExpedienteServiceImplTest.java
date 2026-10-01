package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.dtos.request.ExpedienteRequestDTO;
import pe.edu.upc.legalai.entities.Cliente;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.repositories.IClienteRepository;
import pe.edu.upc.legalai.repositories.IExpedienteRepository;
import pe.edu.upc.legalai.servicesimplements.ExpedienteServiceImplement;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesinterfaces.IUsuarioService;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExpedienteServiceImplTest {

    private IExpedienteRepository expedienteRepository;
    private IClienteRepository clienteRepository;
    private IUsuarioService usuarioService;
    private AuditLogService auditLogService;
    private ExpedienteServiceImplement service;

    private Usuario usuario;
    private Cliente cliente;

    @BeforeEach
    void setup() {
        expedienteRepository = mock(IExpedienteRepository.class);
        clienteRepository = mock(IClienteRepository.class);
        usuarioService = mock(IUsuarioService.class);
        auditLogService = mock(AuditLogService.class);
        service = new ExpedienteServiceImplement(expedienteRepository, clienteRepository, usuarioService, auditLogService);

        usuario = new Usuario();
        usuario.setUserId(1L);
        cliente = new Cliente();
        cliente.setClientId(2L);

        when(usuarioService.obtenerUsuarioAutenticado()).thenReturn(usuario);
        when(clienteRepository.findByClientIdAndOwnerUserId(2L, 1L)).thenReturn(Optional.of(cliente));
        when(expedienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(expedienteRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ExpedienteRequestDTO requestConTitulo(String titulo) {
        ExpedienteRequestDTO request = new ExpedienteRequestDTO();
        request.setClientId(2L);
        request.setTitle(titulo);
        return request;
    }

    @Test
    void registrarSinEstadoNiFechaUsaOpenYFechaActual() {
        var response = service.registrar(requestConTitulo("Demanda laboral"));

        assertEquals(EstadoExpediente.OPEN, response.getStatus());
        assertEquals(LocalDate.now(), response.getOpenedAt());
        assertNull(response.getClosedAt());
        verify(auditLogService).registrar(eq(usuario), eq("CREATE_CASE"), eq("Expediente"), any(), any());
        verify(auditLogService, never()).registrar(eq(usuario), eq("CLOSE_CASE"), any(), any(), any());
    }

    @Test
    void actualizarConFechaDeCierreEnExpedienteAbiertoLanzaBadRequest() {
        Expediente existente = new Expediente();
        existente.setCaseId(10L);
        existente.setClient(cliente);
        existente.setStatus(EstadoExpediente.OPEN);
        existente.setOpenedAt(LocalDate.now().minusDays(5));
        when(expedienteRepository.findByCaseIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(existente));

        ExpedienteRequestDTO request = requestConTitulo("Demanda laboral");
        request.setStatus(EstadoExpediente.OPEN);
        request.setClosedAt(LocalDate.now());

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.actualizar(10L, request));
        assertTrue(ex.getMessage().contains("no puede tener fecha de cierre"));
    }

    @Test
    void actualizarConFechaDeCierreAnteriorALaAperturaLanzaBadRequest() {
        Expediente existente = new Expediente();
        existente.setCaseId(10L);
        existente.setClient(cliente);
        existente.setStatus(EstadoExpediente.OPEN);
        existente.setOpenedAt(LocalDate.of(2026, 1, 10));
        when(expedienteRepository.findByCaseIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(existente));

        ExpedienteRequestDTO request = requestConTitulo("Demanda laboral");
        request.setStatus(EstadoExpediente.CLOSED);
        request.setOpenedAt(LocalDate.of(2026, 1, 10));
        request.setClosedAt(LocalDate.of(2026, 1, 5));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.actualizar(10L, request));
        assertTrue(ex.getMessage().contains("no puede ser anterior"));
    }

    @Test
    void actualizarACerradoSinFechaDeCierreLaAsignaAutomaticamente() {
        Expediente existente = new Expediente();
        existente.setCaseId(10L);
        existente.setClient(cliente);
        existente.setStatus(EstadoExpediente.OPEN);
        existente.setOpenedAt(LocalDate.now().minusDays(5));
        when(expedienteRepository.findByCaseIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(existente));

        ExpedienteRequestDTO request = requestConTitulo("Demanda laboral");
        request.setStatus(EstadoExpediente.CLOSED);

        var response = service.actualizar(10L, request);

        assertEquals(LocalDate.now(), response.getClosedAt());
        verify(auditLogService).registrar(eq(usuario), eq("CLOSE_CASE"), eq("Expediente"), eq(10L), any());
    }

    @Test
    void actualizarSinCambiarEstadoNoDisparaAuditoriaDeCierre() {
        Expediente existente = new Expediente();
        existente.setCaseId(10L);
        existente.setClient(cliente);
        existente.setStatus(EstadoExpediente.CLOSED);
        existente.setOpenedAt(LocalDate.now().minusDays(5));
        existente.setClosedAt(LocalDate.now().minusDays(1));
        when(expedienteRepository.findByCaseIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(existente));

        ExpedienteRequestDTO request = requestConTitulo("Titulo corregido");
        request.setStatus(EstadoExpediente.CLOSED);

        service.actualizar(10L, request);

        verify(auditLogService, never()).registrar(eq(usuario), eq("CLOSE_CASE"), any(), any(), any());
    }

    @Test
    void eliminarBorraElExpedientePropio() {
        Expediente existente = new Expediente();
        existente.setCaseId(10L);
        when(expedienteRepository.findByCaseIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(existente));

        service.eliminar(10L);

        verify(expedienteRepository).delete(existente);
    }
}
