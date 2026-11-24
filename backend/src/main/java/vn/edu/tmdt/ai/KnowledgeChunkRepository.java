package vn.edu.tmdt.ai;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findBySourceType(String sourceType);

    void deleteBySourceTypeAndSourceId(String sourceType, Long sourceId);

    void deleteBySourceType(String sourceType);
}
