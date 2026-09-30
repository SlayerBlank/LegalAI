package pe.edu.upc.legalai;

import org.junit.jupiter.api.Test;
import pe.edu.upc.legalai.config.ModelMapperConfig;
import pe.edu.upc.legalai.entities.Cliente;
import pe.edu.upc.legalai.entities.EstadoExpediente;
import pe.edu.upc.legalai.entities.Expediente;
import pe.edu.upc.legalai.entities.Usuario;
import pe.edu.upc.legalai.DTOs.request.ClienteRequestDTO;
import pe.edu.upc.legalai.DTOs.request.ExpedienteRequestDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.Optional;
import pe.edu.upc.legalai.repositories.ClienteRepository;
import pe.edu.upc.legalai.repositories.ExpedienteRepository;
import pe.edu.upc.legalai.servicesinterfaces.UsuarioService;
import pe.edu.upc.legalai.servicesinterfaces.AuditLogService;
import pe.edu.upc.legalai.servicesimplements.ClienteServiceImpl;
import pe.edu.upc.legalai.servicesimplements.ExpedienteServiceImpl;

class ModelMapperConfigTest {

    @Test
    void beanUsesDefaultConfigurationWithoutProxyMappings() {
        var mapper = new ModelMapperConfig().modelMapper();
        assertThat(mapper.getConfiguration().isImplicitMappingEnabled()).isTrue();
        assertThat(mapper.getTypeMaps()).isEmpty();
    }

    @Test
    void mappingDoesNotReplaceRelationshipsOrBusinessState() {
        var owner = new Usuario();
        owner.setUserId(1L);
        var client = new Cliente();
        client.setClientId(10L);
        var expediente = new Expediente();
        expediente.setCaseId(20L);
        expediente.setOwner(owner);
        expediente.setClient(client);
        expediente.setStatus(EstadoExpediente.OPEN);
        var request = new ExpedienteRequestDTO();
        request.setClientId(10L);
        request.setTitle("Nuevo titulo");
        // Omitted business fields must retain their existing values.
        var cases = mock(ExpedienteRepository.class);
        var clients = mock(ClienteRepository.class);
        var users = mock(UsuarioService.class);
        when(users.obtenerUsuarioAutenticado()).thenReturn(owner);
        when(cases.findByCaseIdAndOwnerUserId(20L, 1L)).thenReturn(Optional.of(expediente));
        when(clients.findByClientIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(client));
        when(cases.saveAndFlush(expediente)).thenReturn(expediente);

        new ExpedienteServiceImpl(cases, clients, users, mock(AuditLogService.class)).actualizar(20L, request);

        assertThat(expediente.getTitle()).isEqualTo("Nuevo titulo");
        assertThat(expediente.getClient()).isSameAs(client);
        assertThat(client.getClientId()).isEqualTo(10L);
        assertThat(expediente.getOwner()).isSameAs(owner);
        assertThat(expediente.getStatus()).isEqualTo(EstadoExpediente.OPEN);
    }

    @Test
    void optionalFieldsCanBeClearedWithoutChangingOwnership() {
        var owner = new Usuario();
        owner.setUserId(1L);
        var client = new Cliente();
        client.setClientId(10L);
        client.setOwner(owner);
        client.setPhone("123456");
        var request = new ClienteRequestDTO();
        request.setFullNameOrCompany("Cliente actualizado");

        var clients = mock(ClienteRepository.class);
        var users = mock(UsuarioService.class);
        when(users.obtenerUsuarioAutenticado()).thenReturn(owner);
        when(clients.findByClientIdAndOwnerUserId(10L, 1L)).thenReturn(Optional.of(client));
        when(clients.saveAndFlush(client)).thenReturn(client);
        new ClienteServiceImpl(clients, users, mock(AuditLogService.class)).actualizar(10L, request);

        assertThat(client.getFullNameOrCompany()).isEqualTo("Cliente actualizado");
        assertThat(client.getPhone()).isNull();
        assertThat(client.getOwner()).isSameAs(owner);
    }
}
