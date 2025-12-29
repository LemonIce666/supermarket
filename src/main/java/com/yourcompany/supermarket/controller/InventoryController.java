package com.yourcompany.supermarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yourcompany.supermarket.entity.InventoryTransaction;
import com.yourcompany.supermarket.entity.Product;
import com.yourcompany.supermarket.security.RequiredRole;
import com.yourcompany.supermarket.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
    @RequiredRole({"BOSS", "STAFF"})
    public void inbound(@RequestBody InventoryAdjustmentRequest request) {
        try {
            validateRequest(request);
            inventoryService.inbound(request.getProductId(), request.getQuantity());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/outbound")
    @RequiredRole({"BOSS", "STAFF"})
    public void outbound(@RequestBody InventoryAdjustmentRequest request) {
        try {
            validateRequest(request);
            inventoryService.outbound(request.getProductId(), request.getQuantity());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/low-stock")
    public List<Product> getLowStockProducts() {
        return inventoryService.getLowStockProducts();
    }

    @GetMapping("/transactions")
    @RequiredRole({"BOSS", "STAFF"})
    public Page<InventoryTransaction> getTransactions(@RequestParam("pageNo") int pageNo,
                                                      @RequestParam("pageSize") int pageSize) {
        return inventoryService.getTransactions(pageNo, pageSize);
    }

    private void validateRequest(InventoryAdjustmentRequest request) {
        if (request == null || request.getProductId() == null || request.getQuantity() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and quantity are required");
        }
    }
}
