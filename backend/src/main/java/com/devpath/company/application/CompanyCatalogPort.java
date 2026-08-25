package com.devpath.company.application;
import com.devpath.company.domain.CompanyProfile;
import java.util.List;
import java.util.Optional;
public interface CompanyCatalogPort { List<CompanyProfile> findSupported(); Optional<CompanyProfile> findSupportedById(String id); }
