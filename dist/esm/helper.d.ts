export interface MediaData {
    id: string;
    filePath: string;
    fileType: string;
    createdAt: string;
    updatedAt: string;
    tags: string;
    metaData: string;
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
export declare type MediaEventMap = {
    onProgress: UploadProgressEvent;
    onComplete: UploadCompleteEventData;
    onError: UploadErrorEvent;
};
export declare function getFileUploadRequestById(id: string): Promise<MediaData>;
export declare enum UploadRequestStatus {
    UPLOADING = "UPLOADING",
    IDLE = "IDLE",
    ERROR = "ERROR",
    PAUSED = "PAUSED",
    COMPLETED = "COMPLETED",
    CANCELED = "CANCELED",
    SYNCHRONIZING = "SYNCHRONIZING"
}
export declare function getAllFileUploadRequests(status?: UploadRequestStatus): Promise<MediaData[]>;
declare enum MediaType {
    IMAGE = "Image",
    VIDEO = "Video",
    AUDIO = "AUDIO",
    PDF = "PDF"
}
export declare function search(tag: Map<string, string>, page: number, pageSize: number, type?: MediaType): Promise<MediaData[]>;
export {};
