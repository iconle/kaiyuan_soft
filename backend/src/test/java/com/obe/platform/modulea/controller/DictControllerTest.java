package com.obe.platform.modulea.controller;

import com.obe.platform.modulea.service.DictService;
import com.obe.platform.modulea.service.AdminClassImportService;
import com.obe.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DictController.class)
@AutoConfigureMockMvc(addFilters = false)
class DictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DictService dictService;

    @MockBean
    private AdminClassImportService adminClassImportService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void deleteCollegeRemovesCollegeAndReturnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/dict/colleges/{id}", 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dictService).deleteCollege(12L);
    }
}
