import { TruvideoSdkMedia } from "./plugin";


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

export type MediaEventMap = {
    onProgress: UploadProgressEvent;
    onComplete: UploadCompleteEventData;
    onError: UploadErrorEvent;
};


export async function getFileUploadRequestById(id: string ) : Promise<MediaData>{
    var options = { id: id }
    var response =  await TruvideoSdkMedia.getFileUploadRequestById(options);
    return parsePluginResponse<MediaData>(response);
}

export enum UploadRequestStatus {
  UPLOADING ="UPLOADING",
  IDLE ="IDLE",
  ERROR ="ERROR",
  PAUSED ="PAUSED",
  COMPLETED ="COMPLETED",
  CANCELED ="CANCELED",
  SYNCHRONIZING ="SYNCHRONIZING",
}

export async function getAllFileUploadRequests(status? : UploadRequestStatus) : Promise<MediaData[]>{
    var option = { status: status || "" }
    var response =  await TruvideoSdkMedia.getAllFileUploadRequests(option);
    return parsePluginResponse<MediaData[]>(response);
}

enum MediaType {
  IMAGE = 'Image',
  VIDEO = 'Video',
  AUDIO = 'AUDIO',
  PDF = 'PDF',
}

export async function search(
        tag: Map<string,string>,
        page: number,
        isLibrary : boolean,
        pageSize: number, 
        type?: MediaType,
    ) : Promise<MediaData[]>{

    var options =  { 
        tag: JSON.stringify(tag),
        type: type  || "",
        isLibrary: isLibrary,
        page: page.toString(),
        pageSize: pageSize.toString() 
    }
    var response =  await TruvideoSdkMedia.search(options);
    return parsePluginResponse<MediaData[]>(response);
}

function parsePluginResponse<T>(response: any): T {
    if (!response || typeof response !== 'object') {
        throw new Error("Plugin response is not an object");
    }

    if (!response.result || typeof response.result !== 'string') {
        throw new Error("Plugin response.result is not a valid string");
    }

    try {
        return JSON.parse(response.result) as T;
    } catch (e) {
        throw new Error("Failed to parse plugin response.result: " + e);
    }
}
