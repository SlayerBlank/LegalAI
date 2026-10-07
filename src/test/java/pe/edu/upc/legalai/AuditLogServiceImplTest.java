package pe.edu.upc.legalai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.edu.upc.legalai.entities.AuditLog;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.BadRequestException;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IAuditLogRepository;
import pe.edu.upc.legalai.servicesimplements.AuditLogServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuditLogServiceImplTest {

    private IAuditLogRepository auditLogRepository;
    private AuditLogServiceImpl service;

    @BeforeEach
    void setup() {
        auditLogRepository = mock(IAuditLogRepository.class);
        service = new AuditLogServiceImpl(auditLogRepository);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComoAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@example.com", "x", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private void autenticarComoUsuarioRegular() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@example.com", "x", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private AuditLog entry(Long id) {
        Usuario usuario = new Usuario();
        usuario.setUserId(7L);
        AuditLog log = new AuditLog();
        log.setLogId(id);
        log.setUsuario(usuario);
        log.setAction("CREATE_CLIENT");
        log.setEntityType("Cliente");
        log.setEntityId(3L);
        log.setDetails("detalle");
        return log;
    }

    @Test
    void registrarGuardaElLogConLosDatosRecibidos() {
        Usuario usuario = new Usuario();
        usuario.setUserId(1L);

        service.registrar(usuario, "CREATE_CLIENT", "Cliente", 3L, "detalle");

        var captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals(usuario, captor.getValue().getUsuario());
        assertEquals("CREATE_CLIENT", captor.getValue().getAction());
        assertEquals("Cliente", captor.getValue().getEntityType());
        assertEquals(3L, captor.getValue().getEntityId());
        assertEquals("detalle", captor.getValue().getDetails());
    }

    @Test
    void buscarSinRolAdminLanzaAccessDenied() {
        autenticarComoUsuarioRegular();

        assertThrows(AccessDeniedException.class,
                () -> service.buscar(7L, "CREATE_CLIENT", "Cliente", null, null, Pageable.unpaged()));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void buscarConRangoDeFechasInvertidoLanzaBadRequest() {
        autenticarComoAdmin();
        LocalDateTime from = LocalDateTime.of(2026, 6, 30, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertThrows(BadRequestException.class,
                () -> service.buscar(null, null, null, from, to, Pageable.unpaged()));
    }

    @Test
    void buscarConservaPaginacionOrdenYDatosDelRegistro() {
        autenticarComoAdmin();
        var pageable = PageRequest.of(1, 2, Sort.by("createdAt").descending());
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entry(9L)), pageable, 5));

        Page<pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO> result =
                service.buscar(7L, "CREATE_CLIENT", "Cliente", null, null, pageable);

        assertEquals(5, result.getTotalElements());
        assertEquals(9L, result.getContent().get(0).logId());
        assertEquals(7L, result.getContent().get(0).userId());
    }

    @Test
    void buscarConsultaAcademicaSinRolAdminLanzaAccessDenied() {
        autenticarComoUsuarioRegular();

        assertThrows(AccessDeniedException.class, () -> service.buscarConsultaAcademica(
                7L, "CREATE_CLIENT", null, null, null, Pageable.unpaged()));
    }

    @Test
    void buscarConsultaAcademicaConMenosDeDosFiltrosLanzaBadRequest() {
        autenticarComoAdmin();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.buscarConsultaAcademica(
                7L, null, null, null, null, Pageable.unpaged()));
        assertTrue(ex.getMessage().contains("al menos dos filtros"));
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void buscarConsultaAcademicaConFechasInvertidasLanzaBadRequest() {
        autenticarComoAdmin();

        assertThrows(BadRequestException.class, () -> service.buscarConsultaAcademica(
                7L, "CREATE_CLIENT", null, LocalDate.of(2026, 6, 30), LocalDate.of(2026, 1, 1), Pageable.unpaged()));
    }

    @Test
    void buscarConsultaAcademicaConDosFiltrosDelegaConOrdenDeterministico() {
        autenticarComoAdmin();
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entry(9L))));

        var result = service.buscarConsultaAcademica(7L, "CREATE_CLIENT", null, null, null,
                PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        var pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("logId")), pageableCaptor.getValue().getSort());
    }

    @Test
    void obtenerPorIdSinRolAdminLanzaAccessDenied() {
        autenticarComoUsuarioRegular();

        assertThrows(AccessDeniedException.class, () -> service.obtenerPorId(9L));
    }

    @Test
    void obtenerPorIdInexistenteLanzaResourceNotFound() {
        autenticarComoAdmin();
        when(auditLogRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorId(10L));
    }

    @Test
    void obtenerPorIdExistenteDevuelveElDto() {
        autenticarComoAdmin();
        when(auditLogRepository.findById(9L)).thenReturn(Optional.of(entry(9L)));

        assertEquals(9L, service.obtenerPorId(9L).logId());
    }

    @Test
    void listarPorUsuarioSinRolAdminLanzaAccessDenied() {
        autenticarComoUsuarioRegular();

        assertThrows(AccessDeniedException.class, () -> service.listarPorUsuario(7L));
    }

    @Test
    void listarPorUsuarioConservaOrdenDelRepositorio() {
        autenticarComoAdmin();
        when(auditLogRepository.findByUsuarioUserIdOrderByCreatedAtDescLogIdDesc(7L))
                .thenReturn(List.of(entry(9L), entry(4L)));

        assertEquals(List.of(9L, 4L), service.listarPorUsuario(7L).stream()
                .map(pe.edu.upc.legalai.dtos.response.AuditLogResponseDTO::logId).toList());
    }
}
