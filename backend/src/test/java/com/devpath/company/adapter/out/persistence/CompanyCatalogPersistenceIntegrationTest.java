package com.devpath.company.adapter.out.persistence;
import static org.assertj.core.api.Assertions.*;
import com.devpath.company.application.CompanyCatalogPort;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest @TestPropertySource(properties="devpath.runtime.worker-enabled=false") @EnabledIfEnvironmentVariable(named="DEVPATH_DB_URL",matches=".+") class CompanyCatalogPersistenceIntegrationTest {
 @Autowired CompanyCatalogPort catalog;
 @Test @Transactional(readOnly=true) void loadsVersionedSupportedCompanies(){assertThat(catalog.findSupported()).hasSize(6);assertThat(catalog.findSupportedById("toss")).get().satisfies(p->{assertThat(p.profileVersion()).isEqualTo("company-v1");assertThat(p.weightOverrides()).containsEntry("TESTING","INCREASE");});assertThat(catalog.findSupportedById("meta")).isEmpty();}
}
