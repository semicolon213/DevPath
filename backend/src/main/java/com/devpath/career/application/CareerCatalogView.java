package com.devpath.career.application;

import java.util.List;

public record CareerCatalogView(String catalogVersion, List<CareerSummaryView> careers) {
    public CareerCatalogView { careers = List.copyOf(careers); }
}
