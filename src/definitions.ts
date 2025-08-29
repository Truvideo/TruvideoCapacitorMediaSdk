import { PluginListenerHandle } from "@capacitor/core";
import { MediaEventMap } from "./helper";


export interface TruvideoSdkMediaPlugin {
    echo(options: { value: string }): Promise<{ value: string }>;

    // New Methods
    mediaBuilder(options: {
        filePath: string;
        tag: string;
        metaData: string;
    }): Promise<{ value: string }>;

    getFileUploadRequestById(options: { id: string }): Promise<string>;

    getAllFileUploadRequests(options: { status: string }): Promise<string>;

    cancelMedia(options: { id: string }): Promise<{ value: string }>;

    deleteMedia(options: { id: string }): Promise<{ value: string }>;

    pauseMedia(options: { id: string }): Promise<{ value: string }>;

    resumeMedia(options: { id: string }): Promise<{ value: string }>;

    uploadMedia(options: { id: string }): Promise<{ value: string }>;

    search(options: {
        tag: string;
        type: string;
        page: string;
        pageSize: string;
    }): Promise<{ value: string }>;

    addListener<K extends keyof MediaEventMap>(
        eventName: K,
        listenerFunc: (event: MediaEventMap[K]) => void
    ): Promise<PluginListenerHandle>;
}
