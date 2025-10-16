//export * from './definitions';
export * from './media-builder';
export * from './plugin'; // this will export TruvideoSdkMedia


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
    tags : object;
    metaData : object;
    createdAt: string;
    updatedAt: string;
    errorMessage : string;
}

export interface SearchData {
    id: string;
    createdAt: string;
    remoteId: string;
    uploadedFileURL: string;
    tags : object;
    metaData : object;
    transcriptionURL: string;
    transcriptionLength: number;
    fileType: string;    
    thumbnailUrl: string;
    previewUrl : string;
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