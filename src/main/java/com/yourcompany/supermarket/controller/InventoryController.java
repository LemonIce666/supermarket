package com.yourcompany.supermarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yourcompany.supermarket.entity.InventoryTransaction;
import com.yourcompany.supermarket.entity.Product;
import com.yourcompany.supermarket.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/page")
    public Page<Product> getInventoryPage(@RequestParam("pageNo") int pageNo,
                                          @RequestParam("pageSize") int pageSize) {
        return inventoryService.getInventoryPage(pageNo, pageSize);
    }

    @PostMapping("/inbound")
    public Map<String, Object> inbound(@RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        int quantity = Integer.parseInt(request.get("quantity").toString());
        inventoryService.inbound(productId, quantity);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Inbound success");
        return response;
    }

    @PostMapping("/outbound")
    public Map<String, Object> outbound(@RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        int quantity = Integer.parseInt(request.get("quantity").toString());
        inventoryService.outbound(productId, quantity);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Outbound success");
        return response;
    }

    @GetMapping("/low-stock")
    public List<Product> getLowStockProducts() {
        return inventoryService.getLowStockProducts();
    }

    @GetMapping("/transactions")
    public Page<InventoryTransaction> getTransactions(@RequestParam("pageNo") int pageNo,
                                                      @RequestParam("pageSize") int pageSize) {
        return inventoryService.getTransactions(pageNo, pageSize);
    }
}
