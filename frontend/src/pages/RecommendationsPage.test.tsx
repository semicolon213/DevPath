import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "../routes/AppRoutes";
import { renderWithProviders } from "../test/renderWithProviders";

const timestamp="2026-08-25T00:00:00Z";
const metadata={requestId:"request-1",apiVersion:"v1",timestamp};
const recommendation={recommendationId:"recommendation-1",skillGapId:"gap-1",category:"TESTING",type:"PROJECT",priority:"HIGH",rationaleCode:"CAREER_REQUIRED_GAP",title:"자동화 테스트 강화",completionCriteria:"공식 Testing 점수 60 이상과 테스트 파일 근거를 확보합니다.",expectedEvidence:["test files"],evidenceIds:["evidence-1"],effortHours:16,position:0,status:"PROPOSED"};

describe("recommendation workspace",()=>{afterEach(()=>vi.unstubAllGlobals());
  it("renders versioned recommendation-set history",async()=>{vi.stubGlobal("fetch",vi.fn(()=>Promise.resolve(Response.json({data:{recommendationSets:[{recommendationSetId:"set-1",careerReadinessId:"readiness-1",policyVersion:"recommendation-v1",status:"PUBLISHED",recommendations:[recommendation],generatedAt:timestamp}]},metadata}))));renderWithProviders(<AppRoutes/>,["/recommendations"]);expect(await screen.findByRole("heading",{name:"추천 작업공간"})).toBeInTheDocument();expect(screen.getByText("recommendation-v1")).toBeInTheDocument();expect(screen.getByRole("link",{name:"준비도 근거 보기"})).toHaveAttribute("href","/career-readiness/readiness-1");expect(screen.getByRole("link",{name:"상세 근거 보기"})).toHaveAttribute("href","/recommendations/recommendation-1");});
  it("renders recommendation detail and owner-scoped evidence",async()=>{vi.stubGlobal("fetch",vi.fn((input:string|URL|Request)=>{const url=String(input);if(url.endsWith("/evidence"))return Promise.resolve(Response.json({data:{recommendationId:"recommendation-1",evidence:[{evidenceId:"evidence-1",evidenceType:"SNAPSHOT_SIGNAL",sourceReference:"README.md",observedFactSummary:"README 파일을 확인했습니다.",confidence:100,createdAt:timestamp}]},metadata}));return Promise.resolve(Response.json({data:recommendation,metadata}));}));renderWithProviders(<AppRoutes/>,["/recommendations/recommendation-1"]);expect(await screen.findByRole("heading",{name:"자동화 테스트 강화"})).toBeInTheDocument();expect(screen.getByRole("heading",{name:"현재 연결된 관찰 근거"})).toBeInTheDocument();expect(screen.getByText("README 파일을 확인했습니다.")).toBeInTheDocument();});
});
