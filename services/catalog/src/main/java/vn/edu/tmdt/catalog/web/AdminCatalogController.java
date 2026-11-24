package vn.edu.tmdt.catalog.web;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.catalog.common.ApiResponse;
import vn.edu.tmdt.catalog.service.CatalogService;

@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {

    private final CatalogService catalog;

    public AdminCatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/pending")
    public ApiResponse<List<CatalogService.PendingView>> pending() {
        return ApiResponse.ok(catalog.pending());
    }

    public record StatusRequest(String status) {
    }

    @PatchMapping("/content/{type}/{id}/status")
    public ApiResponse<String> setStatus(@PathVariable String type, @PathVariable Long id,
                                         @RequestBody StatusRequest request) {
        return ApiResponse.ok("Da cap nhat trang thai",
                catalog.setContentStatus(type, id, request.status()));
    }
}
