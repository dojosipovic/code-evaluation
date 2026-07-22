import { TestVisibilityEnum } from "../enum/TestVisibilityEnum";

export interface ITestResponse {
    input: string;
    output: string | null;
    visibility: TestVisibilityEnum;
}
