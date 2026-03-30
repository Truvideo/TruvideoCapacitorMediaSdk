'use strict';

var core = require('@capacitor/core');

const TruvideoSdkMedia = core.registerPlugin('TruvideoSdkMedia');

function parsePluginResponse(response, valueName = "result") {
    if (!response || typeof response !== "object") {
        throw new Error("Plugin response is not an object");
    }
    const rawValue = response[valueName];
    if (rawValue === undefined || rawValue === null) {
        throw new Error(`Plugin response.${valueName} is missing`);
    }
    // If it's already an object or boolean/number, return directly
    if (typeof rawValue === "object" || typeof rawValue === "boolean" || typeof rawValue === "number") {
        return rawValue;
    }
    if (typeof rawValue !== "string") {
        throw new Error(`Plugin response.${valueName} is not a valid string`);
    }
    try {
        return JSON.parse(rawValue);
    }
    catch (_a) {
        // If parsing fails, return the raw string
        return rawValue;
    }
}
function addListener(eventName, listenerFunc) {
    return TruvideoSdkMedia.addListener(eventName, listenerFunc);
}
exports.UploadRequestStatus = void 0;
(function (UploadRequestStatus) {
    UploadRequestStatus["UPLOADING"] = "UPLOADING";
    UploadRequestStatus["IDLE"] = "IDLE";
    UploadRequestStatus["ERROR"] = "ERROR";
    UploadRequestStatus["PAUSED"] = "PAUSED";
    UploadRequestStatus["COMPLETED"] = "COMPLETED";
    UploadRequestStatus["CANCELED"] = "CANCELED";
    UploadRequestStatus["SYNCHRONIZING"] = "SYNCHRONIZING";
})(exports.UploadRequestStatus || (exports.UploadRequestStatus = {}));
exports.MediaType = void 0;
(function (MediaType) {
    MediaType["IMAGE"] = "Image";
    MediaType["VIDEO"] = "Video";
    MediaType["AUDIO"] = "AUDIO";
    MediaType["PDF"] = "PDF";
})(exports.MediaType || (exports.MediaType = {}));
async function getAllFileUploadRequests(status) {
    let response = await TruvideoSdkMedia.getAllFileUploadRequests({ status: status || '' });
    return parsePluginResponse(response, "requests");
}
const mediaRequest = [];
let allStreamListenerHandle = null;
let currentCallbacks = undefined;
let byIdStreamListenerHandle = null;
let byIdCurrentCallbacks = undefined;
async function streamAllFileUploadRequests(status, callbacks) {
    mediaRequest.length = 0; // Clear previous requests
    // Update callbacks if provided
    if (callbacks) {
        currentCallbacks = callbacks;
    }
    // Attach listener first so we don't miss early native emissions.
    if (!allStreamListenerHandle) {
        allStreamListenerHandle = await TruvideoSdkMedia.addListener('AllStream', (data) => {
            try {
                const results = parsePluginResponse(data, "requests");
                if (currentCallbacks && typeof currentCallbacks.onComplete === "function" && Array.isArray(results)) {
                    results.forEach(result => {
                        const requestFound = mediaRequest.find(req => req.id === result.id);
                        if (requestFound) {
                            // Update existing request
                            Object.assign(requestFound, result);
                        }
                        else {
                            // Create new request and add to list
                            const request = new MediaRequestClass(result);
                            mediaRequest.push(request);
                        }
                    });
                    currentCallbacks.onComplete(mediaRequest);
                }
            }
            catch (error) {
                if (currentCallbacks && typeof currentCallbacks.onError === "function") {
                    const streamError = {
                        id: "",
                        error
                    };
                    currentCallbacks.onError(streamError);
                }
            }
        });
    }
    // Start or restart the native stream for the requested status.
    await TruvideoSdkMedia.streamAllFileUploadRequests({ status: status || '' });
}
async function stopAllFileUploadRequests() {
    if (allStreamListenerHandle) {
        allStreamListenerHandle.remove();
        allStreamListenerHandle = null;
        currentCallbacks = undefined;
    }
}
async function stopFileUploadRequestById() {
    TruvideoSdkMedia.stopFileUploadRequestById();
    if (byIdStreamListenerHandle) {
        byIdStreamListenerHandle.remove();
        byIdStreamListenerHandle = null;
        byIdCurrentCallbacks = undefined;
    }
}
async function streamFileUploadRequestById(id, callbacks) {
    TruvideoSdkMedia.streamFileUploadRequestById({ id: id || '' });
    mediaRequest.length = 0; // Clear previous requests
    if (callbacks) {
        byIdCurrentCallbacks = callbacks;
    }
    // Keep a single active listener for the by-id stream.
    if (!byIdStreamListenerHandle) {
        byIdStreamListenerHandle = await TruvideoSdkMedia.addListener('stream', (data) => {
            const result = parsePluginResponse(data, "request");
            if (byIdCurrentCallbacks && typeof byIdCurrentCallbacks.onComplete === 'function') {
                const requestFound = mediaRequest.find(request => request.id === result.id);
                if (requestFound) {
                    // Update existing request
                    Object.assign(requestFound, result);
                    byIdCurrentCallbacks.onComplete(requestFound);
                    return;
                }
                else {
                    const request = new MediaRequestClass(result);
                    mediaRequest.push(request);
                    byIdCurrentCallbacks.onComplete(request);
                }
            }
        });
    }
}
async function getFileUploadRequestById(id) {
    let response = await TruvideoSdkMedia.getFileUploadRequestById({ id: id || '' });
    return parsePluginResponse(response, "request");
}
async function getAllStreamUploadRequests() {
    const response = await TruvideoSdkMedia.getAllStreamUploadRequests();
    return parsePluginResponse(response, "requests");
}
async function getStreamUploadRequestById(id, callbacks) {
    if (callbacks) {
        await streamFileUploadRequestById(id, callbacks);
    }
    const response = await TruvideoSdkMedia.getStreamUploadRequestById({ id: id || '' });
    return parsePluginResponse(response, "request");
}
async function uploadStreamUploadRequest(id, title, tags, metadata, includeInReport, isLibrary) {
    const response = await TruvideoSdkMedia.uploadStreamUploadRequest({
        id: id || '',
        title: title || '',
        tags: tags || '',
        metadata: metadata || '',
        includeInReport,
        isLibrary
    });
    return parsePluginResponse(response, "request");
}
async function pauseStreamUploadRequest(id) {
    const response = await TruvideoSdkMedia.pauseStreamUploadRequest({ id: id || '' });
    return parsePluginResponse(response, "request");
}
async function resumeStreamUploadRequest(id) {
    const response = await TruvideoSdkMedia.resumeStreamUploadRequest({ id: id || '' });
    return parsePluginResponse(response, "request");
}
async function retryStreamUploadRequest(id) {
    const response = await TruvideoSdkMedia.retryStreamUploadRequest({ id: id || '' });
    return parsePluginResponse(response, "request");
}
async function deleteStreamUploadRequest(id) {
    const response = await TruvideoSdkMedia.deleteStreamUploadRequest({ id: id || '' });
    return parsePluginResponse(response, "request");
}
async function search(tag, page, pageSize, type, isLibrary) {
    let raw = await TruvideoSdkMedia.search({ tag: JSON.stringify(tag) || '', type: type, page: page.toString(), pageSize: pageSize.toString(), isLibrary: isLibrary });
    //let searchData = parsePluginResponse<SearchData[]>(response,"response");
    const data = parsePluginResponse(raw, "response");
    const totalPages = parsePluginResponse(raw, "totalPages");
    const totalElements = parsePluginResponse(raw, "totalElements");
    const numberOfElements = parsePluginResponse(raw, "numberOfElements");
    const size = parsePluginResponse(raw, "size");
    const number = parsePluginResponse(raw, "number");
    const first = parsePluginResponse(raw, "first");
    const empty = parsePluginResponse(raw, "empty");
    const last = parsePluginResponse(raw, "last");
    return {
        data: data,
        totalPages: totalPages,
        totalElements: totalElements,
        numberOfElements: numberOfElements,
        size: size,
        number: number,
        first: first,
        empty: empty,
        last: last
    };
}
class MediaBuilder {
    constructor(filePath) {
        this._metaData = new Map();
        this._tag = new Map();
        this.listeners = [];
        this.isLibrary = false;
        if (!filePath) {
            throw new Error('filePath is required for MediaBuilder.');
        }
        this._filePath = filePath;
    }
    setTag(key, value) {
        this._tag.set(key, value);
        return this;
    }
    getTag() {
        return this._tag;
    }
    setMetaData(key, value) {
        this._metaData.set(key, value);
        return this;
    }
    getMetaData() {
        return this._metaData;
    }
    clearTags() {
        this._tag.clear();
        return this;
    }
    deleteTag(key) {
        this._tag.delete(key);
        return this;
    }
    deleteMetaData(key) {
        this._metaData.delete(key);
        return this;
    }
    clearMetaDatas() {
        this._metaData.clear();
        return this;
    }
    setIsLibrary(isLibrary) {
        this.isLibrary = isLibrary;
        return this;
    }
    mapToJsonObject(map) {
        const obj = {};
        map.forEach((value, key) => {
            obj[key] = value;
        });
        return obj;
    }
    async build() {
        const tag = JSON.stringify(this.mapToJsonObject(this._tag));
        const metaData = JSON.stringify(this.mapToJsonObject(this._metaData));
        const response = await TruvideoSdkMedia.mediaBuilder({
            filePath: this._filePath,
            tag,
            metaData,
            isLibrary: this.isLibrary,
        });
        this.mediaDetail = JSON.parse(response.value);
        return this;
    }
    async cancel() {
        if (!this.mediaDetail)
            return Promise.reject('mediaDetail is undefined');
        const res = await TruvideoSdkMedia.cancelMedia({ id: this.mediaDetail.id });
        return res.value;
    }
    async delete() {
        if (!this.mediaDetail)
            return Promise.reject('mediaDetail is undefined');
        const res = await TruvideoSdkMedia.deleteMedia({ id: this.mediaDetail.id });
        return res.value;
    }
    async pause() {
        if (!this.mediaDetail)
            return Promise.reject('mediaDetail is undefined');
        const res = await TruvideoSdkMedia.pauseMedia({ id: this.mediaDetail.id });
        return res.value;
    }
    async resume() {
        if (!this.mediaDetail)
            return Promise.reject('mediaDetail is undefined');
        const res = await TruvideoSdkMedia.resumeMedia({ id: this.mediaDetail.id });
        return res.value;
    }
    async upload(callbacks) {
        if (!this.mediaDetail)
            return Promise.reject('mediaDetail is undefined');
        this.removeEventListeners();
        this.currentUploadId = this.mediaDetail.id;
        const onProgress = await TruvideoSdkMedia.addListener('onProgress', (event) => {
            if (event.id === this.currentUploadId && callbacks.onProgress) {
                callbacks.onProgress(event);
            }
        });
        const onComplete = await TruvideoSdkMedia.addListener('onComplete', (event) => {
            if (event.id === this.currentUploadId && callbacks.onComplete) {
                if (typeof event.metaData === 'string')
                    event.metaData = JSON.parse(event.metaData);
                if (typeof event.tags === 'string')
                    event.tags = JSON.parse(event.tags);
                callbacks.onComplete(event);
            }
            this.removeEventListeners();
        });
        const onError = await TruvideoSdkMedia.addListener('onError', (event) => {
            if (event.id === this.currentUploadId && callbacks.onError) {
                callbacks.onError(event);
            }
            this.removeEventListeners();
        });
        this.listeners.push(onProgress, onComplete, onError);
        const result = await TruvideoSdkMedia.uploadMedia({ id: this.mediaDetail.id });
        return result.value;
    }
    removeEventListeners() {
        this.listeners.forEach((listener) => listener.remove());
        this.listeners = [];
        this.currentUploadId = undefined;
    }
}
class MediaRequestClass {
    constructor(data) {
        this.listeners = [];
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
    async cancel() {
        if (!this.id)
            return Promise.reject('mediaDetail is undefined');
        const res = await TruvideoSdkMedia.cancelMedia({ id: this.id });
        return res.value;
    }
    async delete() {
        if (!this.id)
            return Promise.reject('mediaDetail is undefined');
        const res = await TruvideoSdkMedia.deleteMedia({ id: this.id });
        return res.value;
    }
    async pause() {
        if (!this.id)
            return Promise.reject('mediaDetail is undefined');
        const res = await TruvideoSdkMedia.pauseMedia({ id: this.id });
        return res.value;
    }
    async resume() {
        if (!this.id)
            return Promise.reject('mediaDetail is undefined');
        const res = await TruvideoSdkMedia.resumeMedia({ id: this.id });
        return res.value;
    }
    async upload(callbacks) {
        if (!this.id)
            return Promise.reject('mediaDetail is undefined');
        this.removeEventListeners();
        const onProgress = await TruvideoSdkMedia.addListener('onProgress', (event) => {
            if (event.id === this.id && callbacks.onProgress) {
                callbacks.onProgress(event);
            }
        });
        const onComplete = await TruvideoSdkMedia.addListener('onComplete', (event) => {
            if (event.id === this.id && callbacks.onComplete) {
                if (typeof event.metaData === 'string')
                    event.metaData = JSON.parse(event.metaData);
                if (typeof event.tags === 'string')
                    event.tags = JSON.parse(event.tags);
                callbacks.onComplete(event);
            }
            this.removeEventListeners();
        });
        const onError = await TruvideoSdkMedia.addListener('onError', (event) => {
            if (event.id === this.id && callbacks.onError) {
                callbacks.onError(event);
            }
            this.removeEventListeners();
        });
        this.listeners.push(onProgress, onComplete, onError);
        const result = await TruvideoSdkMedia.uploadMedia({ id: this.id });
        return result.value;
    }
    removeEventListeners() {
        this.listeners.forEach((listener) => listener.remove());
        this.listeners = [];
    }
}

exports.MediaBuilder = MediaBuilder;
exports.MediaRequestClass = MediaRequestClass;
exports.TruvideoSdkMedia = TruvideoSdkMedia;
exports.addListener = addListener;
exports.deleteStreamUploadRequest = deleteStreamUploadRequest;
exports.getAllFileUploadRequests = getAllFileUploadRequests;
exports.getAllStreamUploadRequests = getAllStreamUploadRequests;
exports.getFileUploadRequestById = getFileUploadRequestById;
exports.getStreamUploadRequestById = getStreamUploadRequestById;
exports.pauseStreamUploadRequest = pauseStreamUploadRequest;
exports.resumeStreamUploadRequest = resumeStreamUploadRequest;
exports.retryStreamUploadRequest = retryStreamUploadRequest;
exports.search = search;
exports.stopAllFileUploadRequests = stopAllFileUploadRequests;
exports.stopFileUploadRequestById = stopFileUploadRequestById;
exports.streamAllFileUploadRequests = streamAllFileUploadRequests;
exports.streamFileUploadRequestById = streamFileUploadRequestById;
exports.uploadStreamUploadRequest = uploadStreamUploadRequest;
//# sourceMappingURL=plugin.cjs.js.map
