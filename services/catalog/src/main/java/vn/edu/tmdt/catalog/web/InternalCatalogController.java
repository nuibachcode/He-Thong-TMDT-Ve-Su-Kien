package vn.edu.tmdt.catalog.web;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.catalog.service.CatalogService;

@RestController
@RequestMapping("/internal")
public class InternalCatalogController {

    private final CatalogService catalog;

    public InternalCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    public record ItemsRequest(String refType, List<Long> ids) {
    }

    @PostMapping("/items")
    public List<CatalogService.ItemMeta> items(@RequestBody ItemsRequest request) {
        return catalog.items(request.refType(), request.ids());
    }

    @GetMapping("/content/owned")
    public Map<String, Boolean> owned(@RequestParam String refType, @RequestParam Long refId,
                                      @RequestParam Long organizerId) {
        return Map.of("owned", catalog.ownedBy(refType, refId, organizerId));
    }

    @GetMapping("/organizer/pending-count")
    public Map<String, Long> pendingCount(@RequestParam Long organizerId) {
        return catalog.organizerPendingCounts(organizerId);
    }

    @GetMapping("/stats/counts")
    public Map<String, Long> counts() {
        return catalog.counts();
    }
}
