package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yourcompany.supermarket.entity.InventoryTransaction;
import com.yourcompany.supermarket.entity.Product;
import com.yourcompany.supermarket.mapper.InventoryTransactionMapper;
import com.yourcompany.supermarket.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private InventoryTransactionMapper transactionMapper;

    public Page<Product> getInventoryPage(int pageNo, int pageSize) {
        Page<Product> page = new Page<>(pageNo, pageSize);
        return productMapper.selectPage(page, null);
    }

    @Transactional
    public void inbound(Long productId, int quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        product.setStock((product.getStock() == null ? 0 : product.getStock()) + quantity);
        productMapper.updateById(product);

        InventoryTransaction transaction = buildTransaction(productId, quantity, "IN");
        transactionMapper.insert(transaction);
    }

    @Transactional
    public void outbound(Long productId, int quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        int currentStock = product.getStock() == null ? 0 : product.getStock();
        if (currentStock < quantity) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        product.setStock(currentStock - quantity);
        productMapper.updateById(product);

        InventoryTransaction transaction = buildTransaction(productId, -quantity, "OUT");
        transactionMapper.insert(transaction);
    }

    public List<Product> getLowStockProducts() {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("stock <= reorder_level");
        return productMapper.selectList(queryWrapper);
    }

    public Page<InventoryTransaction> getTransactions(int pageNo, int pageSize) {
        Page<InventoryTransaction> page = new Page<>(pageNo, pageSize);
        return transactionMapper.selectPage(page, null);
    }

    private InventoryTransaction buildTransaction(Long productId, int quantity, String type) {
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductId(productId);
        transaction.setQuantity(quantity);
        transaction.setType(type);
        transaction.setTransactionTime(LocalDateTime.now());
        return transaction;
    }
}
