package pl.pchmielecki.shop;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final JdbcTemplate jdbcTemplate;

    public ProductController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public List<Product> listProducts() {
        return jdbcTemplate.query(
                "SELECT id, name, price FROM products ORDER BY id",
                (resultSet, rowNumber) -> new Product(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getBigDecimal("price")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product addProduct(@RequestBody ProductRequest request) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO products (name, price) VALUES (?, ?) RETURNING id",
                Long.class,
                request.name(),
                request.price());
        return new Product(id, request.name(), request.price());
    }

    public record Product(Long id, String name, BigDecimal price) {
    }

    public record ProductRequest(String name, BigDecimal price) {
    }
}
