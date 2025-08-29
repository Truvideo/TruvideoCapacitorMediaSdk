import type { UploadCallbacks } from './helper';
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
