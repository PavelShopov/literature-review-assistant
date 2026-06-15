package mk.ukim.finki.literaturereviewassistant.repository;

import mk.ukim.finki.literaturereviewassistant.model.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByToken(String token);

    void deleteByToken(String token);
}
