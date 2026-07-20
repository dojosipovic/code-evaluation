import { IUserResponse } from "../user/IUserResponse";

export interface IPlagScanClusterMember {
    id: number;
    submissionId: number;
    user: IUserResponse;
    submittedAt: string;
}
