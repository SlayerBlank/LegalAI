package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.upc.legalai.entities.AuditLog;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.exceptions.ResourceNotFoundException;
import pe.edu.upc.legalai.repositories.IAuditLogRepository;
import pe.edu.upc.legalai.servicesimplements.AuditLogServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditLogServiceImplTest {
    private final IAuditLogRepository repository = mock(IAuditLogRepository.class);
    private final AuditLogServiceImpl service = new AuditLogServiceImpl(repository);

    @Test
    void buscarConservaPaginacionOrdenYDatosDelRegistro() {
        var pageable = PageRequest.of(1, 2, Sort.by("createdAt").descending());
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<AuditLog>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entry(9L)), pageable, 5));

        var result = service.buscar(7L, "CREATE_CLIENT", "Cliente", null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getPageable()).isEqualTo(pageable);
        assertThat(result.getContent()).singleElement().satisfies(dto -> {
            assertThat(dto.getLogId()).isEqualTo(9L);
            assertThat(dto.getUserId()).isEqualTo(7L);
            assertThat(dto.getAction()).isEqualTo("CREATE_CLIENT");
            assertThat(dto.getEntityType()).isEqualTo("Cliente");
            assertThat(dto.getEntityId()).isEqualTo(3L);
            assertThat(dto.getDetails()).isEqualTo("detalle");
        });
    }

    @Test
    void obtenerPorIdConsultaRegistroYLanzaExcepcionSiNoExiste() {
        when(repository.findById(9L)).thenReturn(Optional.of(entry(9L)));
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThat(service.obtenerPorId(9L).getLogId()).isEqualTo(9L);
        assertThatThrownBy(() -> service.obtenerPorId(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorUsuarioConservaOrdenDelRepositorio() {
        when(repository.findByUsuarioUserIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(entry(9L), entry(4L)));

        assertThat(service.listarPorUsuario(7L)).extracting(dto -> dto.getLogId())
                .containsExactly(9L, 4L);
        verify(repository).findByUsuarioUserIdOrderByCreatedAtDesc(7L);
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
}
