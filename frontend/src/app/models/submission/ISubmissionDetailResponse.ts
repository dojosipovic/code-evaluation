import { SubmissionStatusEnum } from "../enum/SubmissionStatusEnum";
import { IUserResponse } from "../user/IUserResponse";
import { ISubmissionSimilarityResponse } from "./ISubmissionSimilarityResponse";
import { ISubmissionTestRunResponse } from "./ISubmissionTestRunReponse";

export interface ISubmissionDetailResponse {
    id: number;
    assignmentId: number;
    taskId: number | null;
    user: IUserResponse;
    status: SubmissionStatusEnum;
    code: string | null;
    finalScore: number | null;
    submittedAt: string;
    testRuns: ISubmissionTestRunResponse[];
    similarities: ISubmissionSimilarityResponse[];
}
