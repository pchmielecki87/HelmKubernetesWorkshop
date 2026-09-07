package pl.pchmielecki.shop;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartController {
    private final StringRedisTemplate redisTemplate;

    public CartController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/{cartId}")
    public Map<String, Integer> getCart(@PathVariable String cartId) {
        HashOperations<String, String, String> cartOperations = redisTemplate.opsForHash();
        Map<String, String> values = cartOperations.entries(cartKey(cartId));
        Map<String, Integer> cart = new TreeMap<>();
        values.forEach((productId, quantity) -> cart.put(productId, Integer.parseInt(quantity)));
        return cart;
    }

    @PostMapping("/{cartId}/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addItem(@PathVariable String cartId, @RequestBody CartItemRequest request) {
        String key = cartKey(cartId);
        String field = String.valueOf(request.productId());
        HashOperations<String, String, String> cartOperations = redisTemplate.opsForHash();
        String storedQuantity = cartOperations.get(key, field);
        int current = storedQuantity == null ? 0 : Integer.parseInt(storedQuantity);
        cartOperations.put(key, field, String.valueOf(current + request.quantity()));
    }

    @DeleteMapping("/{cartId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@PathVariable String cartId) {
        redisTemplate.delete(cartKey(cartId));
    }

    private String cartKey(String cartId) {
        return "cart:" + cartId;
    }

    public record CartItemRequest(Long productId, int quantity) {
    }
}
