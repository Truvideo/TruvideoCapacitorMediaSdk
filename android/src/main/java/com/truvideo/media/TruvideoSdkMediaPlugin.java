package com.truvideo.media;

import com.truvideo.sdk.media.TruvideoSdkMedia;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
import kotlinx.coroutines.Job;
import kotlinx.coroutines.flow.Flow;
import truvideo.sdk.common.exceptions.TruvideoSdkException;

@CapacitorPlugin(name = "TruvideoSdkMedia")
public class TruvideoSdkMediaPlugin extends Plugin {

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");
        JSObject ret = new JSObject();
        call.resolve(ret);
    }

    @PluginMethod
    public void mediaBuilder(PluginCall call) {
        builder(getContext(), call);
    }

    @PluginMethod
    public void getFileUploadRequestById(PluginCall call) {
        String id = call.getString("id");
        if (id == null) {
            call.reject("MISSING_ID", "id is required");
            return;
        }
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest truvideoSdkMediaFileUploadRequest) {
                var mainResponse = returnRequest(truvideoSdkMediaFileUploadRequest);
                JSObject ret = new JSObject();
                ret.put("request", mainResponse);
                call.resolve(ret);
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception", "TruvideoSdkException", e);
            }
        });
    }

    // ─── streamAllFileUploadRequests ──────────────────────────────────────────
    // FIX: Added call.setKeepAlive(true) so Capacitor doesn't GC the call before
    //      events fire, and added call.resolve() so the Promise settles on the
    //      TypeScript side instead of hanging forever.
    @PluginMethod
    public void streamAllFileUploadRequests(PluginCall call) {
        String status = call.getString("status");

        // Keep the call alive so events can keep firing after resolve()
        call.setKeepAlive(true);

        try {
            TruvideoSdkMediaFileUploadRequestStatus mainStatus = parseStatus(status);

            KotlinBridgeKt.streamRequest(mainStatus, new ReturnData() {
                @Override
                public void returnData(@NotNull List<@NotNull TruvideoSdkMediaFileUploadRequest> data) {
                    if (data != null) {
                        JSObject request = new JSObject();
                        request.put("requests", returnRequestList(data));
                        sendEvent("AllStream", request);
                    }
                }
            });

            // ✅ Settle the Promise immediately after the stream is set up
            call.resolve(new JSObject());

        } catch (Exception e) {
            call.reject("GET_REQUESTS_ERROR", e);
        }
    }

    @PluginMethod
    public void streamUploadRequestById(PluginCall call) {
        Object idObj = call.getData() != null ? call.getData().opt("id") : null;
        String id = idObj != null ? String.valueOf(idObj) : null;

        Long longId = null;
        if (idObj instanceof Number) {
            longId = ((Number) idObj).longValue();
        } else if (id != null) {
            longId = safeLong(id);
        }
        if (longId == null) {
            call.reject("Stream upload request ID must be a valid numeric (Long) value", "INVALID_ID");
            return;
        }

        // TS expects first emission to resolve promise while updates continue via event.
        call.setKeepAlive(true);
        final boolean[] resolvedOnce = new boolean[]{false};

        try {
            KotlinBridgeKt.streamUploadRequestById(
                longId,
                new ReturnSingleUploadData() {
                    @Override
                    public void returnUploadData(TruvideoSdkMediaUploadRequest data) {
                        JSObject payload = new JSObject();
                        payload.put("request", data == null ? "{}" : new Gson().toJson(data));
                        sendEvent("UploadRequestByIdStream", payload);

                        if (!resolvedOnce[0]) {
                            resolvedOnce[0] = true;
                            call.resolve(payload);
                        }
                    }
                },
                new ReturnUploadError() {
                    @Override
                    public void returnUploadError(@NotNull String message) {
                        if (!resolvedOnce[0]) {
                            resolvedOnce[0] = true;
                            call.reject("STREAM_UPLOAD_REQUEST_BY_ID_ERROR", message);
                        }
                    }
                }
            );
        } catch (Exception e) {
            call.reject("STREAM_UPLOAD_REQUEST_BY_ID_EXCEPTION", e.getMessage(), e);
        }
    }



    @PluginMethod
    public void streamAllUploadRequests(PluginCall call) {
        // TS expects this to resolve with `{ requests: string }` (first emission),
        // while continuous updates are delivered via the `AllUploadStream` event.
        call.setKeepAlive(true);

        final boolean[] resolvedOnce = new boolean[]{false};

        try {
            KotlinBridgeKt.streamAllUploadRequests(
                new ReturnUploadData() {
                    @Override
                    public void returnUploadData(@NotNull List<@NotNull TruvideoSdkMediaUploadRequest> data) {
                        String jsonString = new Gson().toJson(data != null ? data : new ArrayList<>());

                        JSObject payload = new JSObject();
                        payload.put("requests", jsonString);
                        sendEvent("AllUploadStream", payload);

                        if (!resolvedOnce[0]) {
                            resolvedOnce[0] = true;
                            call.resolve(payload);
                        }
                    }
                },
                new ReturnUploadError() {
                    @Override
                    public void returnUploadError(@NotNull String message) {
                        if (!resolvedOnce[0]) {
                            resolvedOnce[0] = true;
                            call.reject("STREAM_ALL_UPLOAD_REQUESTS_ERROR", message);
                        }
                    }
                }
            );
        } catch (Exception e) {
            call.reject("GET_REQUESTS_ERROR", e);
        }
    }

    @PluginMethod
    public void stopAllUploadRequests(PluginCall call) {
        KotlinBridgeKt.stopUploadRequestsListener();
        call.resolve();
    }

    @PluginMethod
    public void stopUploadRequestById(PluginCall call) {
        KotlinBridgeKt.stopUploadRequestByIdListener();
        call.resolve();
    }

    @PluginMethod
    public void stopAllFileUploadRequests(PluginCall call) {
        KotlinBridgeKt.stopListner();
        call.resolve();
    }

    // ─── streamFileUploadRequestById ─────────────────────────────────────────
    // FIX 1: Removed dead LiveData reference (singleData) that was left over
    //         from an older LiveData implementation and was never actually used.
    // FIX 2: Added call.setKeepAlive(true) so Capacitor doesn't GC the call.
    // FIX 3: Added call.resolve() inside onComplete so the Promise settles.
    // FIX 4: Added call.reject() inside onError which was previously empty.
    private Job singleJob;

    @PluginMethod
    public void stopFileUploadRequestById(PluginCall call) {
        if (singleJob != null) {
            singleJob.cancel(null);
            singleJob = null;
        }
        call.resolve();
    }

    @PluginMethod
    public void streamFileUploadRequestById(PluginCall call) {
        String id = call.getString("id");

        // Cancel any previous single-stream job
        if (singleJob != null) {
            singleJob.cancel(null);
            singleJob = null;
        }

        if (id == null) {
            call.reject("MISSING_ID", "id is required");
            return;
        }

        // Keep the call alive so events can keep firing after resolve()
        call.setKeepAlive(true);

        TruvideoSdkMedia.getInstance().streamFileUploadRequestById(id,
            new TruvideoSdkMediaCallback<Flow<TruvideoSdkMediaFileUploadRequest>>() {
                @Override
                public void onComplete(Flow<TruvideoSdkMediaFileUploadRequest> flow) {
                    singleJob = KotlinBridgeKt.collectUploadFlow(flow, request -> {
                        JSObject ret = new JSObject();
                        ret.put("request", returnRequest(request));
                        sendEvent("stream", ret);
                        return kotlin.Unit.INSTANCE;
                    });

                    // ✅ Settle the Promise immediately after the stream is set up
                    call.resolve(new JSObject());
                }

                @Override
                public void onError(@NonNull TruvideoSdkException e) {
                    // ✅ Was previously empty — rejection was silently swallowed
                    call.reject("SDK Exception", "TruvideoSdkException", e);
                }
            });
    }

    @PluginMethod
    public void getAllFileUploadRequests(PluginCall call) {
        String status = call.getString("status");
        try {
            TruvideoSdkMediaFileUploadRequestStatus mainStatus = parseStatus(status);

            TruvideoSdkMedia.getInstance().getAllFileUploadRequests(mainStatus, new TruvideoSdkMediaCallback<List<TruvideoSdkMediaFileUploadRequest>>() {
                @Override
                public void onComplete(List<TruvideoSdkMediaFileUploadRequest> truvideoSdkMediaFileUploadRequests) {
                    JSObject request = new JSObject();
                    request.put("requests", returnRequestList(truvideoSdkMediaFileUploadRequests));
                    call.resolve(request);
                }

                @Override
                public void onError(@NonNull TruvideoSdkException e) {
                    call.reject("SDK Exception", "TruvideoSdkException", e);
                }
            });
        } catch (Exception e) {
            call.reject("GET_REQUESTS_ERROR", e);
        }
    }

    @PluginMethod
    public void cancelMedia(PluginCall call) {
        String id = call.getString("id");
        if (id == null) {
            call.reject("MISSING_ID", "id is required");
            return;
        }
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.cancel(new TruvideoSdkMediaCallback<Unit>() {
                    @Override
                    public void onComplete(Unit unit) {
                        JSObject jsObject = new JSObject();
                        jsObject.put("message", "Cancel Success");
                        call.resolve(jsObject);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception", "TruvideoSdkException", e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception", "TruvideoSdkException", e);
            }
        });
    }

    @PluginMethod
    public void deleteMedia(PluginCall call) {
        String id = call.getString("id");
        if (id == null) {
            call.reject("MISSING_ID", "id is required");
            return;
        }
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.delete(new TruvideoSdkMediaCallback<Unit>() {
                    @Override
                    public void onComplete(Unit unit) {
                        JSObject jsObject = new JSObject();
                        jsObject.put("message", "Delete Success");
                        call.resolve(jsObject);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception", "TruvideoSdkException", e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception", "TruvideoSdkException", e);
            }
        });
    }

    @PluginMethod
    public void pauseMedia(PluginCall call) {
        String id = call.getString("id");
        if (id == null) {
            call.reject("MISSING_ID", "id is required");
            return;
        }
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.pause(new TruvideoSdkMediaCallback<Unit>() {
                    @Override
                    public void onComplete(Unit unit) {
                        JSObject jsObject = new JSObject();
                        jsObject.put("message", "Pause Success");
                        call.resolve(jsObject);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception", "TruvideoSdkException", e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception", "TruvideoSdkException", e);
            }
        });
    }

    @PluginMethod
    public void resumeMedia(PluginCall call) {
        String id = call.getString("id");
        if (id == null) {
            call.reject("MISSING_ID", "id is required");
            return;
        }
        TruvideoSdkMedia.getInstance().getFileUploadRequestById(id, new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaFileUploadRequest request) {
                request.resume(new TruvideoSdkMediaCallback<Unit>() {
                    @Override
                    public void onComplete(Unit unit) {
                        JSObject jsObject = new JSObject();
                        jsObject.put("message", "Resume Success");
                        call.resolve(jsObject);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("SDK Exception", "TruvideoSdkException", e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("SDK Exception", "TruvideoSdkException", e);
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
    public void getAllUploadRequests(PluginCall call) {
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
    public void getUploadRequestById(PluginCall call) {
        Object idObj = call.getData() != null ? call.getData().opt("id") : null;
        String id = idObj != null ? String.valueOf(idObj) : null;

        // Accept both numeric ids and string ids from the JS layer.
        Long longId = null;
        if (idObj instanceof Number) {
            longId = ((Number) idObj).longValue();
        } else if (id != null) {
            longId = safeLong(id);
        }
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
        Object idObj = call.getData() != null ? call.getData().opt("id") : null;
        String id = idObj != null ? String.valueOf(idObj) : null;

        // Accept both numeric ids and string ids from the JS layer.
        Long longId = null;
        if (idObj instanceof Number) {
            longId = ((Number) idObj).longValue();
        } else if (id != null) {
            longId = safeLong(id);
        }
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
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    try {
                        TruvideoSdkMediaTags tagsData = buildTagsFromJson(tags);
                        TruvideoSdkMediaMetadata metadataData = buildMetadataFromJson(metadata);

                        request.upload(
                            title,
                            tagsData,
                            metadataData,
                            includeInReport,
                            isLibrary,
                            new TruvideoSdkMediaCallback<Boolean>() {
                                @Override
                                public void onComplete(Boolean data) {
                                    // Upload request accepted by SDK.
                                }

                                @Override
                                public void onError(@NonNull TruvideoSdkException exception) {
                                    JSObject ret = new JSObject();
                                    ret.put("id", id);
                                    ret.put("error", exception.getMessage());
                                    sendEvent("onError", ret);
                                }
                            }
                        );

                        JSObject ret = new JSObject();
                        ret.put("request", new Gson().toJson(request));
                        call.resolve(ret);
                    } catch (Exception e) {
                        call.reject("Exception", e.getMessage(), e);
                    }
                });
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                call.reject("TruvideoSdkException", e.getMessage(), e);
            }
        }, call);
    }

    @PluginMethod
    public void pauseStream(PluginCall call) {
        handleStreamAction(call, "pause");
    }

    @PluginMethod
    public void resumeStream(PluginCall call) {
        handleStreamAction(call, "resume");
    }

    @PluginMethod
    public void retryStream(PluginCall call) {
        handleStreamAction(call, "retry");
    }

    @PluginMethod
    public void deleteStream(PluginCall call) {
        handleStreamAction(call, "delete");
    }

    @PluginMethod
    public void search(PluginCall call) {
        String tag = call.getString("tag");
        String type = call.getString("type");
        String page = call.getString("page");
        String pageSize = call.getString("pageSize");
        Boolean isLibrary = call.getBoolean("isLibrary", false);
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
                entriesList.add(new TruvideoSdkMediaTags.Entry(key, value));
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
                            mainResponse.put("createdDate", DateUtilsKt.toIsoString(it.getCreatedAt()));
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
                        jet.put("response", gson.toJson(list));
                        jet.put("totalPages", response.getTotalPages());
                        jet.put("totalElements", response.getTotalElements());
                        jet.put("numberOfElements", "0");
                        jet.put("size", response.getPageSize());
                        jet.put("number", "0");
                        jet.put("first", response.getFirst());
                        jet.put("empty", response.getEmpty());
                        jet.put("last", response.getLast());
                        call.resolve(jet);
                    }

                    @Override
                    public void onError(@NonNull TruvideoSdkException e) {
                        call.reject("TruvideoSdkException", e);
                    }
                }
            );

        } catch (Exception e) {
            call.reject("SEARCH_ERROR", e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracted from the duplicated switch blocks in streamAllFileUploadRequests
     * and getAllFileUploadRequests to a single reusable helper.
     */
    private TruvideoSdkMediaFileUploadRequestStatus parseStatus(String status) {
        if (status == null || status.isEmpty()) return null;
        switch (status) {
            case "UPLOADING":     return TruvideoSdkMediaFileUploadRequestStatus.UPLOADING;
            case "IDLE":          return TruvideoSdkMediaFileUploadRequestStatus.IDLE;
            case "ERROR":         return TruvideoSdkMediaFileUploadRequestStatus.ERROR;
            case "PAUSED":        return TruvideoSdkMediaFileUploadRequestStatus.PAUSED;
            case "COMPLETED":     return TruvideoSdkMediaFileUploadRequestStatus.COMPLETED;
            case "CANCELED":      return TruvideoSdkMediaFileUploadRequestStatus.CANCELED;
            case "SYNCHRONIZING": return TruvideoSdkMediaFileUploadRequestStatus.SYNCHRONIZING;
            default:              return null;
        }
    }

    private void handleStreamAction(PluginCall call, String action) {
        Object idObj = call.getData() != null ? call.getData().opt("id") : null;
        String id = idObj != null ? String.valueOf(idObj) : null;

        // Accept both numeric ids and string ids from the JS layer.
        Long longId = null;
        if (idObj instanceof Number) {
            longId = ((Number) idObj).longValue();
        } else if (id != null) {
            longId = safeLong(id);
        }
        if (longId == null) {
            call.reject("Stream upload request ID must be a valid numeric (Long) value", "INVALID_ID");
            return;
        }
        getUploadRequestById(longId, new TruvideoSdkMediaCallback<TruvideoSdkMediaUploadRequest>() {
            @Override
            public void onComplete(TruvideoSdkMediaUploadRequest request) {
                try {
                    if (request == null) {
                        JSObject ret = new JSObject();
                        ret.put("request", "{}");
                        call.resolve(ret);
                        return;
                    }

                    String normalizedAction = action == null ? "" : action.trim().toLowerCase();
                    String methodName;
                    switch (normalizedAction) {
                        case "pause":
                        case "pausemedia":
                        case "pause_media":
                            methodName = "pause";
                            break;
                        case "resume":
                        case "resumemedia":
                        case "resume_media":
                            methodName = "resume";
                            break;
                        case "retry":
                        case "retry_media":
                            methodName = "retry";
                            break;
                        case "cancel":
                        case "cancelmedia":
                        case "cancel_media":
                            methodName = "cancel";
                            break;
                        case "delete":
                        case "deletemedia":
                        case "delete_media":
                            methodName = "delete";
                            break;
                        default:
                            methodName = null;
                            break;
                    }

                    if (methodName == null) {
                        JSObject ret = new JSObject();
                        ret.put("request", "{}");
                        call.resolve(ret);
                        return;
                    }

                    // Use reflection so we don't depend on the exact generic signature of the SDK callbacks.
                    Method targetMethod = null;
                    for (Method m : request.getClass().getMethods()) {
                        if (m.getName().equals(methodName) && m.getParameterTypes().length == 1) {
                            targetMethod = m;
                            break;
                        }
                    }

                    if (targetMethod == null) {
                        JSObject ret = new JSObject();
                        ret.put("request", "{}");
                        call.resolve(ret);
                        return;
                    }

                    TruvideoSdkMediaCallback callback = new TruvideoSdkMediaCallback() {
                        @Override
                        public void onComplete(Object unit) {
                            JSObject ret = new JSObject();
                            ret.put("request", new Gson().toJson(request));
                            call.resolve(ret);
                        }

                        @Override
                        public void onError(@NonNull TruvideoSdkException e) {
                            JSObject ret = new JSObject();
                            ret.put("request", "{}");
                            call.resolve(ret);
                        }
                    };

                    targetMethod.invoke(request, callback);
                } catch (Exception e) {
                    JSObject ret = new JSObject();
                    ret.put("request", "{}");
                    call.resolve(ret);
                }
            }

            @Override
            public void onError(@NonNull TruvideoSdkException e) {
                JSObject ret = new JSObject();
                ret.put("request", "{}");
                call.resolve(ret);
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

    public void builder(Context context, PluginCall call) {
        try {
            String filePath = call.getString("filePath");
            String tag = call.getString("tag");
            String metaData = call.getString("metaData");
            Boolean isLibrary = call.getBoolean("isLibrary", false);
            final TruvideoSdkMediaFileUploadRequestBuilder builder = TruvideoSdkMedia.getInstance().FileUploadRequestBuilder(filePath);

            JSONObject jsonTag = new JSONObject(tag);
            Iterator<String> keys = jsonTag.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonTag.getString(key);
                builder.addTag(key, value);
            }
            builder.setIsLibrary(isLibrary);

            JSONObject jsonMetadata = new JSONObject(metaData);
            Iterator<String> metadataKeys = jsonMetadata.keys();
            while (metadataKeys.hasNext()) {
                String key = metadataKeys.next();
                String value = jsonMetadata.getString(key);
                builder.addMetadata(key, value);
            }

            builder.build(new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
                @Override
                public void onComplete(TruvideoSdkMediaFileUploadRequest data) {
                    var mainResponse = returnRequest(data);
                    JSObject ret = new JSObject();
                    ret.put("value", mainResponse);
                    call.resolve(ret);
                }

                @Override
                public void onError(@NonNull TruvideoSdkException exception) {
                    call.reject("API_FAILURE", "TruvideoSdkMediaFileUploadRequest", exception);
                }
            });

        } catch (JSONException e) {
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
            map.put("createdAt", DateUtilsKt.toIsoString(request.getCreatedAt()));
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
        map.put("tags", request.getTags());
        map.put("metadata", request.getMetadata());
        map.put("errorMessage", request.getErrorMessage());
        map.put("createdAt", DateUtilsKt.toIsoString(request.getCreatedAt()));
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
        map.put("tags", request.getTags());
        map.put("metadata", request.getMetadata());
        map.put("errorMessage", request.getErrorMessage());
        map.put("createdAt", DateUtilsKt.toIsoString(request.getCreatedAt()));
        map.put("updatedAt", DateUtilsKt.toIsoString(request.getUpdatedAt()));
        return map;
    }

    @PluginMethod
    public void uploadMedia(PluginCall call) {
        uploadFile(getContext(), call);
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
        notifyListeners(event, object);
    }
}