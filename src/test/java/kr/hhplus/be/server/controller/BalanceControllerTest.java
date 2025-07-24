package kr.hhplus.be.server.controller;

import kr.hhplus.be.server.domain.User;
import kr.hhplus.be.server.dto.BalanceChargeRequest;
import kr.hhplus.be.server.dto.BalanceUseRequest;
import kr.hhplus.be.server.service.BalanceChargeService;
import kr.hhplus.be.server.service.BalanceQueryService;
import kr.hhplus.be.server.service.BalanceUseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
public class BalanceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BalanceChargeService chargeService;

    @MockBean
    private BalanceUseService useService;

    @MockBean
    private BalanceQueryService queryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 잔액_정상_조회_API() throws Exception {
        // Given
        Long userId = 1L;
        when(queryService.getBalance(userId)).thenReturn(new User(userId, 3000L));

        // When & Then
        mockMvc.perform(get("/balance/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.amount").value(3000));
    }

    @Test
    void 잔액_충전_API() throws Exception {
        // Given
        Long userId = 1L;
        BalanceChargeRequest request = new BalanceChargeRequest();
        request.setAmount(5000L);

        // When & Then
        mockMvc.perform(patch("/balance/{userId}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(chargeService).charge(userId, 5000L);
    }

    @Test
    void 잔액_사용_API() throws Exception {
        // Given
        Long userId = 1L;
        BalanceUseRequest request = new BalanceUseRequest();
        request.setAmount(2000L);

        // When & Then
        mockMvc.perform(patch("/balance/{userId}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(useService).use(userId, 2000L);
    }
}
