package com.bonsaimarket.backend.community;

import com.bonsaimarket.backend.marketplace.MarketplaceErrors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class MarketplaceErrorPrivacyTest {
    @RestController static class InvalidRequests {
        record Input(@NotBlank String name,@Min(1) int quantity) {}
        @PostMapping("/validation") Object validate(@Valid @RequestBody Input input) {return input;}
        @GetMapping("/unexpected") Object fail() {throw new IllegalStateException("SQL syntax error SELECT secret FROM users at C:/server/database.sql");}
    }
    @Test void unexpectedExceptionsNeverBecomeClientDiagnostics() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new InvalidRequests()).setControllerAdvice(new MarketplaceErrors()).build();
        mvc.perform(get("/unexpected")).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.message").value("Không thể xử lý yêu cầu lúc này."))
            .andExpect(content().string(not(containsString("secret"))))
            .andExpect(content().string(not(containsString("database.sql"))))
            .andExpect(jsonPath("$.trace").doesNotExist());
    }
    @Test void constraintFeedbackIsLocalizedWithoutRejectedValues() throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new InvalidRequests()).setControllerAdvice(new MarketplaceErrors()).build();
        mvc.perform(post("/validation").contentType("application/json").content("{\"name\":\"\",\"quantity\":0}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fields.name").value("Vui lòng bổ sung thông tin này."))
            .andExpect(jsonPath("$.fields.quantity").value("Giá trị phải lớn hơn hoặc bằng mức tối thiểu cho phép."))
            .andExpect(content().string(not(containsString("must"))));
    }
}
