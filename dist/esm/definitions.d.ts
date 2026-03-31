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
    search(options: {
        tag: string;
        type: string;
        page: string;
        pageSize: string;
        isLibrary: boolean;
    }): Promise<{
        value: string;
    }>;
    getAllUploadRequests(): Promise<{
        requests: string;
    }>;
    getUploadRequestById(options: {
        id: string;
    }): Promise<{
        request: string;
    }>;
    streamUploadRequestById(options: {
        id: string;
    }): Promise<{
        request: string;
    }>;
    streamAllUploadRequests(): Promise<{
        requests: string;
    }>;
    stopAllUploadRequests(): Promise<void>;
    pauseStream(options: {
        id: string;
    }): Promise<{
        request: string;
    }>;
    resumeStream(options: {
        id: string;
    }): Promise<{
        request: string;
    }>;
    retryStream(options: {
        id: string;
    }): Promise<{
        request: string;
    }>;
    deleteStream(options: {
        id: string;
    }): Promise<{
        request: string;
    }>;
    uploadStreamUploadRequest(options: {
        id: string;
        title: string;
        tags: string;
        metadata: string;
        includeInReport: boolean;
        isLibrary: boolean;
    }): Promise<{
        request: string;
    }>;
    addListener<K extends keyof MediaEventMap>(eventName: K, listenerFunc: (event: MediaEventMap[K]) => void): Promise<PluginListenerHandle>;
}
