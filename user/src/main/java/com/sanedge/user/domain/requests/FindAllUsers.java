package com.sanedge.user.domain.requests;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
public class FindAllUsers {
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
