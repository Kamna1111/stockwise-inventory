package inventory_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;

import inventory_api.repository.ProductRepository;
import inventory_api.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryApiApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired StockMovementRepository movements;

    @BeforeEach
    void cleanDatabase() {
        movements.deleteAll();
        products.deleteAll();
    }

    @Test
    void createsReadsUpdatesAndDeletesProduct() throws Exception {
        String body = """
                {"name":"Laptop","sku":"LAP-001","quantity":25,"price":799.99}
                """;
        String location = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(799.99))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(get(location)).andExpect(status().isOk()).andExpect(jsonPath("$.sku").value("LAP-001"));
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Laptop Pro\",\"sku\":\"LAP-001\",\"quantity\":10,\"price\":899.50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.price").value(899.50));
        mvc.perform(delete(location)).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    void validatesProductsAndRejectsDuplicateSku() throws Exception {
        String body = "{\"name\":\"Keyboard\",\"sku\":\"KEY-001\",\"quantity\":5,\"price\":20.00}";
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"sku\":\"X\",\"quantity\":-1,\"price\":-2}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields").exists());
    }

    @Test
    void recordsStockUpdatesAndPreventsNegativeInventory() throws Exception {
        String location = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mouse\",\"sku\":\"MOU-001\",\"quantity\":5,\"price\":10.00}"))
                .andReturn().getResponse().getHeader("Location");
        mvc.perform(post(location + "/stock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityChange\":3,\"reason\":\"Restock\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(8));
        mvc.perform(get(location + "/stock-history")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].change").value(3)).andExpect(jsonPath("$[0].reason").value("Restock"));
        mvc.perform(post(location + "/stock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityChange\":-20,\"reason\":\"Bad adjustment\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(location)).andExpect(status().isConflict());
    }

    @Test
    void assistantExplainsWhenItIsNotConfigured() throws Exception {
        mvc.perform(post("/api/assistant/inventory").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How many laptops are in stock?\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void servesTheBrowserDashboardAtTheHomePage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Stockwise")));
    }

    @Test
    void prioritizesLowestStockProductsInTheRestockQueue() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Nearly out\",\"sku\":\"LOW-3\",\"quantity\":3,\"price\":4.00,\"reorderPoint\":5}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Almost gone\",\"sku\":\"LOW-1\",\"quantity\":1,\"price\":8.00,\"reorderPoint\":2}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Healthy stock\",\"sku\":\"OK-100\",\"quantity\":100,\"price\":2.00,\"reorderPoint\":10}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/products/restock"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].sku").value("LOW-1"))
                .andExpect(jsonPath("$[1].sku").value("LOW-3"));
    }
}
