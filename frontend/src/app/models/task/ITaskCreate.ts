import { IStarterCode } from "./IStarterCode";
import { ITestCase } from "./ITestCase";

export interface ITaskCreate {
    id?: number;
    title: string;
    description: string;
    includeStarterCode: boolean;
    starterCode: IStarterCode;
    publicTests: ITestCase[];
    hiddenTests: ITestCase[];
}
