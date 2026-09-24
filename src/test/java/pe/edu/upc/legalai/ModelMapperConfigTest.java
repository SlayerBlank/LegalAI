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

class ModelMapperConfigTest {

    @Test
    void mappingDoesNotReplaceRelationshipsOrBusinessState() {
        var mapper = new ModelMapperConfig().modelMapper();
        var owner = new Usuario();
        var client = new Cliente();
        client.setClientId(10L);
        var expediente = new Expediente();
        expediente.setOwner(owner);
        expediente.setClient(client);
        expediente.setStatus(EstadoExpediente.OPEN);
        var request = new ExpedienteRequestDTO();
        request.setClientId(99L);
        request.setTitle("Nuevo titulo");
        request.setStatus(EstadoExpediente.CLOSED);

        mapper.map(request, expediente);

        assertThat(expediente.getTitle()).isEqualTo("Nuevo titulo");
        assertThat(expediente.getClient()).isSameAs(client);
        assertThat(client.getClientId()).isEqualTo(10L);
        assertThat(expediente.getOwner()).isSameAs(owner);
        assertThat(expediente.getStatus()).isEqualTo(EstadoExpediente.OPEN);
    }

    @Test
    void optionalFieldsCanBeClearedWithoutChangingOwnership() {
        var mapper = new ModelMapperConfig().modelMapper();
        var owner = new Usuario();
        var client = new Cliente();
        client.setOwner(owner);
        client.setPhone("123456");
        var request = new ClienteRequestDTO();
        request.setFullNameOrCompany("Cliente actualizado");

        mapper.map(request, client);

        assertThat(client.getFullNameOrCompany()).isEqualTo("Cliente actualizado");
        assertThat(client.getPhone()).isNull();
        assertThat(client.getOwner()).isSameAs(owner);
    }
}
