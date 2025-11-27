package com.nemi.report.model.ads_manager;

import lombok.Data;

import java.util.List;

@Data
public class AdsCostOfUsersResponse {
    private String currency;
    private List<AdsCostOfUser> adsCostOfUsers;
}
