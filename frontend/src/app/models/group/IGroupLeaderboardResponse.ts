import { IUserResponse } from "../user/IUserResponse";

export interface IGroupLeaderboardResponse {
    user: IUserResponse;
    totalScore: number;
}
