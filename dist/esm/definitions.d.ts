import { PluginListenerHandle } from '@capacitor/core';
import type { MediaEventMap } from './index';
export interface TruvideoSdkMediaPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    mediaBuilder(options: {
        filePath: string;
        tag: string;
        metaData: string;
        isLibrary: boolean;
    }): Promise<{
        value: string;
    }>;
    getFileUploadRequestById(options: {
        id: string;
    }): Promise<{
        value: string;
    }>;
    streamFileUploadRequestById(options: {
        id: string;
    }): Promise<{
        value: string;
    }>;
    getAllFileUploadRequests(options: {
        status: string;
    }): Promise<{
        value: string;
    }>;
    streamAllFileUploadRequests(options: {
        status: string;
    }): Promise<{
        value: string;
    }>;
    cancelMedia(options: {
        id: string;
    }): Promise<{
        value: string;
    }>;
    deleteMedia(options: {
        id: string;
    }): Promise<{
        value: string;
    }>;
    pauseMedia(options: {
        id: string;
    }): Promise<{
        value: string;
    }>;
    resumeMedia(options: {
        id: string;
    }): Promise<{
        value: string;
    }>;
    uploadMedia(options: {
        id: string;
    }): Promise<{
        value: string;
    }>;
    search(options: {
        tag: string;
        type: string;
        page: string;
        pageSize: string;
        isLibrary: boolean;
    }): Promise<{
        value: string;
    }>;
    addListener<K extends keyof MediaEventMap>(eventName: K, listenerFunc: (event: MediaEventMap[K]) => void): Promise<PluginListenerHandle>;
}
