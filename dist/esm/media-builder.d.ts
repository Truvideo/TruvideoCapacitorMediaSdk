import { PluginListenerHandle } from '@capacitor/core';
import type { UploadCallbacks, UploadCompleteEventData, MediaData, MediaEventMap } from './index';
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
export declare function getFileUploadRequestById(id: string): Promise<MediaData>;
export declare function search(tag: Map<string, string>, page: Number, pageSize: Number, type: MediaType): Promise<UploadCompleteEventData[]>;
export declare class MediaBuilder {
    private _filePath;
    private _metaData;
    private _tag;
    private mediaDetail;
    private listeners;
    private currentUploadId;
    constructor(filePath: string);
    setTag(key: string, value: string): MediaBuilder;
    getTag(): Map<string, string>;
    setMetaData(key: string, value: string): MediaBuilder;
    getMetaData(): Map<string, string>;
    clearTags(): MediaBuilder;
    deleteTag(key: string): MediaBuilder;
    deleteMetaData(key: string): MediaBuilder;
    clearMetaDatas(): MediaBuilder;
    private mapToJsonObject;
    build(): Promise<MediaBuilder>;
    cancel(): Promise<string>;
    delete(): Promise<string>;
    pause(): Promise<string>;
    resume(): Promise<string>;
    upload(callbacks: UploadCallbacks): Promise<string>;
    removeEventListeners(): void;
}
