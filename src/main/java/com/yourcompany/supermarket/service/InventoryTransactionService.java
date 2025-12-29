package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
public class InventoryTransactionService {

    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;

    @Autowired
    private ProductMapper productMapper;

    public InventoryTransaction getById(Long id) {
        return inventoryTransactionMapper.selectById(id);
    }

    public List<InventoryTransaction> listAll() {
        return inventoryTransactionMapper.selectList(new QueryWrapper<>());
    }

    @Transactional
    public InventoryTransaction recordTransaction(InventoryTransaction transaction) {
        validateTransaction(transaction);
        Product product = productMapper.selectById(transaction.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("商品不存在: " + transaction.getProductId());
        }

        int qty = transaction.getQuantity();
        if ("OUT".equalsIgnoreCase(transaction.getType())) {
            if (product.getStock() == null || product.getStock() < qty) {
                throw new IllegalStateException("库存不足，无法出库");
            }
            product.setStock(product.getStock() - qty);
        } else {
            product.setStock(product.getStock() + qty);
        }
        productMapper.updateById(product);

        transaction.setTransactionTime(LocalDateTime.now());
        inventoryTransactionMapper.insert(transaction);
        return transaction;
    }

    public void deleteById(Long id) {
        inventoryTransactionMapper.deleteById(id);
    }

    private void validateTransaction(InventoryTransaction transaction) {
        if (transaction.getProductId() == null) {
            throw new IllegalArgumentException("缺少商品 ID");
        }
        if (transaction.getQuantity() == null || transaction.getQuantity() <= 0) {
            throw new IllegalArgumentException("数量必须大于 0");
        }
        if (!"IN".equalsIgnoreCase(transaction.getType()) && !"OUT".equalsIgnoreCase(transaction.getType())) {
            throw new IllegalArgumentException("交易类型必须为 IN 或 OUT");
        }
    }
}
