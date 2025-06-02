import { PluginListenerHandle } from "@capacitor/core";

export type MediaEventMap = {
    onProgress: { id: string; progress: number };
    onComplete: { id: string; response: any };
    onError: { id: string; error: any };
};
export interface TruvideoSdkMediaPlugin {
    echo(options: { value: string }): Promise<{ value: string }>;

    uploadMedia(options: object): Promise<{}>;

    addListener<K extends keyof MediaEventMap>(
        eventName: K,
        listenerFunc: (event: MediaEventMap[K]) => void
    ): Promise<PluginListenerHandle>;
}
