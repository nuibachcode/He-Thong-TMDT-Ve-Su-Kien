package vn.edu.tmdt.ai;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    @Query("select coalesce(avg(m.latencyMs), 0) from ChatMessage m where m.role = 'ASSISTANT'")
    double averageLatency();

    long countByFallbackTrue();

    long countByRefusedTrue();

    long countByRole(String role);
}
