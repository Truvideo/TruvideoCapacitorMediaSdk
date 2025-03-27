import { WebPlugin } from '@capacitor/core';
import type { MediaPluginPlugin } from './definitions';
export declare class MediaPluginWeb extends WebPlugin implements MediaPluginPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
