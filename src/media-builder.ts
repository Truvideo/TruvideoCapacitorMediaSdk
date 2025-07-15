import { TruvideoSdkMedia } from './index';
import type {
  UploadCallbacks,
  UploadCompleteEventData,
  UploadErrorEvent,
  UploadProgressEvent,
  MediaData,
} from './definitions.ts';
import { PluginListenerHandle } from '@capacitor/core';

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
