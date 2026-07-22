import { IPlagScanClusterMember } from "./IPlagScanClusterMember";

export interface IPlagScanCluster {
    id: number;
    plagiarismRunId: number;
    assignmentId: number;
    assignmentName: string;
    similarity: number;
    createdAt: string;
    members: IPlagScanClusterMember[];
}
