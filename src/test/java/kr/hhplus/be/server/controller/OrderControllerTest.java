package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.domain.Order;
import kr.hhplus.be.server.service.PlaceOrderUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.github.dockerjava.core.MediaType;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
public class OrderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlaceOrderUseCase placeOrderUseCase;

    @Test
    void 주문_API_정상처리() throws Exception {
        Order order = new Order(1L, 100L, 2, 10000);
        when(placeOrderUseCase.placeOrder(anyLong(), anyLong(), anyInt())).thenReturn(order);

        mockMvc.perform(post("/orders")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content("""
                            {
                              "userId": 1,
                              "productId": 100,
                              "quantity": 2
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(10000));
    }
}
