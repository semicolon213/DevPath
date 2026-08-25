import {screen} from "@testing-library/react";
import {afterEach,expect,it,vi} from "vitest";
import {Route,Routes} from "react-router-dom";
import {renderWithProviders} from "../test/renderWithProviders";
import {CompaniesPage} from "./CompaniesPage";
afterEach(()=>vi.unstubAllGlobals());
it("labels the selected supported company without readiness claims",async()=>{vi.stubGlobal("fetch",vi.fn((input:RequestInfo|URL)=>Promise.resolve(Response.json({data:input.toString().endsWith("/companies")?{catalogVersion:"company-v1",companies:[{companyId:"toss",name:"Toss",localizedName:"토스",status:"SUPPORTED",profileVersion:"company-v1",engineeringCulture:"빠른 반복과 정확성"}]}:{careerId:"backend",companyId:"toss",updatedAt:null},metadata:{requestId:"r",apiVersion:"v1",timestamp:"2026-08-12T00:00:00Z"}}))));renderWithProviders(<Routes><Route path="/companies" element={<CompaniesPage/>}/></Routes>,["/companies"]);expect(await screen.findByRole("heading",{name:"지원 회사"})).toBeInTheDocument();expect(screen.getByText("현재 목표")).toBeInTheDocument();expect(screen.getByText(/비공개 채용 기준이나 합격 가능성/)).toBeInTheDocument();});
