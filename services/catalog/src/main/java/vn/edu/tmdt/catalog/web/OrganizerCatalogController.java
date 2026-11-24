package vn.edu.tmdt.catalog.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.catalog.common.ApiResponse;
import vn.edu.tmdt.catalog.service.CatalogService;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerCatalogController {

    private final CatalogService catalog;

    public OrganizerCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/events")
    public ApiResponse<List<CatalogService.OrgEvent>> events(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(catalog.organizerEvents(userId));
    }

    @PostMapping("/events")
    public ApiResponse<CatalogService.OrgEvent> createEvent(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CatalogService.EventInput input) {
        return ApiResponse.ok("Da gui duyet su kien", catalog.createEvent(userId, input));
    }

    @GetMapping("/tours")
    public ApiResponse<List<CatalogService.TourCard>> tours(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(catalog.listTours(null, null));
    }

    @PostMapping("/tours")
    public ApiResponse<CatalogService.CreatedTour> createTour(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody CatalogService.TourInput input) {
        return ApiResponse.ok("Da gui duyet tour", catalog.createTour(userId, input));
    }
}
