import { PluginListenerHandle } from '@capacitor/core';

export interface MediaData {
    id: string;
    filePath: string;
    fileType: string;
    durationMilliseconds: number;
    remoteId: string;
    remoteURL: string;
    transcriptionURL: string;
    transcriptionLength: number;
    status: string;
    progress: number;
}

export interface UploadProgressEvent {
    id: string;
    progress: string;
}

export interface UploadCompleteEventData {
    id: string;
    createdDate?: string;
    remoteId?: string;
    uploadedFileURL?: string;
    metaData?: any;
    tags?: any;
    transcriptionURL?: string;
    transcriptionLength?: number;
    fileType?: string;
}

export interface UploadErrorEvent {
    id: string;
    error: any;
}

export interface UploadCallbacks {
    onProgress?: (event: UploadProgressEvent) => void;
    onComplete?: (event: UploadCompleteEventData) => void;
    onError?: (event: UploadErrorEvent) => void;
}

export type MediaEventMap = {
    onProgress: UploadProgressEvent;
    onComplete: UploadCompleteEventData;
    onError: UploadErrorEvent;
};

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

    // Event Listeners
    addListener<K extends keyof MediaEventMap>(
        eventName: K,
        listenerFunc: (event: MediaEventMap[K]) => void
    ): Promise<PluginListenerHandle>;
}
