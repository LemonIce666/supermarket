package com.yourcompany.supermarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yourcompany.supermarket.entity.Product;
import com.yourcompany.supermarket.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // 获取所有商品
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();  // 获取所有商品
    }

    // 添加商品
    @PostMapping
    public void addProduct(@RequestBody Product product) {
        productService.addProduct(product);  // 添加商品
    }

    // 分页查询商品
    @GetMapping("/page")
    public Page<Product> getProductsByPage(
            @RequestParam("pageNo") int pageNo,
            @RequestParam("pageSize") int pageSize) {
        return productService.getProductsByPage(pageNo, pageSize);  // 分页查询
    }
}
