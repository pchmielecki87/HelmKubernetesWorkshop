CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0)
);

INSERT INTO products (name, price)
SELECT 'Kubernetes T-shirt', 29.99
WHERE NOT EXISTS (SELECT 1 FROM products);
