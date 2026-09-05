package com.sanedge.role.domain.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {
    @Min(value = 1, message = "ID role minimal 1")
    private Integer roleId;

    @NotBlank(message = "Nama role wajib diisi")
    private String name;
}
