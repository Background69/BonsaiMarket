package com.bonsaimarket.backend.inventory;

import com.bonsaimarket.backend.auth.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

@RestController @RequestMapping("/api/seller/inventory")
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) {this.service=service;}
    public record Adjust(@NotNull(message="Nhập số lượng tồn kho.") @Min(value=0,message="Tồn kho không được âm.") @Max(value=1000000,message="Tồn kho tối đa 1.000.000.") @Digits(integer=7,fraction=0,message="Tồn kho phải là số nguyên.") java.math.BigDecimal stock,
                         @NotNull(message="Cần tồn kho đã đọc gần nhất.") @Min(value=0,message="Tồn kho trước đó không hợp lệ.") @Max(value=2147483647,message="Tồn kho trước đó không hợp lệ.") @Digits(integer=10,fraction=0,message="Tồn kho trước đó phải là số nguyên.") java.math.BigDecimal expectedStock) { }
    @GetMapping public ResponseEntity<InventoryService.StockPage> list(@AuthenticationPrincipal AuthPrincipal user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(user.id(),page,size));
    }
    @PutMapping("/{productId}") public InventoryService.StockView adjust(@AuthenticationPrincipal AuthPrincipal user,@PathVariable Long productId,@Valid @RequestBody Adjust body) {
        return service.adjust(user.id(),productId,body.stock().intValueExact(),body.expectedStock().intValueExact());
    }
}
