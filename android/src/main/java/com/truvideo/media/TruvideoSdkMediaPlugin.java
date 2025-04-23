package com.truvideo.media;

import static com.truvideo.sdk.media.TruvideoSdkMedia.TruvideoSdkMedia;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.gson.Gson;
import com.truvideo.sdk.media.builder.TruvideoSdkMediaFileUploadRequestBuilder;
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaCallback;
import com.truvideo.sdk.media.interfaces.TruvideoSdkMediaFileUploadCallback;
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadRequest;
import com.truvideo.sdk.media.model.TruvideoSdkMediaMetadata;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import kotlin.Unit;
import truvideo.sdk.common.exceptions.TruvideoSdkException;

@CapacitorPlugin(name = "TruvideoSdkMedia")
public class TruvideoSdkMediaPlugin extends Plugin {

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        //ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void uploadMedia(PluginCall call) {
        uploadFile(getContext(),call);
    }

    private void uploadFile(Context context, PluginCall call) {
        try {
            String filePath = call.getString("filePath");
            String tag = call.getString("tag");
            String metaData = call.getString("metaData");
            final TruvideoSdkMediaFileUploadRequestBuilder builder = TruvideoSdkMedia.FileUploadRequestBuilder(filePath);
            JSONObject jsonTag = new JSONObject(tag);
            Iterator<String> keys = jsonTag.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonTag.getString(key); // Can be any type: String, Integer, Boolean, etc.
                builder.addTag(key,value);
            }

            JSONObject jsonMetadata = new JSONObject(metaData);
            Iterator<String> metadataKeys = jsonMetadata.keys();
            while (metadataKeys.hasNext()) {
                String key = metadataKeys.next();
                String value = jsonMetadata.getString(key); // Can be any type: String, Integer, Boolean, etc.
                builder.addMetadata(key, value);
            }

            builder.build(new TruvideoSdkMediaCallback<TruvideoSdkMediaFileUploadRequest>() {
                @Override
                public void onComplete(TruvideoSdkMediaFileUploadRequest data) {
                    // File upload request created successfully
                    // Send to upload
                    data.upload(new TruvideoSdkMediaCallback<Unit>() {
                                    @Override
                                    public void onComplete(Unit data) {
                                        // Upload started successfully
                                    }
                                    @Override
                                    public void onError(@NonNull TruvideoSdkException exception) {
                                        // Upload started with error
                                        call.reject("API_FAILURE", "upload", exception);
                                    }
                                },
                            new TruvideoSdkMediaFileUploadCallback() {
                                @Override
                                public void onError(@NonNull String id, @NonNull TruvideoSdkException ex) {
                                    JSObject ret = new JSObject();
                                    ret.put("id",id);
                                    ret.put("error",new Gson().toJson(ex));
                                    //promise.resolve(gson.toJson(mainResponse));
                                    sendEvent("onError", ret);
                                }

                                @Override
                                public void onProgressChanged(@NonNull String id, float progress) {
                                    JSObject ret = new JSObject();
                                    ret.put("id",id);
                                    ret.put("progress",new Gson().toJson(progress * 100));
                                    sendEvent("onProgress", ret);
                                }
                                @Override
                                public void onComplete(@NonNull String id, @NonNull TruvideoSdkMediaFileUploadRequest response) {
                                    JSObject ret = new JSObject();
                                    ret.put("id",id);
                                    ret.put("response",new Gson().toJson(response));
                                    call.resolve(ret);
                                    sendEvent("onComplete", ret);
                                }
                            }
                    );
                }

                @Override
                public void onError(@NonNull TruvideoSdkException exception) {
                    call.reject("API_FAILURE", "TruvideoSdkMediaFileUploadRequest", exception);
                    // Handle error creating the file upload request
                }
            });

        } catch (JSONException e) {
            call.reject("JSON_ERROR", "Invalid JSON format", e);
        }
    }

    public void sendEvent(String event, JSObject object) {
        notifyListeners(event,object);
    }

}
