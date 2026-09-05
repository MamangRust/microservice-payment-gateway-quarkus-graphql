package com.sanedge.role.domain.requests;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
public class FindAllRoles {
    @QueryParam("page")
    @DefaultValue("1")
    private Integer page = 1;

    @QueryParam("pageSize")
    @DefaultValue("10")
    private Integer pageSize = 10;

    @QueryParam("search")
    @DefaultValue("")
    private String search = "";
}
