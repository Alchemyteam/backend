package com.ecosystem.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificationResponse {
    private Boolean peCertified;
    private String certificateNumber;
    private String certifiedBy;
    private String certifiedDate;
}

