package com.devpath.career.application;

import com.devpath.career.domain.CareerProfile;
import java.util.List;
import java.util.Optional;

public interface CareerCatalogPort {
    List<CareerProfile> findSupported();
    Optional<CareerProfile> findSupportedById(String careerId);
}
