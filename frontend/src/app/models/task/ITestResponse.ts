import { TestVisibilityEnum } from "../enum/TestVisibilityEnum";

export interface ITestResponse {
    input: string;
    output: string;
    visibility: TestVisibilityEnum;
}
