'use strict';

var core = require('@capacitor/core');

const TruvideoSdkMedia = core.registerPlugin('TruvideoSdkMedia');

class MediaBuilder {
    constructor(filePath) {
        this._metaData = new Map();
        this._tag = new Map();
        this.listeners = [];
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

async function getFileUploadRequestById(id) {
    var options = { id: id };
    var response = await TruvideoSdkMedia.getFileUploadRequestById(options);
    return parsePluginResponse(response);
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
async function getAllFileUploadRequests(status) {
    var option = { status: status || "" };
    var response = await TruvideoSdkMedia.getAllFileUploadRequests(option);
    return parsePluginResponse(response);
}
var MediaType;
(function (MediaType) {
    MediaType["IMAGE"] = "Image";
    MediaType["VIDEO"] = "Video";
    MediaType["AUDIO"] = "AUDIO";
    MediaType["PDF"] = "PDF";
})(MediaType || (MediaType = {}));
async function search(tag, page, pageSize, type) {
    var options = {
        tag: JSON.stringify(tag),
        type: type || "",
        page: page.toString(),
        pageSize: pageSize.toString()
    };
    var response = await TruvideoSdkMedia.search(options);
    return parsePluginResponse(response);
}
function parsePluginResponse(response) {
    if (!response || typeof response !== 'object') {
        throw new Error("Plugin response is not an object");
    }
    if (!response.result || typeof response.result !== 'string') {
        throw new Error("Plugin response.result is not a valid string");
    }
    try {
        return JSON.parse(response.result);
    }
    catch (e) {
        throw new Error("Failed to parse plugin response.result: " + e);
    }
}

exports.MediaBuilder = MediaBuilder;
exports.getAllFileUploadRequests = getAllFileUploadRequests;
exports.getFileUploadRequestById = getFileUploadRequestById;
exports.search = search;
//# sourceMappingURL=plugin.cjs.js.map
