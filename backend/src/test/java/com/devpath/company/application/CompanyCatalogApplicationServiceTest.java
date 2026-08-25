package com.devpath.company.application;
import static org.assertj.core.api.Assertions.*;
import com.devpath.company.domain.CompanyProfile;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
class CompanyCatalogApplicationServiceTest {
 @Test void returnsSupportedVersionedCompanyPolicyWithoutReadiness(){var p=new CompanyProfile("toss","Toss","토스",UUID.randomUUID(),"company-v1","빠른 반복과 정확성",List.of("테스트"),List.of("신뢰성"),List.of("테스트"),List.of("정확성"),Map.of("TESTING","INCREASE"),Instant.parse("2026-08-12T00:00:00Z"));var s=new CompanyCatalogApplicationService(new CompanyCatalogPort(){public List<CompanyProfile> findSupported(){return List.of(p);}public Optional<CompanyProfile> findSupportedById(String id){return "toss".equals(id)?Optional.of(p):Optional.empty();}});assertThat(s.list().companies()).singleElement().extracting(CompanySummaryView::companyId).isEqualTo("toss");assertThat(s.get("toss").weightOverrides()).containsEntry("TESTING","INCREASE");assertThatThrownBy(()->s.get("meta")).isInstanceOf(CompanyNotFoundException.class);}
}
