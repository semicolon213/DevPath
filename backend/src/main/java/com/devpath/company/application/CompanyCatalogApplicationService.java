package com.devpath.company.application;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class CompanyCatalogApplicationService {
    private final CompanyCatalogPort catalog;
    public CompanyCatalogApplicationService(CompanyCatalogPort catalog){this.catalog=catalog;}
    @Transactional(readOnly=true) public CompanyCatalogView list(){var values=catalog.findSupported().stream().sorted(Comparator.comparing(v->v.localizedName())).map(v->new CompanySummaryView(v.companyId(),v.name(),v.localizedName(),"SUPPORTED",v.profileVersion(),v.engineeringCulture())).toList(); String version=values.stream().map(CompanySummaryView::profileVersion).distinct().sorted().reduce((a,b)->a+","+b).orElse("unavailable"); return new CompanyCatalogView(version,values);}
    @Transactional(readOnly=true) public CompanyProfileView get(String id){return catalog.findSupportedById(id).map(CompanyProfileView::from).orElseThrow(CompanyNotFoundException::new);}
    @Transactional(readOnly=true) public boolean supports(String id){return catalog.findSupportedById(id).isPresent();}
}
