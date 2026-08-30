import { apiRequest } from "../../../shared/api/apiClient";
export type CompanySummary={companyId:string;name:string;localizedName:string;status:"SUPPORTED";profileVersion:string;engineeringCulture:string};
export type CompanyCatalog={catalogVersion:string;companies:CompanySummary[]};
export type CompanyProfile=CompanySummary&{companyProfileVersionId:string;technologyFocus:string[];preferredCompetencies:string[];recommendationPriorities:string[];skillEmphasis:string[];weightOverrides:Record<string,"INCREASE">;effectiveAt:string};
export async function getCompanies(){return apiRequest<CompanyCatalog>("/api/v1/companies");}
export async function getCompany(id:string){return apiRequest<CompanyProfile>(`/api/v1/companies/${id}`);}
