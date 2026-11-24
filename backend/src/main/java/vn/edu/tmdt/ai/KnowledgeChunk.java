package vn.edu.tmdt.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "knowledge_chunks")
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String sourceType;
    public Long sourceId;
    public String title;
    public Integer chunkIndex;
    @Column(columnDefinition = "TEXT")
    public String content;
    public String contentHash;
    @Column(columnDefinition = "TEXT")
    public String embedding;
    public String embeddingModel;
    public Integer embeddingDim;
    public Instant updatedAt = Instant.now();
}
