package com.bonsaimarket.backend.marketplace;

import org.springframework.validation.FieldError;

/** Localized constraint feedback without rejected values or implementation details. */
public final class ValidationPresentation {
    private ValidationPresentation() {}
    public static String message(FieldError error) {
        // Explicit Vietnamese constraint messages are business copy owned by the DTO.
        String message=error.getDefaultMessage();
        if(message!=null && message.matches("(?s).*[À-ỹĐđ].*") && !message.contains("{") && !message.contains("<"))return message;
        return switch(error.getCode()==null?"":error.getCode()) {
            case "NotNull", "NotBlank", "NotEmpty" -> "Vui lòng bổ sung thông tin này.";
            case "Email" -> "Email chưa đúng định dạng.";
            case "Positive", "Min", "DecimalMin" -> "Giá trị phải lớn hơn hoặc bằng mức tối thiểu cho phép.";
            case "Max", "DecimalMax" -> "Giá trị vượt mức tối đa cho phép.";
            case "Size" -> "Độ dài hoặc số lượng chưa phù hợp. Vui lòng kiểm tra lại.";
            case "Pattern" -> "Thông tin chưa đúng định dạng yêu cầu.";
            default -> "Vui lòng kiểm tra lại thông tin này.";
        };
    }
}
