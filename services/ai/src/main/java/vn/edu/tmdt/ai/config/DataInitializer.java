package vn.edu.tmdt.ai.config;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.edu.tmdt.ai.ingest.IngestService;
import vn.edu.tmdt.ai.ingest.SeedLoader;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SeedLoader seedLoader;
    private final IngestService ingest;
    private final AiProperties properties;

    public DataInitializer(SeedLoader seedLoader, IngestService ingest, AiProperties properties) {
        this.seedLoader = seedLoader;
        this.ingest = ingest;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedLoader.load()
                    .flatMap(seed -> ingest.countForCurrentProvider()
                            .flatMap(count -> {
                                if (count == 0 || properties.reindexOnStart()) {
                                    log.info("Chua co index cho provider hien tai hoac bat reindex-on-start, bat dau ingest...");
                                    return ingest.reindexAll();
                                }
                                return ingest.syncCards().thenReturn(count.intValue());
                            }))
                    .doOnSuccess(count -> log.info("AI service san sang: {} chunk, provider={}",
                            count, ingest.provider()))
                    .block(Duration.ofMinutes(5));
        } catch (Exception ex) {
            log.error("Khoi tao du lieu AI that bai, service van chay o muc suy giam: {}", ex.getMessage(), ex);
        }
    }
}
