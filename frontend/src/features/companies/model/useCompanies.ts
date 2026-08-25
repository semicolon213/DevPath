import {useQuery} from "@tanstack/react-query";
import {getCompanies,getCompany} from "../api/companyApi";
export const companiesKey=["companies"] as const;
export function useCompanies(){return useQuery({queryKey:companiesKey,queryFn:getCompanies});}
export function useCompany(id?:string){return useQuery({queryKey:[...companiesKey,id],queryFn:()=>getCompany(id!),enabled:Boolean(id)});}
