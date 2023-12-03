package io.k8s.framework.extractor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Metadata {
    private String city;
    private String zipCode;
    private String lastUpdate;
}
