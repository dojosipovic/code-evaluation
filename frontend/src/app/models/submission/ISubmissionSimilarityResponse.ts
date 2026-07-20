import { IUserResponse } from "../user/IUserResponse";

export interface ISubmissionSimilarityResponse {
    id: number;
    plagiarismRunId: number;
    matchedSubmissionId: number;
    matchedUser: IUserResponse;
    similarityScore: number;
    createdAt: string;
}
