package vn.edu.tmdt.ai.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import vn.edu.tmdt.ai.config.RabbitConfig;
import vn.edu.tmdt.ai.ingest.IngestService;

/**
 * Khi Catalog/Organizer phat su kien catalog.content.changed, AI service reindex lai.
 * Hien tai core-api chua publish (se tach sang Catalog service); endpoint /api/ai/reindex
 * la duong du phong de demo.
 */
@Component
public class CatalogEventListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventListener.class);

    private final IngestService ingest;

    public CatalogEventListener(IngestService ingest) {
        this.ingest = ingest;
    }

    @RabbitListener(queues = RabbitConfig.AI_QUEUE)
    public void onCatalogChanged(String message) {
        log.info("Nhan su kien catalog thay doi: {}", message);
        ingest.reindexAll()
                .subscribe(
                        count -> log.info("Reindex tu RabbitMQ xong: {} chunk", count),
                        error -> log.warn("Reindex tu RabbitMQ loi: {}", error.getMessage()));
    }
}
