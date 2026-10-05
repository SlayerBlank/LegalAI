package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.dtos.response.ClienteCantidadDocumentosResponseDTO;
import pe.edu.upc.legalai.entities.Cliente;

import java.util.List;
import java.util.Optional;

@Repository
public interface IClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByOwnerUserId(Long userId);

    Optional<Cliente> findByClientIdAndOwnerUserId(Long clientId, Long userId);

    // HU-42: nombre del cliente y cantidad de documentos asociados
    @Query("""
            SELECT new pe.edu.upc.legalai.dtos.response.ClienteCantidadDocumentosResponseDTO(
                       c.clientId, c.fullNameOrCompany, COUNT(d))
            FROM Cliente c
            LEFT JOIN Expediente e ON e.client = c
            LEFT JOIN Documento d ON d.expediente = e
            WHERE c.owner.userId = :userId
            GROUP BY c.clientId, c.fullNameOrCompany
            ORDER BY COUNT(d) DESC
            """)
    List<ClienteCantidadDocumentosResponseDTO> contarDocumentosPorCliente(@Param("userId") Long userId);
}