import { WebPlugin } from '@capacitor/core';
import type { TruvideoSdkMediaPlugin } from './definitions';
export declare class TruvideoSdkMediaWeb extends WebPlugin implements TruvideoSdkMediaPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
