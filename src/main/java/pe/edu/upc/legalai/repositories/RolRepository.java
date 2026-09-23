package pe.edu.upc.legalai.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.upc.legalai.entities.Rol;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByName(String name);

    @Modifying
    @Query(value = "INSERT INTO roles (name, description) VALUES ('USER', 'Usuario regular') ON CONFLICT (name) DO NOTHING",
            nativeQuery = true)
    void crearRolUsuarioSiNoExiste();
}
