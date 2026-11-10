package br.com.guardian.municipality.domain.repository;

import br.com.guardian.municipality.domain.model.Municipality;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MunicipalityRepository extends JpaRepository<Municipality, UUID> {

    Optional<Municipality> findByIbgeCode(String ibgeCode);

    boolean existsByIbgeCode(String ibgeCode);

    Page<Municipality> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Municipality> findByStateIgnoreCase(String state, Pageable pageable);

    Page<Municipality> findByNameContainingIgnoreCaseAndStateIgnoreCase(String name, String state, Pageable pageable);

    @Query("SELECT m FROM Municipality m WHERE m.boundary IS NOT NULL AND contains(m.boundary, :point) = true")
    List<Municipality> findByContainingPoint(@Param("point") Point point);
}
