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
    stopFileUploadRequestById(): Promise<void>;
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
    stopAllFileUploadRequests(): Promise<void>;
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
    getAllStreamUploadRequests(): Promise<any>;
    getStreamUploadRequestById(options: {
        id: string;
    }): Promise<any>;
    uploadStreamUploadRequest(options: {
        id: string;
        title: string;
        tags: string;
        metadata: string;
        includeInReport: boolean;
        isLibrary: boolean;
    }): Promise<any>;
    pauseStreamUploadRequest(options: {
        id: string;
    }): Promise<any>;
    resumeStreamUploadRequest(options: {
        id: string;
    }): Promise<any>;
    retryStreamUploadRequest(options: {
        id: string;
    }): Promise<any>;
    deleteStreamUploadRequest(options: {
        id: string;
    }): Promise<any>;
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
