import {afterEach,expect,it,vi} from "vitest";
import {getCompanies,getCompany} from "./companyApi";
afterEach(()=>vi.unstubAllGlobals());
it("loads only server-provided company policy",async()=>{const metadata={requestId:"r",apiVersion:"v1",timestamp:"2026-08-12T00:00:00Z"};const fetchMock=vi.fn().mockResolvedValueOnce(Response.json({data:{catalogVersion:"company-v1",companies:[{companyId:"toss"}]},metadata})).mockResolvedValueOnce(Response.json({data:{companyId:"toss",weightOverrides:{TESTING:"INCREASE"}},metadata}));vi.stubGlobal("fetch",fetchMock);await expect(getCompanies()).resolves.toMatchObject({catalogVersion:"company-v1"});await expect(getCompany("toss")).resolves.toMatchObject({weightOverrides:{TESTING:"INCREASE"}});});
