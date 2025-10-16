import { TruvideoSdkMedia } from './plugin';

import { PluginListenerHandle } from '@capacitor/core';
import {
  UploadCallbacks,
  UploadCompleteEventData,
  UploadErrorEvent,
  UploadProgressEvent,
  MediaData,
  MediaEventMap,
  SearchData,
  RequestsCallback,
  RequestCallback,
} from './index';

function parsePluginResponse<T>(response: any, valueName: string = "result"): T {
    if (!response || typeof response !== "object") {
        throw new Error("Plugin response is not an object");
    }

    const rawValue = response[valueName];

    if (rawValue === undefined || rawValue === null) {
        throw new Error(`Plugin response.${valueName} is missing`);
    }

    // If it's already an object or boolean/number, return directly
    if (typeof rawValue === "object" || typeof rawValue === "boolean" || typeof rawValue === "number") {
        return rawValue as T;
    }

    if (typeof rawValue !== "string") {
        throw new Error(`Plugin response.${valueName} is not a valid string`);
    }

    try {
        return JSON.parse(rawValue) as T;
    } catch {
        // If parsing fails, return the raw string
        return rawValue as unknown as T;
    }
}

export function addListener<K extends keyof MediaEventMap>( eventName: K,listenerFunc: (event: MediaEventMap[K]) => void): Promise<PluginListenerHandle>{
  return TruvideoSdkMedia.addListener(eventName, listenerFunc);
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
export enum MediaType {
  IMAGE ="Image",
  VIDEO ="Video",
  AUDIO ="AUDIO",
  PDF ="PDF",
  
}
export async function getAllFileUploadRequests(status?: UploadRequestStatus): Promise<MediaData[]> {
  let response = await TruvideoSdkMedia.getAllFileUploadRequests({ status : status || ''});
  return parsePluginResponse<MediaData[]>(response,"requests");
}

const mediaRequest: MediaRequestClass[] = [];

export async function streamAllFileUploadRequests(status?: UploadRequestStatus, callbacks? : RequestsCallback) {
  mediaRequest.length = 0; // Clear previous requests
  TruvideoSdkMedia.streamAllFileUploadRequests({ status : status || ''});
  TruvideoSdkMedia.addListener('AllStream', (data) => {
    const results = parsePluginResponse<MediaData[]>(data, "requests");
    if (callbacks && typeof callbacks.onComplete === "function" && Array.isArray(results)) {
      results.forEach(result => {
        const requestFound = mediaRequest.find(req => req.id === result.id);
        if (requestFound) {
          // Update existing request
          Object.assign(requestFound, result);
        } else {
          // Create new request and add to list
          const request = new MediaRequestClass(result);
          mediaRequest.push(request);
        }
      });
      callbacks.onComplete(mediaRequest);
    }
  });
}

export async function streamFileUploadRequestById(id?: string, callbacks? : RequestCallback) {
  TruvideoSdkMedia.streamFileUploadRequestById({ id : id || ''});
  mediaRequest.length = 0; // Clear previous requests
  TruvideoSdkMedia.addListener('stream', (data) => {
    const result = parsePluginResponse<MediaData>(data,"request");
    if (callbacks && typeof callbacks.onComplete === 'function') {
      const requestFound = mediaRequest.find(request => request.id === result.id);
      if (requestFound) {
        // Update existing request
        Object.assign(requestFound, result);
        callbacks.onComplete(requestFound);
        return;
      }else{
        const request = new MediaRequestClass(result); 
        mediaRequest.push(request);
        callbacks.onComplete(request);
      }
    }
  });
} 

export async function getFileUploadRequestById(id : string): Promise<MediaData> {
  let response = await TruvideoSdkMedia.getFileUploadRequestById({ id : id || ''});
  return parsePluginResponse<MediaData>(response,"request");
}

export async function search(tag : Map<string,string>,page : Number, pageSize : Number ,type : MediaType): Promise<SearchData[]> {
  let response = await TruvideoSdkMedia.search({ tag : JSON.stringify(tag) || '', type: type, page: page.toString(), pageSize: pageSize.toString() });
  return parsePluginResponse<SearchData[]>(response,"response");
}


export class MediaBuilder {
  private _filePath: string;
  private _metaData: Map<string, string> = new Map();
  private _tag: Map<string, string> = new Map();
  private mediaDetail: MediaData | undefined;
  private listeners: PluginListenerHandle[] = [];
  private currentUploadId: string | undefined;

  constructor(filePath: string) {
    if (!filePath) {
      throw new Error('filePath is required for MediaBuilder.');
    }
    this._filePath = filePath;
  }

  setTag(key: string, value: string): MediaBuilder {
    this._tag.set(key, value);
    return this;
  }

  getTag(): Map<string, string> {
    return this._tag;
  }

  setMetaData(key: string, value: string): MediaBuilder {
    this._metaData.set(key, value);
    return this;
  }

  getMetaData(): Map<string, string> {
    return this._metaData;
  }

  clearTags(): MediaBuilder {
    this._tag.clear();
    return this;
  }

  deleteTag(key: string): MediaBuilder {
    this._tag.delete(key);
    return this;
  }

  deleteMetaData(key: string): MediaBuilder {
    this._metaData.delete(key);
    return this;
  }

  clearMetaDatas(): MediaBuilder {
    this._metaData.clear();
    return this;
  }

  private mapToJsonObject(map: Map<string, string>): { [key: string]: string } {
    const obj: { [key: string]: string } = {};
    map.forEach((value, key) => {
      obj[key] = value;
    });
    return obj;
  }

  async build(): Promise<MediaBuilder> {
    const tag = JSON.stringify(this.mapToJsonObject(this._tag));
    const metaData = JSON.stringify(this.mapToJsonObject(this._metaData));

    const response = await TruvideoSdkMedia.mediaBuilder({
      filePath: this._filePath,
      tag,
      metaData,
    });

    this.mediaDetail = JSON.parse(response.value);
    return this;
  }

  async cancel(): Promise<string> {
    if (!this.mediaDetail) return Promise.reject('mediaDetail is undefined');
    const res = await TruvideoSdkMedia.cancelMedia({ id: this.mediaDetail.id });
    return res.value;
  }

  async delete(): Promise<string> {
    if (!this.mediaDetail) return Promise.reject('mediaDetail is undefined');
    const res = await TruvideoSdkMedia.deleteMedia({ id: this.mediaDetail.id });
    return res.value;
  }

  async pause(): Promise<string> {
    if (!this.mediaDetail) return Promise.reject('mediaDetail is undefined');
    const res = await TruvideoSdkMedia.pauseMedia({ id: this.mediaDetail.id });
    return res.value;
  }

  async resume(): Promise<string> {
    if (!this.mediaDetail) return Promise.reject('mediaDetail is undefined');
    const res = await TruvideoSdkMedia.resumeMedia({ id: this.mediaDetail.id });
    return res.value;
  }

  async upload(callbacks: UploadCallbacks): Promise<string> {
    if (!this.mediaDetail) return Promise.reject('mediaDetail is undefined');

    this.removeEventListeners();
    this.currentUploadId = this.mediaDetail.id;

    const onProgress = await TruvideoSdkMedia.addListener(
      'onProgress',
      (event: UploadProgressEvent) => {
        if (event.id === this.currentUploadId && callbacks.onProgress) {
          callbacks.onProgress(event);
        }
      }
    );

    const onComplete = await TruvideoSdkMedia.addListener(
      'onComplete',
      (event: UploadCompleteEventData) => {
        if (event.id === this.currentUploadId && callbacks.onComplete) {
          if (typeof event.metaData === 'string') event.metaData = JSON.parse(event.metaData);
          if (typeof event.tags === 'string') event.tags = JSON.parse(event.tags);
          callbacks.onComplete(event);
        }
        this.removeEventListeners();
      }
    );

    const onError = await TruvideoSdkMedia.addListener(
      'onError',
      (event: UploadErrorEvent) => {
        if (event.id === this.currentUploadId && callbacks.onError) {
          callbacks.onError(event);
        }
        this.removeEventListeners();
      }
    );

    this.listeners.push(onProgress, onComplete, onError);

    const result = await TruvideoSdkMedia.uploadMedia({ id: this.mediaDetail.id });
    return result.value;
  }

  removeEventListeners(): void {
    this.listeners.forEach((listener) => listener.remove());
    this.listeners = [];
    this.currentUploadId = undefined;
  }
}


export class MediaRequestClass {
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
    tags : any;
    metaData : any;
    createdAt: string;
    updatedAt: string;
    errorMessage : string;   
    constructor(data: MediaData) {
        this.id = data.id;
        this.filePath = data.filePath;
        this.fileType = data.fileType;
        this.durationMilliseconds = data.durationMilliseconds;
        this.remoteId = data.remoteId;
        this.remoteURL = data.remoteURL;
        this.transcriptionURL = data.transcriptionURL;
        this.transcriptionLength = data.transcriptionLength;
        this.status = data.status;
        this.progress = data.progress;
        this.tags = data.tags;
        this.metaData = data.metaData;
        this.createdAt = data.createdAt;
        this.updatedAt = data.updatedAt;
        this.errorMessage = data.errorMessage;
    }
  private listeners: PluginListenerHandle[] = [];
  async cancel(): Promise<string> {
    if (!this.id) return Promise.reject('mediaDetail is undefined');
    const res = await TruvideoSdkMedia.cancelMedia({ id: this.id });
    return res.value;
  }

  async delete(): Promise<string> {
    if (!this.id) return Promise.reject('mediaDetail is undefined');
    const res = await TruvideoSdkMedia.deleteMedia({ id: this.id });
    return res.value;
  }

  async pause(): Promise<string> {
    if (!this.id) return Promise.reject('mediaDetail is undefined');
    const res = await TruvideoSdkMedia.pauseMedia({ id: this.id });
    return res.value;
  }

  async resume(): Promise<string> {
    if (!this.id) return Promise.reject('mediaDetail is undefined');
    const res = await TruvideoSdkMedia.resumeMedia({ id: this.id });
    return res.value;
  }

  async upload(callbacks: UploadCallbacks): Promise<string> {
    if (!this.id) return Promise.reject('mediaDetail is undefined');

    this.removeEventListeners();
  
    const onProgress = await TruvideoSdkMedia.addListener(
      'onProgress',
      (event: UploadProgressEvent) => {
        if (event.id === this.id && callbacks.onProgress) {
          callbacks.onProgress(event);
        }
      }
    );

    const onComplete = await TruvideoSdkMedia.addListener(
      'onComplete',
      (event: UploadCompleteEventData) => {
        if (event.id === this.id && callbacks.onComplete) {
          if (typeof event.metaData === 'string') event.metaData = JSON.parse(event.metaData);
          if (typeof event.tags === 'string') event.tags = JSON.parse(event.tags);
          callbacks.onComplete(event);
        }
        this.removeEventListeners();
      }
    );

    const onError = await TruvideoSdkMedia.addListener(
      'onError',
      (event: UploadErrorEvent) => {
        if (event.id === this.id && callbacks.onError) {
          callbacks.onError(event);
        }
        this.removeEventListeners();
      }
    );

    this.listeners.push(onProgress, onComplete, onError);

    const result = await TruvideoSdkMedia.uploadMedia({ id: this.id });
    return result.value;
  }

  removeEventListeners(): void {
    this.listeners.forEach((listener) => listener.remove());
    this.listeners = [];
  }
}
