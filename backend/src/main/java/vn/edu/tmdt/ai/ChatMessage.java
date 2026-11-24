package vn.edu.tmdt.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public Long sessionId;
    public String role;
    @Column(columnDefinition = "LONGTEXT")
    public String content;
    @Column(columnDefinition = "LONGTEXT")
    public String citations;
    public String provider;
    public Integer latencyMs;
    public boolean fallback;
    public boolean refused;
    public Instant createdAt = Instant.now();
}
