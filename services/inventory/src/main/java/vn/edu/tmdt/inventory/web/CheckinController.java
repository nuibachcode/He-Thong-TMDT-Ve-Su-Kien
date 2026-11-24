package vn.edu.tmdt.inventory.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.inventory.common.ApiResponse;
import vn.edu.tmdt.inventory.service.InventoryService;

@RestController
@RequestMapping("/api/organizer")
public class CheckinController {

    private final InventoryService inventory;

    public CheckinController(InventoryService inventory) {
        this.inventory = inventory;
    }

    public record CheckinRequest(String bookingCode, Long ticketId) {
    }

    @PostMapping("/checkin")
    public ApiResponse<String> checkin(@RequestBody CheckinRequest request,
                                       @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(inventory.checkin(request.bookingCode(), request.ticketId(), userId));
    }
}
