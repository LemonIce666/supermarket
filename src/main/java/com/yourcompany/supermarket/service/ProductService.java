package com.yourcompany.supermarket.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yourcompany.supermarket.entity.Product;
import com.yourcompany.supermarket.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    // 获取所有商品
    public List<Product> getAllProducts() {
        return productMapper.selectList(null);  // 获取所有商品
    }

    // 添加商品
    public void addProduct(Product product) {
        productMapper.insert(product);  // 添加商品
    }

    // 分页查询商品
    public Page<Product> getProductsByPage(int pageNo, int pageSize) {
        Page<Product> page = new Page<>(pageNo, pageSize);
        return productMapper.selectPage(page, null);  // 分页查询
    }
}
