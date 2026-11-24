package vn.edu.tmdt.inventory.web;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.tmdt.inventory.service.InventoryService;

@RestController
@RequestMapping("/internal")
public class InternalInventoryController {

    private final InventoryService inventory;

    public InternalInventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @PostMapping("/stock/ensure")
    public List<InventoryService.StockView> ensure(@RequestBody List<InventoryService.StockDef> defs) {
        return inventory.ensure(defs);
    }

    @PostMapping("/stock/query")
    public List<InventoryService.StockView> query(@RequestBody InventoryService.StockQuery request) {
        return inventory.query(request.refType(), request.ids());
    }

    @GetMapping("/stock/organizer/{id}")
    public InventoryService.OrganizerStat organizerStat(@PathVariable Long id) {
        return inventory.organizerStat(id);
    }

    @PostMapping("/holds")
    public InventoryService.HoldResult hold(@RequestBody InventoryService.HoldRequest request) {
        return inventory.hold(request);
    }

    @PostMapping("/holds/{code}/confirm")
    public Map<String, String> confirm(@PathVariable String code) {
        return Map.of("status", inventory.confirm(code));
    }

    @PostMapping("/holds/{code}/release")
    public Map<String, String> release(@PathVariable String code) {
        inventory.release(code);
        return Map.of("status", "OK");
    }

    @GetMapping("/tickets/{code}")
    public List<InventoryService.TicketView> tickets(@PathVariable String code) {
        return inventory.tickets(code);
    }
}
