package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.domain.Product;
import kr.hhplus.be.server.service.ProductDetailQueryService;
import kr.hhplus.be.server.service.ProductQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductDetailQueryService detailService;

    @MockBean
    private ProductQueryService queryService;

    @Test
    void 상품_상세정보_조회() throws Exception {
        Long id = 1L;
        Product product = new Product(id, "노트북", 1200000, 5);
        when(detailService.getProductById(id)).thenReturn(product);

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("노트북"));
    }
}
