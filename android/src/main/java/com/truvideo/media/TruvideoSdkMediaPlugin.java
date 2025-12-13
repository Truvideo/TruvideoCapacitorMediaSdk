package com.truvideo.media;

import static com.truvideo.sdk.media.TruvideoSdkMedia.TruvideoSdkMedia;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.truvideo.sdk.media.builder.TruvideoSdkMediaFileUploadRequestBuilder;
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaCallback;
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaFileUploadCallback;
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileType;
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadRequest;
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadStatus;
import com.truvideo.sdk.media.model.TruvideoSdkMediaPaginatedResponse;
import com.truvideo.sdk.media.model.TruvideoSdkMediaResponse;
import com.truvideo.sdk.media.model.TruvideoSdkMediaTags;
import com.truvideo.sdk.media.util.DateUtilsKt;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.GlobalScope;
import truvideo.sdk.common.exceptions.TruvideoSdkException;

@CapacitorPlugin(name = "TruvideoSdkMedia")
public class TruvideoSdkMediaPlugin extends Plugin {
    private final CoroutineScope scope = GlobalScope.INSTANCE;
    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        //ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void mediaBuilder(PluginCall call) {
        builder(getContext(),call);
    }

    @PluginMethod
    public void getFileUploadRequestById(PluginCall call) {
        String id = call.getString("id");
        if(id == null){
            return;
        }
        TruvideoSdkMedia.getFileUploadRequestById(id,new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>(){
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest truvideoSdkMediaFileUploadRequest) {
                var mainResponse = returnRequest(truvideoSdkMediaFileUploadRequest);
                // Upload the file
                JSObject ret = new JSObject();
                ret.put("request",mainResponse);
                call.resolve(ret);
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception", "TruvideoSdkException", e);
            }
        });
    }

    @PluginMethod
    public void streamAllFileUploadRequests(PluginCall call){
        String status = call.getString("status");
        try {
            if (status == null || status.isEmpty()) {
                TruvideoSdkMedia.streamAllFileUploadRequests(null, new TruvideoSdkMediaCallback<LiveData<List<TruvideoSdkMediaFileUploadRequest>>>() {
                    @Override
                    public void onComplete(LiveData<List<TruvideoSdkMediaFileUploadRequest>> listLiveData) {
                        listLiveData.observe(getActivity(), new Observer<List<TruvideoSdkMediaFileUploadRequest>>() {
                            @Override
                            public void onChanged(List<TruvideoSdkMediaFileUploadRequest> requests) {
                                if (requests != null) {
                                    JSObject request = new JSObject();
                                    request.put("requests",returnRequestList(requests));
                                    sendEvent("AllStream",request);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception","TruvideoSdkException",e);
                    }
                });
            } else {
                TruvideoSdkMediaFileUploadStatus mainStatus = null;
                switch (status) {
                    case "UPLOADING":
                        mainStatus = TruvideoSdkMediaFileUploadStatus.UPLOADING;
                        break;
                    case "IDLE":
                        mainStatus = TruvideoSdkMediaFileUploadStatus.IDLE;
                        break;
                    case "ERROR":
                        mainStatus = TruvideoSdkMediaFileUploadStatus.ERROR;
                        break;
                    case "PAUSED":
                        mainStatus = TruvideoSdkMediaFileUploadStatus.PAUSED;
                        break;
                    case "COMPLETED":
                        mainStatus = TruvideoSdkMediaFileUploadStatus.COMPLETED;
                        break;
                    case "CANCELED":
                        mainStatus = TruvideoSdkMediaFileUploadStatus.CANCELED;
                        break;
                    case "SYNCHRONIZING":
                        mainStatus = TruvideoSdkMediaFileUploadStatus.SYNCHRONIZING;
                        break;
                }

                TruvideoSdkMedia.streamAllFileUploadRequests(mainStatus, new TruvideoSdkMediaCallback<LiveData<List<TruvideoSdkMediaFileUploadRequest>>>() {
                    @Override
                    public void onComplete(LiveData<List<TruvideoSdkMediaFileUploadRequest>> listLiveData) {
                        listLiveData.observe(getActivity(), new Observer<List<TruvideoSdkMediaFileUploadRequest>>() {
                            @Override
                            public void onChanged(List<TruvideoSdkMediaFileUploadRequest> requests) {
                                if (requests != null) {
                                    JSObject request = new JSObject();
                                    request.put("requests",returnRequestList(requests));
                                    sendEvent("AllStream",request);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception","TruvideoSdkException",e);
                    }
                });
            }
        } catch (Exception e) {
            call.reject("GET_REQUESTS_ERROR", e);
        }

    }

    @PluginMethod
    public void streamFileUploadRequestById(PluginCall call){
        String id = call.getString("id");
        if(id == null){
            return;
        }
        TruvideoSdkMedia.streamFileUploadRequestById(id, new TruvideoSdkMediaCallback<LiveData<TruvideoSdkMediaFileUploadRequest>>() {
            @Override
            public void onComplete(LiveData<TruvideoSdkMediaFileUploadRequest> truvideoSdkMediaFileUploadRequestLiveData) {
                truvideoSdkMediaFileUploadRequestLiveData.observe(getActivity(), new Observer<TruvideoSdkMediaFileUploadRequest>() {
                    @Override
                    public void onChanged(TruvideoSdkMediaFileUploadRequest request) {
                        var mainResponse = returnRequest(request);
                        // Upload the file
                        JSObject ret = new JSObject();
                        ret.put("request",mainResponse);
                        sendEvent("stream",ret);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {

            }
        });
    }

    @PluginMethod
    public void getAllFileUploadRequests(PluginCall call) {
        String status = call.getString("status");
            try {
                if (status == null || status.isEmpty()) {
                    TruvideoSdkMedia.getAllFileUploadRequests(null, new TruvideoSdkMediaCallback<List<TruvideoSdkMediaFileUploadRequest>>() {
                        @Override
                        public void onComplete(List<TruvideoSdkMediaFileUploadRequest> truvideoSdkMediaFileUploadRequests) {
                            JSObject request = new JSObject();
                            request.put("requests",returnRequestList(truvideoSdkMediaFileUploadRequests));
                            call.resolve(request);
                        }

                        @Override
                        public void onError(@NonNull TruvideoSdkException e) {
                            call.reject("SDK Exception","TruvideoSdkException",e);
                        }
                    });
                } else {
                    TruvideoSdkMediaFileUploadStatus mainStatus = null;
                    switch (status) {
                        case "UPLOADING":
                            mainStatus = TruvideoSdkMediaFileUploadStatus.UPLOADING;
                            break;
                        case "IDLE":
                            mainStatus = TruvideoSdkMediaFileUploadStatus.IDLE;
                            break;
                        case "ERROR":
                            mainStatus = TruvideoSdkMediaFileUploadStatus.ERROR;
                            break;
                        case "PAUSED":
                            mainStatus = TruvideoSdkMediaFileUploadStatus.PAUSED;
                            break;
                        case "COMPLETED":
                            mainStatus = TruvideoSdkMediaFileUploadStatus.COMPLETED;
                            break;
                        case "CANCELED":
                            mainStatus = TruvideoSdkMediaFileUploadStatus.CANCELED;
                            break;
                        case "SYNCHRONIZING":
                            mainStatus = TruvideoSdkMediaFileUploadStatus.SYNCHRONIZING;
                            break;
                    }

                    TruvideoSdkMedia.getAllFileUploadRequests(mainStatus, new TruvideoSdkMediaCallback<List<TruvideoSdkMediaFileUploadRequest>>()  {
                        @Override
                        public void onError(@NonNull TruvideoSdkException e) {
                            call.reject("SDK Exception","TruvideoSdkException",e);
                        }

                        @Override
                        public void onComplete(List<TruvideoSdkMediaFileUploadRequest> truvideoSdkMediaFileUploadRequests) {
                            JSObject request = new JSObject();
                            request.put("requests",returnRequestList(truvideoSdkMediaFileUploadRequests));
                            call.resolve(request);
                        }
                    });
                }
            } catch (Exception e) {
                call.reject("GET_REQUESTS_ERROR", e);
            }

    }

    @PluginMethod
    public void cancelMedia(PluginCall call) {
        String id = call.getString("id");
        if(id == null){
            return;
        }
        TruvideoSdkMedia.getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.cancel(new TruvideoSdkMediaCallback<Unit>() {
                    @Override
                    public void onComplete(Unit unit) {
                        JSObject jsObject = new JSObject();
                        jsObject.put("message","Cancel Success");
                        call.resolve(jsObject);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception","TruvideoSdkException",e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception","TruvideoSdkException",e);
            }
        });
    }

    @PluginMethod
    public void deleteMedia(PluginCall call) {
        String id = call.getString("id");
        if(id == null){
            return;
        }
        TruvideoSdkMedia.getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.delete(new TruvideoSdkMediaCallback<Unit>() {
                    @Override
                    public void onComplete(Unit unit) {
                        JSObject jsObject = new JSObject();
                        jsObject.put("message","Delete Success");
                        call.resolve(jsObject);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception","TruvideoSdkException",e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception","TruvideoSdkException",e);
            }
        });
    }

    @PluginMethod
    public void pauseMedia(PluginCall call) {
        String id = call.getString("id");
        if(id == null){
            return;
        }
        TruvideoSdkMedia.getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.pause(new TruvideoSdkMediaCallback<Unit>() {
                    @Override
                    public void onComplete(Unit unit) {
                        JSObject jsObject = new JSObject();
                        jsObject.put("message","Pause Success");
                        call.resolve(jsObject);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception","TruvideoSdkException",e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception","TruvideoSdkException",e);
            }
        });
    }

    @PluginMethod
    public void resumeMedia(PluginCall call) {
        String id = call.getString("id");
        if(id == null){
            return;
        }
        TruvideoSdkMedia.getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.resume(new TruvideoSdkMediaCallback<Unit>() {
                    @Override
                    public void onComplete(Unit unit) {
                        JSObject jsObject = new JSObject();
                        jsObject.put("message","Resume Success");
                        call.resolve(jsObject);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception","TruvideoSdkException",e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception","TruvideoSdkException",e);
            }
        });
    }

    @PluginMethod
    public void search(PluginCall call) {
        String tag = call.getString("tag");
        String type = call.getString("type");
        String page= call.getString("page");
        String pageSize = call.getString("pageSize");
        Boolean isLibrary = call.getBoolean("isLibrary",false);
        try {
            TruvideoSdkMediaFileType typeData;
            if ("All".equals(type)) {
                typeData = TruvideoSdkMediaFileType.All;
            } else if ("Video".equals(type)) {
                typeData = TruvideoSdkMediaFileType.Video;
            } else if ("AUDIO".equals(type)) {
                typeData = TruvideoSdkMediaFileType.AUDIO;
            } else if ("PDF".equals(type)) {
                typeData = TruvideoSdkMediaFileType.PDF;
            } else {
                typeData = TruvideoSdkMediaFileType.Picture;
            }

            JSONObject jsonTag = new JSONObject(tag);
            Map<String, String> map = new HashMap<>();
            Iterator<String> keys = jsonTag.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonTag.getString(key);
                map.put(key, value);
            }

            TruvideoSdkMedia.search(
                    new TruvideoSdkMediaTags(map),
                    typeData,
                    isLibrary,
                    Integer.parseInt(page),
                    Integer.parseInt(pageSize),
                    new TruvideoSdkMediaCallback<TruvideoSdkMediaPaginatedResponse<TruvideoSdkMediaResponse>>() {
                        @Override
                        public void onComplete(TruvideoSdkMediaPaginatedResponse<TruvideoSdkMediaResponse> response) {
                            Gson gson = new Gson();
                            ArrayList<String> list = new ArrayList<>();
                            for (TruvideoSdkMediaResponse it : response.getData()) {
                                Map<String, Object> mainResponse = new HashMap<>();
                                mainResponse.put("id", it.getId());
                                mainResponse.put("createdDate",DateUtilsKt.toIsoString(it.getCreatedDate()));
                                mainResponse.put("remoteId", it.getId());
                                mainResponse.put("uploadedFileURL", it.getUrl());
                                mainResponse.put("metaData", it.getMetadata().toJson());
                                mainResponse.put("tags", it.getTags().toJson());
                                mainResponse.put("transcriptionURL", it.getTranscriptionUrl());
                                mainResponse.put("transcriptionLength", it.getTranscriptionLength());
                                mainResponse.put("fileType", it.getType().name());
                                mainResponse.put("thumbnailUrl", it.getThumbnailUrl());
                                mainResponse.put("previewUrl", it.getPreviewUrl());
                                list.add(gson.toJson(mainResponse));
                            }
                            JSObject jet = new JSObject();
                            jet.put("response",gson.toJson(list));
                            jet.put("totalPages",response.getTotalPages());
                            jet.put("totalElements",response.getTotalElements());
                            jet.put("numberOfElements",response.getNumberOfElements());
                            jet.put("size",response.getSize());
                            jet.put("number",response.getNumber());
                            jet.put("first",response.getFirst());
                            jet.put("empty",response.getEmpty());
                            jet.put("last",response.getLast());
                            // prod 
//                            api-key - EPhPPsbv7e
//                            secret-key - 9lHCnkfeLl
                            call.resolve(jet);
                        }

                        @Override
                        public void onError(@NonNull TruvideoSdkException e) {
                            call.reject("TruvideoSdkException",e);
                        }
                    }
            );

        } catch (Exception e) {
            call.reject("SEARCH_ERROR", e);
        }

    }

    public void builder(Context context,PluginCall call){
        // Create a file upload request builder
        try {
            String filePath = call.getString("filePath");
            String tag = call.getString("tag");
            String metaData = call.getString("metaData");
            Boolean isLibrary = call.getBoolean("isLibrary",false);
            final TruvideoSdkMediaFileUploadRequestBuilder builder = TruvideoSdkMedia.FileUploadRequestBuilder(filePath);
            JSONObject jsonTag = new JSONObject(tag);
            Iterator<String> keys = jsonTag.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonTag.getString(key); // Can be any type: String, Integer, Boolean, etc.
                builder.addTag(key, value);
            }
            builder.setIsLibrary(isLibrary);

            JSONObject jsonMetadata = new JSONObject(metaData);
            Iterator<String> metadataKeys = jsonMetadata.keys();
            while (metadataKeys.hasNext()) {
                String key = metadataKeys.next();
                String value = jsonMetadata.getString(key); // Can be any type: String, Integer, Boolean, etc.
                builder.addMetadata(key, value);
            }
            // Build the request
            builder.build(new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
                @Override
                public void onComplete(TruvideoSdkMediaFileUploadRequest data) {
                    // File upload request created successfully
                    // Send to upload
                    var mainResponse = returnRequest(data);
                    // Upload the file
                    JSObject ret = new JSObject();
                    ret.put("value",mainResponse);
                    call.resolve(ret);
                }

                @Override
                public void onError(@NonNull TruvideoSdkException exception) {
                    call.reject("API_FAILURE", "TruvideoSdkMediaFileUploadRequest", exception);
                    // Handle error creating the file upload request
                }
            });

        }catch (JSONException e){
            call.reject("JSON_ERROR", "JSONException", e);
        }
    }


    public String returnRequestList(List<TruvideoSdkMediaFileUploadRequest> requests) {
        List<Map<String, Object>> list = new ArrayList<>();

        for (TruvideoSdkMediaFileUploadRequest request : requests) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", request.getId());
            map.put("filePath", request.getFilePath());
            map.put("fileType", request.getType());
            map.put("durationMilliseconds", request.getDurationMilliseconds());
            map.put("remoteId", request.getRemoteId());
            map.put("remoteURL", request.getRemoteUrl());
            map.put("transcriptionURL", request.getTranscriptionUrl());
            map.put("transcriptionLength", request.getTranscriptionLength());
            map.put("status", request.getStatus());
            map.put("progress", request.getUploadProgress());
            map.put("tags", request.getTags());
            map.put("metadata", request.getMetadata());
            map.put("errorMessage", request.getErrorMessage());
            map.put("createdAt",DateUtilsKt.toIsoString(request.getCreatedAt()));
            map.put("updatedAt", DateUtilsKt.toIsoString(request.getUpdatedAt()));

            list.add(map);
        }

        return new Gson().toJson(list);
    }

    public String returnRequest(TruvideoSdkMediaFileUploadRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", request.getId());
        map.put("filePath", request.getFilePath());
        map.put("fileType", request.getType());
        map.put("durationMilliseconds", request.getDurationMilliseconds());
        map.put("remoteId", request.getRemoteId());
        map.put("remoteURL", request.getRemoteUrl());
        map.put("transcriptionURL", request.getTranscriptionUrl());
        map.put("transcriptionLength", request.getTranscriptionLength());
        map.put("status", request.getStatus());
        map.put("progress", request.getUploadProgress());
        map.put("tags",request.getTags());
        map.put("metadata",request.getMetadata());
        map.put("errorMessage",request.getErrorMessage());
        map.put("createdAt",DateUtilsKt.toIsoString(request.getCreatedAt()));
        map.put("updatedAt", DateUtilsKt.toIsoString(request.getUpdatedAt()));
        return new Gson().toJson(map);
    }

    public JSObject returnRequestJSON(TruvideoSdkMediaFileUploadRequest request) {
        JSObject map = new JSObject();
        map.put("id", request.getId());
        map.put("filePath", request.getFilePath());
        map.put("fileType", request.getType());
        map.put("durationMilliseconds", request.getDurationMilliseconds());
        map.put("remoteId", request.getRemoteId());
        map.put("remoteURL", request.getRemoteUrl());
        map.put("transcriptionURL", request.getTranscriptionUrl());
        map.put("transcriptionLength", request.getTranscriptionLength());
        map.put("status", request.getStatus());
        map.put("progress", request.getUploadProgress());
        map.put("tags",request.getTags());
        map.put("metadata",request.getMetadata());
        map.put("errorMessage",request.getErrorMessage());
        map.put("createdAt",DateUtilsKt.toIsoString(request.getCreatedAt()));
        map.put("updatedAt", DateUtilsKt.toIsoString(request.getUpdatedAt()));
        return map;
    }
    @PluginMethod
    public void uploadMedia(PluginCall call) {
        uploadFile(getContext(),call);
    }

    private void uploadFile(Context context, PluginCall call) {

        String id = call.getString("id");
        if (id == null || id.isEmpty()) {
            call.reject("ID_MISSING", "Upload ID is required.");
            return;
        }

        TruvideoSdkMedia.getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.upload(
                        new TruvideoSdkMediaCallback<Unit>() {
                            @Override
                            public void onComplete(Unit unit) {
                                // Upload started successfully
                            }

                            @Override
                            public void onError(@NonNull TruvideoSdkException exception) {
                                call.reject("UPLOAD_FAILED", "Upload failed to start", exception);
                            }
                        },
                        new TruvideoSdkMediaFileUploadCallback() {
                            @Override
                            public void onError(@NonNull String id, @NonNull TruvideoSdkException ex) {
                                JSObject ret = new JSObject();
                                ret.put("id", id);
                                ret.put("error", new Gson().toJson(ex));
                                sendEvent("onError", ret);
                            }

                            @Override
                            public void onProgressChanged(@NonNull String id, float progress) {
                                JSObject ret = new JSObject();
                                ret.put("id", id);
                                ret.put("progress", progress * 100);
                                sendEvent("onProgress", ret);
                            }

                            @Override
                            public void onComplete(@NonNull String id, @NonNull TruvideoSdkMediaFileUploadRequest response) {
                                JSObject ret = new JSObject();
                                ret.put("id", id);
                                ret.put("createdDate", DateUtilsKt.toIsoString(response.getCreatedAt()));
                                ret.put("remoteId", response.getRemoteId());
                                ret.put("uploadedFileURL", response.getRemoteUrl());
                                ret.put("metaData", response.getMetadata().toJson());
                                ret.put("tags", request.getTags().toJson());
                                ret.put("transcriptionURL", response.getTranscriptionUrl());
                                ret.put("transcriptionLength", response.getTranscriptionLength());
                                ret.put("fileType", response.getType().name());

                                call.resolve(ret);
                                sendEvent("onComplete", ret);
                            }
                        }
                );
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK_EXCEPTION", "Failed to fetch upload request by ID", e);
            }
        });
    }

    public void sendEvent(String event, JSObject object) {
        notifyListeners(event,object);
    }

}
