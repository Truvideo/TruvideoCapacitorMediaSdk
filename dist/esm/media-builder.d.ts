import { PluginListenerHandle } from '@capacitor/core';
import { UploadCallbacks, MediaData, MediaEventMap, RequestsCallback, RequestCallback, SearchPaginationData } from './index';
export declare function addListener<K extends keyof MediaEventMap>(eventName: K, listenerFunc: (event: MediaEventMap[K]) => void): Promise<PluginListenerHandle>;
export declare enum UploadRequestStatus {
    UPLOADING = "UPLOADING",
    IDLE = "IDLE",
    ERROR = "ERROR",
    PAUSED = "PAUSED",
    COMPLETED = "COMPLETED",
    CANCELED = "CANCELED",
    SYNCHRONIZING = "SYNCHRONIZING"
}
export declare enum MediaType {
    IMAGE = "Image",
    VIDEO = "Video",
    AUDIO = "AUDIO",
    PDF = "PDF"
}
export declare function getAllFileUploadRequests(status?: UploadRequestStatus): Promise<MediaData[]>;
export declare function streamAllFileUploadRequests(status?: UploadRequestStatus, callbacks?: RequestsCallback): Promise<void>;
export declare function stopAllFileUploadRequests(): Promise<void>;
export declare function stopFileUploadRequestById(): Promise<void>;
export declare function streamFileUploadRequestById(id?: string, callbacks?: RequestCallback): Promise<void>;
export declare function getFileUploadRequestById(id: string): Promise<MediaData>;
export declare function search(tag: Map<string, string>, page: Number, pageSize: Number, type: MediaType, isLibrary: boolean): Promise<SearchPaginationData>;
export declare class MediaBuilder {
    private _filePath;
    private _metaData;
    private _tag;
    mediaDetail: MediaData | undefined;
    private listeners;
    private currentUploadId;
    private isLibrary;
    constructor(filePath: string);
    setTag(key: string, value: string): MediaBuilder;
    getTag(): Map<string, string>;
    setMetaData(key: string, value: string): MediaBuilder;
    getMetaData(): Map<string, string>;
    clearTags(): MediaBuilder;
    deleteTag(key: string): MediaBuilder;
    deleteMetaData(key: string): MediaBuilder;
    clearMetaDatas(): MediaBuilder;
    setIsLibrary(isLibrary: boolean): MediaBuilder;
    private mapToJsonObject;
    build(): Promise<MediaBuilder>;
    cancel(): Promise<string>;
    delete(): Promise<string>;
    pause(): Promise<string>;
    resume(): Promise<string>;
    upload(callbacks: UploadCallbacks): Promise<string>;
    removeEventListeners(): void;
}
export declare class MediaRequestClass {
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
    tags: any;
    metaData: any;
    createdAt: string;
    updatedAt: string;
    errorMessage: string;
    constructor(data: MediaData);
    private listeners;
    cancel(): Promise<string>;
    delete(): Promise<string>;
    pause(): Promise<string>;
    resume(): Promise<string>;
    upload(callbacks: UploadCallbacks): Promise<string>;
    removeEventListeners(): void;
}
