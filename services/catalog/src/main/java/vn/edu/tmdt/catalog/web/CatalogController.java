package vn.edu.tmdt.catalog.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.catalog.common.ApiResponse;
import vn.edu.tmdt.catalog.service.CatalogService;

@RestController
@RequestMapping("/api/public")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/events")
    public ApiResponse<List<CatalogService.EventCard>> events(@RequestParam(required = false) String q,
                                                              @RequestParam(required = false) String category) {
        return ApiResponse.ok(catalog.listEvents(q, category));
    }

    @GetMapping("/events/{slug}")
    public ApiResponse<CatalogService.EventDetail> event(@PathVariable String slug) {
        return ApiResponse.ok(catalog.eventDetail(slug));
    }

    @GetMapping("/tours")
    public ApiResponse<List<CatalogService.TourCard>> tours(@RequestParam(required = false) String q,
                                                           @RequestParam(required = false) String destination) {
        return ApiResponse.ok(catalog.listTours(q, destination));
    }

    @GetMapping("/tours/{slug}")
    public ApiResponse<CatalogService.TourDetail> tour(@PathVariable String slug) {
        return ApiResponse.ok(catalog.tourDetail(slug));
    }
}
