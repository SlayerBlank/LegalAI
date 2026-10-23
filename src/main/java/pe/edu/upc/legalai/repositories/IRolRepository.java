package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Rol;

import java.util.Optional;

@Repository
public interface IRolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndRoleIdNot(String name, Long roleId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rol r where r.roleId = :id")
    Optional<Rol> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    @Modifying
    @Query(value = "INSERT INTO roles (name, description) VALUES ('USER', 'Usuario regular') ON CONFLICT (name) DO NOTHING",
            nativeQuery = true)
    void crearRolUsuarioSiNoExiste();
}
