import {screen} from "@testing-library/react";
import {afterEach,expect,it,vi} from "vitest";
import {Route,Routes} from "react-router-dom";
import {renderWithProviders} from "../test/renderWithProviders";
import {CompanyDetailPage} from "./CompanyDetailPage";
afterEach(()=>vi.unstubAllGlobals());
it("shows generic versioned emphasis and guardrail",async()=>{vi.stubGlobal("fetch",vi.fn((input:RequestInfo|URL)=>Promise.resolve(Response.json({data:input.toString().endsWith("/companies/toss")?{companyId:"toss",name:"Toss",localizedName:"토스",status:"SUPPORTED",companyProfileVersionId:"p",profileVersion:"company-v1",engineeringCulture:"빠른 반복과 정확성",technologyFocus:["테스트"],preferredCompetencies:["신뢰성"],recommendationPriorities:["테스트"],skillEmphasis:["정확성"],weightOverrides:{TESTING:"INCREASE"},effectiveAt:"2026-08-12T00:00:00Z"}:{careerId:"backend",companyId:"toss",updatedAt:null},metadata:{requestId:"r",apiVersion:"v1",timestamp:"2026-08-12T00:00:00Z"}}))));renderWithProviders(<Routes><Route path="/companies/:id" element={<CompanyDetailPage/>}/></Routes>,["/companies/toss"]);expect(await screen.findByRole("heading",{name:"토스"})).toBeInTheDocument();expect(screen.getByText(/비공개 채용 정보나 합격 예측이 아닙니다/)).toBeInTheDocument();expect(screen.getByRole("heading",{name:"기술 관심사"})).toBeInTheDocument();});
