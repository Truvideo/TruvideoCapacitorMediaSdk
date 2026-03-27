package com.truvideo.media;

import com.truvideo.sdk.media.TruvideoSdkMedia;
import android.content.Context;
import android.util.Log;

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
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileType;
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadRequest;
//import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadStatus;
//import com.truvideo.sdk.media.model.external.TruvideoSdkMediaPaginatedResponse;
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadRequestStatus;
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaPagedResult;
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaResponse;
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaMetadata;
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaTags;
import com.truvideo.sdk.media.util.DateUtilsKt;


import com.truvideo.sdk.media.model.external.TruvideoSdkMediaModel;
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaUploadRequest;


import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Method;
import kotlin.Unit;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
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
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id,new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>(){
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
                KotlinBridgeKt.streamRequest( null, new ReturnData() {
                    @Override
                    public void returnData(@NotNull List<@NotNull TruvideoSdkMediaFileUploadRequest> data) {
                        if (data != null) {
                                    JSObject request = new JSObject();
                                    request.put("requests",returnRequestList(data));
                                    sendEvent("AllStream",request);
                                }
                    }
                });
            } else {
                TruvideoSdkMediaFileUploadRequestStatus mainStatus = null;
                switch (status) {
                    case "UPLOADING":
                        mainStatus = TruvideoSdkMediaFileUploadRequestStatus.UPLOADING;
                        break;
                    case "IDLE":
                        mainStatus = TruvideoSdkMediaFileUploadRequestStatus.IDLE;
                        break;
                    case "ERROR":
                        mainStatus = TruvideoSdkMediaFileUploadRequestStatus.ERROR;
                        break;
                    case "PAUSED":
                        mainStatus = TruvideoSdkMediaFileUploadRequestStatus.PAUSED;
                        break;
                    case "COMPLETED":
                        mainStatus = TruvideoSdkMediaFileUploadRequestStatus.COMPLETED;
                        break;
                    case "CANCELED":
                        mainStatus = TruvideoSdkMediaFileUploadRequestStatus.CANCELED;
                        break;
                    case "SYNCHRONIZING":
                        mainStatus = TruvideoSdkMediaFileUploadRequestStatus.SYNCHRONIZING;
                        break;
                }
                KotlinBridgeKt.streamRequest(mainStatus, new ReturnData() {
                    @Override
                    public void returnData(@NotNull List<@NotNull TruvideoSdkMediaFileUploadRequest> data) {
                        if (data != null) {
                            JSObject request = new JSObject();
                            request.put("requests",returnRequestList(data));
                            sendEvent("AllStream",request);
                        }
                    }
                });
            }
        } catch (Exception e) {
            call.reject("GET_REQUESTS_ERROR", e);
        }

    }
    @PluginMethod
    public void stopAllFileUploadRequests(){
//        if(allData == null){
//            return;
//        }
//        allData.removeObservers(getActivity());
//        allData = null;
        KotlinBridgeKt.stopListner();
    }

    LiveData<TruvideoSdkMediaFileUploadRequest> singleData = null;

    @PluginMethod
    public void stopFileUploadRequestById(){
        if(singleData == null){
            return;
        }
        singleData.removeObservers(getActivity());
        singleData = null;
    }
    private Job singleJob;
    @PluginMethod
    public void streamFileUploadRequestById(PluginCall call){
        String id = call.getString("id");
        if(singleData != null){
            singleData.removeObservers(getActivity());
            singleData = null;
        }
        if(id == null){
            return;
        }
        TruvideoSdkMedia.getInstance().streamFileUploadRequestById(id, new TruvideoSdkMediaCallback<Flow<TruvideoSdkMediaFileUploadRequest>>() {
            @Override
            public void onComplete(Flow<TruvideoSdkMediaFileUploadRequest> truvideoSdkMediaFileUploadRequestFlow) {
//                singleData = truvideoSdkMediaFileUploadRequestLiveData;
//                singleData.observe(getActivity(), new Observer<TruvideoSdkMediaFileUploadRequest>() {
//                    @Override
//                    public void onChanged(TruvideoSdkMediaFileUploadRequest request) {
//                        var mainResponse = returnRequest(request);
//                        // Upload the file
//                        JSObject ret = new JSObject();
//                        ret.put("request",mainResponse);
//                        sendEvent("stream",ret);
//                    }
//                });

                // Cancel previous listener
                if (singleJob != null) {
                    singleJob.cancel(null);
                }

                singleJob = KotlinBridgeKt.collectUploadFlow(truvideoSdkMediaFileUploadRequestFlow, request -> {
                    Object mainResponse = returnRequest(request);

                    JSObject ret = new JSObject();
                    ret.put("request", mainResponse);

                    sendEvent("stream", ret);
                    return kotlin.Unit.INSTANCE;
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
                    TruvideoSdkMedia.getInstance().getAllFileUploadRequests(null, new TruvideoSdkMediaCallback<List<TruvideoSdkMediaFileUploadRequest>>() {
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
                    TruvideoSdkMediaFileUploadRequestStatus mainStatus = null;
                    switch (status) {
                        case "UPLOADING":
                            mainStatus = TruvideoSdkMediaFileUploadRequestStatus.UPLOADING;
                            break;
                        case "IDLE":
                            mainStatus = TruvideoSdkMediaFileUploadRequestStatus.IDLE;
                            break;
                        case "ERROR":
                            mainStatus = TruvideoSdkMediaFileUploadRequestStatus.ERROR;
                            break;
                        case "PAUSED":
                            mainStatus = TruvideoSdkMediaFileUploadRequestStatus.PAUSED;
                            break;
                        case "COMPLETED":
                            mainStatus = TruvideoSdkMediaFileUploadRequestStatus.COMPLETED;
                            break;
                        case "CANCELED":
                            mainStatus = TruvideoSdkMediaFileUploadRequestStatus.CANCELED;
                            break;
                        case "SYNCHRONIZING":
                            mainStatus = TruvideoSdkMediaFileUploadRequestStatus.SYNCHRONIZING;
                            break;
                    }

                    TruvideoSdkMedia.getInstance().getAllFileUploadRequests(mainStatus, new TruvideoSdkMediaCallback<List<TruvideoSdkMediaFileUploadRequest>>()  {
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
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
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
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
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
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
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
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
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
    public void createStreamUploadRequest(PluginCall call) {
        call.reject(
                "Stream upload requests are created by the recording SDK, not by this method. Use getAllStreamUploadRequests() to list pending requests after recording.",
                "NOT_SUPPORTED"
        );
    }

    @PluginMethod
    public void getAllStreamUploadRequests(PluginCall call) {
        try {
            TruvideoSdkMedia.getInstance().getAllUploadRequests(new TruvideoSdkMediaCallback<List<TruvideoSdkMediaUploadRequest>>() {
                @Override
                public void onComplete(List<TruvideoSdkMediaUploadRequest> requests) {
                    JSObject ret = new JSObject();
                    ret.put("requests", new Gson().toJson(requests != null ? requests : new ArrayList<>()));
                    call.resolve(ret);
                }

                @Override
                public void onError(@NonNull TruvideoSdkException e) {
                    call.reject("TruvideoSdkException", e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            call.reject("Exception", e.getMessage(), e);
        }
    }

    @PluginMethod
    public void getStreamUploadRequestById(PluginCall call) {
        String id = call.getString("id");
        Long longId = id != null ? safeLong(id) : null;
        if (longId == null) {
            call.reject("Stream upload request ID must be a valid numeric (Long) value", "INVALID_ID");
            return;
        }

        getUploadRequestById(longId, new TruvideoSdkMediaCallback<TruvideoSdkMediaUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaUploadRequest request) {
                JSObject ret = new JSObject();
                ret.put("request", request == null ? "{}" : new Gson().toJson(request));
                call.resolve(ret);
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("TruvideoSdkException", e.getMessage(), e);
            }
        }, call);
    }

    @PluginMethod
    public void uploadStreamUploadRequest(PluginCall call) {
        String id = call.getString("id");
        Long longId = id != null ? safeLong(id) : null;
        if (longId == null) {
            call.reject("Stream upload request ID must be a valid numeric (Long) value", "INVALID_ID");
            return;
        }

        String title = call.getString("title", "");
        String tags = call.getString("tags", "{}");
        String metadata = call.getString("metadata", "{}");
        boolean includeInReport = call.getBoolean("includeInReport", false);
        boolean isLibrary = call.getBoolean("isLibrary", false);

        getUploadRequestById(longId, new TruvideoSdkMediaCallback<TruvideoSdkMediaUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaUploadRequest request) {
                if (request == null) {
                    call.reject("Stream upload request not found for id: " + id, "NOT_FOUND");
                    return;
                }
                try {
                    invokeUpload(request, title, tags, metadata, includeInReport, isLibrary);
                    JSObject ret = new JSObject();
                    ret.put("request", new Gson().toJson(request));
                    call.resolve(ret);
                } catch (Exception e) {
                    call.reject("Exception", e.getMessage(), e);
                }
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("TruvideoSdkException", e.getMessage(), e);
            }
        }, call);
    }

    @PluginMethod
    public void pauseStreamUploadRequest(PluginCall call) {
        handleStreamAction(call, "pause");
    }

    @PluginMethod
    public void resumeStreamUploadRequest(PluginCall call) {
        handleStreamAction(call, "resume");
    }

    @PluginMethod
    public void retryStreamUploadRequest(PluginCall call) {
        handleStreamAction(call, "retry");
    }

    @PluginMethod
    public void deleteStreamUploadRequest(PluginCall call) {
        handleStreamAction(call, "delete");
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
                typeData = null;
            } else if ("Video".equals(type)) {
                typeData = TruvideoSdkMediaFileType.VIDEO;
            } else if ("AUDIO".equals(type)) {
                typeData = TruvideoSdkMediaFileType.AUDIO;
            } else if ("PDF".equals(type)) {
                typeData = TruvideoSdkMediaFileType.DOCUMENT;
            } else {
                typeData = TruvideoSdkMediaFileType.IMAGE;
            }

            JSONObject jsonTag = new JSONObject(tag);
            ArrayList<TruvideoSdkMediaTags.Entry> entriesList = new ArrayList<>();
            Iterator<String> keys = jsonTag.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonTag.getString(key);
                entriesList.add(new TruvideoSdkMediaTags.Entry(key,value));
            }

            TruvideoSdkMedia.getInstance().search(
                    new TruvideoSdkMediaTags(entriesList),
                    typeData,
                    isLibrary,
                    Integer.parseInt(page),
                    Integer.parseInt(pageSize),
                    new TruvideoSdkMediaCallback<TruvideoSdkMediaResponse<TruvideoSdkMediaPagedResult>>() {
                        @Override
                        public void onComplete(TruvideoSdkMediaResponse<TruvideoSdkMediaPagedResult> truvideoSdkMediaPagedResultTruvideoSdkMediaResponse) {
                            Gson gson = new Gson();
                            ArrayList<String> list = new ArrayList<>();
                            TruvideoSdkMediaPagedResult response = truvideoSdkMediaPagedResultTruvideoSdkMediaResponse.getData();

                            for (TruvideoSdkMediaModel it : response.getItems()) {
                                Map<String, Object> mainResponse = new HashMap<>();
                                mainResponse.put("id", it.getId());
                                mainResponse.put("createdDate",DateUtilsKt.toIsoString(it.getCreatedAt()));
                                mainResponse.put("remoteId", it.getId());
                                mainResponse.put("uploadedFileURL", it.getUrl());
                                mainResponse.put("metaData", it.getMetadata().toJsonObject().toString());
                                mainResponse.put("tags", it.getTags().toJsonObject().toString());
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
                            jet.put("numberOfElements","0");
                            jet.put("size",response.getPageSize());
                            jet.put("number","0");
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

    private void handleStreamAction(PluginCall call, String action) {
        String id = call.getString("id");
        Long longId = id != null ? safeLong(id) : null;
        if (longId == null) {
            call.reject("Stream upload request ID must be a valid numeric (Long) value", "INVALID_ID");
            return;
        }

        getUploadRequestById(longId, new TruvideoSdkMediaCallback<TruvideoSdkMediaUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaUploadRequest request) {
                if (request == null) {
                    call.reject("Stream upload request not found for id: " + id, "NOT_FOUND");
                    return;
                }
                try {
                    invokeNoArgAction(request, action);
                    JSObject ret = new JSObject();
                    ret.put("request", new Gson().toJson(request));
                    call.resolve(ret);
                } catch (Exception e) {
                    call.reject("Exception", e.getMessage(), e);
                }
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("TruvideoSdkException", e.getMessage(), e);
            }
        }, call);
    }

    private void getUploadRequestById(
            long id,
            TruvideoSdkMediaCallback<TruvideoSdkMediaUploadRequest> callback,
            PluginCall call
    ) {
        try {
            TruvideoSdkMedia.getInstance().getUploadRequestById(id, callback);
        } catch (Exception e) {
            call.reject("Exception", e.getMessage(), e);
        }
    }

    private Long safeLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void invokeNoArgAction(TruvideoSdkMediaUploadRequest request, String action) throws Exception {
        Method method = request.getClass().getMethod(action);
        method.invoke(request);
    }

    private void invokeUpload(
            TruvideoSdkMediaUploadRequest request,
            String title,
            String tags,
            String metadata,
            boolean includeInReport,
            boolean isLibrary
    ) throws Exception {
        TruvideoSdkMediaTags tagsObj = buildTagsFromJson(tags);
        TruvideoSdkMediaMetadata metadataObj = buildMetadataFromJson(metadata);

        Method[] methods = request.getClass().getMethods();
        for (Method method : methods) {
            if (!"upload".equals(method.getName())) continue;
            Class<?>[] types = method.getParameterTypes();
            if (types.length != 5) continue;

            Object[] args = new Object[5];
            int boolIndex = 0;
            for (int i = 0; i < types.length; i++) {
                Class<?> type = types[i];
                if (type == String.class) {
                    args[i] = title;
                } else if (type.getName().equals(TruvideoSdkMediaTags.class.getName())) {
                    args[i] = tagsObj;
                } else if (type.getName().equals(TruvideoSdkMediaMetadata.class.getName())) {
                    args[i] = metadataObj;
                } else if (type == boolean.class || type == Boolean.class) {
                    args[i] = (boolIndex++ == 0) ? includeInReport : isLibrary;
                } else {
                    args[i] = null;
                }
            }
            method.invoke(request, args);
            return;
        }

        throw new NoSuchMethodException("upload method signature not found on TruvideoSdkMediaUploadRequest");
    }

    private TruvideoSdkMediaTags buildTagsFromJson(String tags) throws JSONException {
        JSONObject jsonTag = new JSONObject(tags == null || tags.isEmpty() ? "{}" : tags);
        ArrayList<TruvideoSdkMediaTags.Entry> entries = new ArrayList<>();
        Iterator<String> keys = jsonTag.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            entries.add(new TruvideoSdkMediaTags.Entry(key, jsonTag.optString(key, "")));
        }
        return new TruvideoSdkMediaTags(entries);
    }

    private TruvideoSdkMediaMetadata buildMetadataFromJson(String metadata) throws JSONException {
        JSONObject jsonMetadata = new JSONObject(metadata == null || metadata.isEmpty() ? "{}" : metadata);
        ArrayList<TruvideoSdkMediaMetadata.Entry> entries = new ArrayList<>();
        Iterator<String> keys = jsonMetadata.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            entries.add(new TruvideoSdkMediaMetadata.Entry.StringEntry(key, jsonMetadata.optString(key, "")));
        }
        return new TruvideoSdkMediaMetadata(entries);
    }

    public void builder(Context context,PluginCall call){
        // Create a file upload request builder
        try {
            String filePath = call.getString("filePath");
            String tag = call.getString("tag");
            String metaData = call.getString("metaData");
            Boolean isLibrary = call.getBoolean("isLibrary",false);
            final TruvideoSdkMediaFileUploadRequestBuilder builder = TruvideoSdkMedia.getInstance().FileUploadRequestBuilder(filePath);
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
            map.put("fileType", request.getFileType());
            map.put("durationMilliseconds", request.getDurationMilliseconds());
            map.put("remoteId", request.getMediaId());
            map.put("remoteURL", request.getMediaUrl());
            map.put("transcriptionURL", request.getTranscriptionUrl());
            map.put("transcriptionLength", "0");
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
        map.put("fileType", request.getFileType());
        map.put("durationMilliseconds", request.getDurationMilliseconds());
        map.put("remoteId", request.getMediaId());
        map.put("remoteURL", request.getMediaUrl());
        map.put("transcriptionURL", request.getTranscriptionUrl());
        map.put("transcriptionLength", "0");
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
        map.put("fileType", request.getFileType());
        map.put("durationMilliseconds", request.getDurationMilliseconds());
        map.put("remoteId", request.getMediaId());
        map.put("remoteURL", request.getMediaUrl());
        map.put("transcriptionURL", request.getTranscriptionUrl());
        map.put("transcriptionLength", "0");
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

        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
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
                                ret.put("remoteId", response.getMediaId());
                                ret.put("uploadedFileURL", response.getMediaUrl());
                                ret.put("metaData", response.getMetadata().toJsonObject().toString());
                                ret.put("tags", request.getTags().toJsonObject().toString());
                                ret.put("transcriptionURL", response.getTranscriptionUrl());
                                ret.put("transcriptionLength", "0");
                                ret.put("fileType", response.getFileType().name());

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
