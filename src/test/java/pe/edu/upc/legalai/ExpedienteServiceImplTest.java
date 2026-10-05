package pe.edu.upc.legalai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
import java.util.List;
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

    private Expediente expediente(Long caseId, EstadoExpediente status, LocalDate openedAt) {
        Expediente expediente = new Expediente();
        expediente.setCaseId(caseId);
        expediente.setOwner(usuario);
        expediente.setClient(cliente);
        expediente.setTitle("Expediente " + caseId);
        expediente.setStatus(status);
        expediente.setOpenedAt(openedAt);
        return expediente;
    }

    @Test
    void registrarSinEstadoNiFechaUsaOpenYFechaActual() {
        var response = service.registrar(requestConTitulo("Demanda laboral"));

        assertEquals(EstadoExpediente.OPEN, response.getStatus());
        assertEquals(LocalDate.now(), response.getOpenedAt());
        assertNull(response.getClosedAt());
        verify(auditLogService).registrar(eq(usuario), eq("CREATE_CASE"), eq("Expediente"), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listarPorEstadoYFechaDelegaEnElRepositorioConLosFiltrosYElUsuarioAutenticado() {
        Expediente abierto = expediente(10L, EstadoExpediente.OPEN, LocalDate.of(2026, 3, 1));
        when(expedienteRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(abierto));

        var result = service.listarPorEstadoYFecha(EstadoExpediente.OPEN,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getCaseId());
        verify(usuarioService).obtenerUsuarioAutenticado();
        verify(expedienteRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listarPorEstadoYFechaSinFiltrosNoLanzaErrorYConsultaAlRepositorio() {
        when(expedienteRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Expediente>>any(),
                any(Sort.class))).thenReturn(List.of());

        var result = service.listarPorEstadoYFecha(null, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void listarPorEstadoYFechaConRangoInvertidoLanzaBadRequestYNoConsultaElRepositorio() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.listarPorEstadoYFecha(null, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 1, 1)));

        assertTrue(ex.getMessage().contains("openedFrom"));
        verifyNoInteractions(expedienteRepository);
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
