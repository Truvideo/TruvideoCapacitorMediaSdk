import { TruvideoSdkMedia } from "./plugin";
export async function getFileUploadRequestById(id) {
    var options = { id: id };
    var response = await TruvideoSdkMedia.getFileUploadRequestById(options);
    return parsePluginResponse(response);
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
export async function getAllFileUploadRequests(status) {
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
export async function search(tag, page, isLibrary, pageSize, type) {
    var options = {
        tag: JSON.stringify(tag),
        type: type || "",
        isLibrary: isLibrary,
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
//# sourceMappingURL=helper.js.map