import { TruvideoSdkMedia } from './plugin';
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
export function addListener(eventName, listenerFunc) {
    return TruvideoSdkMedia.addListener(eventName, listenerFunc);
}
export var UploadRequestStatus;
(function (UploadRequestStatus) {
    UploadRequestStatus["UPLOADING"] = "UPLOADING";
    UploadRequestStatus["IDLE"] = "IDLE";
    UploadRequestStatus["ERROR"] = "ERROR";
    UploadRequestStatus["PAUSED"] = "PAUSED";
    UploadRequestStatus["COMPLETED"] = "COMPLETED";
    UploadRequestStatus["CANCELED"] = "CANCELED";
    UploadRequestStatus["SYNCHRONIZING"] = "SYNCHRONIZING";
})(UploadRequestStatus || (UploadRequestStatus = {}));
export var MediaType;
(function (MediaType) {
    MediaType["IMAGE"] = "Image";
    MediaType["VIDEO"] = "Video";
    MediaType["AUDIO"] = "AUDIO";
    MediaType["PDF"] = "PDF";
})(MediaType || (MediaType = {}));
export async function getAllFileUploadRequests(status) {
    let response = await TruvideoSdkMedia.getAllFileUploadRequests({ status: status || '' });
    return parsePluginResponse(response, "requests");
}
const mediaRequest = [];
export async function streamAllFileUploadRequests(status, callbacks) {
    mediaRequest.length = 0; // Clear previous requests
    TruvideoSdkMedia.streamAllFileUploadRequests({ status: status || '' });
    TruvideoSdkMedia.addListener('AllStream', (data) => {
        const results = parsePluginResponse(data, "requests");
        if (callbacks && typeof callbacks.onComplete === "function" && Array.isArray(results)) {
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
            callbacks.onComplete(mediaRequest);
        }
    });
}
export async function streamFileUploadRequestById(id, callbacks) {
    TruvideoSdkMedia.streamFileUploadRequestById({ id: id || '' });
    mediaRequest.length = 0; // Clear previous requests
    TruvideoSdkMedia.addListener('stream', (data) => {
        const result = parsePluginResponse(data, "request");
        if (callbacks && typeof callbacks.onComplete === 'function') {
            const requestFound = mediaRequest.find(request => request.id === result.id);
            if (requestFound) {
                // Update existing request
                Object.assign(requestFound, result);
                callbacks.onComplete(requestFound);
                return;
            }
            else {
                const request = new MediaRequestClass(result);
                mediaRequest.push(request);
                callbacks.onComplete(request);
            }
        }
    });
}
export async function getFileUploadRequestById(id) {
    let response = await TruvideoSdkMedia.getFileUploadRequestById({ id: id || '' });
    return parsePluginResponse(response, "request");
}
export async function search(tag, page, pageSize, type, isLibrary) {
    let raw = await TruvideoSdkMedia.search({ tag: JSON.stringify(tag) || '', type: type, page: page.toString(), pageSize: pageSize.toString(), isLibrary: isLibrary });
    //let searchData = parsePluginResponse<SearchData[]>(response,"response");
    const response = JSON.parse(raw.value);
    const data = parsePluginResponse(raw, "response");
    return {
        data: data,
        page: response.page,
        pageSize: response.pageSize,
        totalPages: response.totalPages,
        totalElements: response.totalElements,
        numberOfElements: response.numberOfElements,
        size: response.size,
        pageNumber: response.pageNumber,
        first: response.first,
        empty: response.empty,
        last: response.last
    };
}
export class MediaBuilder {
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
export class MediaRequestClass {
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
//# sourceMappingURL=media-builder.js.map