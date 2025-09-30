var capacitorTruvideoSdkMedia = (function (exports, core) {
    'use strict';

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
    async function getFileUploadRequestById(id) {
        let response = await TruvideoSdkMedia.getFileUploadRequestById({ id: id || '' });
        return parsePluginResponse(response, "request");
    }
    async function search(tag, page, pageSize, type) {
        let response = await TruvideoSdkMedia.search({ tag: JSON.stringify(tag) || '', type: type, page: page.toString(), pageSize: pageSize.toString() });
        return parsePluginResponse(response, "response");
    }
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

    exports.MediaBuilder = MediaBuilder;
    exports.TruvideoSdkMedia = TruvideoSdkMedia;
    exports.addListener = addListener;
    exports.getAllFileUploadRequests = getAllFileUploadRequests;
    exports.getFileUploadRequestById = getFileUploadRequestById;
    exports.search = search;

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
