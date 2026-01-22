package com.truvideo.media

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.truvideo.sdk.media.TruvideoSdkMedia
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadRequest
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadStatus

interface ReturnData{
    fun returnData(data:List<TruvideoSdkMediaFileUploadRequest>)
}

private var uploadLiveData: LiveData<List<TruvideoSdkMediaFileUploadRequest>>? = null
private var uploadObserver: Observer<List<TruvideoSdkMediaFileUploadRequest>>? = null

fun streamRequest(status: TruvideoSdkMediaFileUploadStatus? = null,returnData: ReturnData){
    uploadLiveData = TruvideoSdkMedia.streamAllFileUploadRequests(status)
    uploadObserver = Observer { req ->
        returnData.returnData(req)
    }
    Handler(Looper.getMainLooper()).post {
        uploadLiveData?.observeForever(uploadObserver!!)
    }
}

fun stopListner(){
    if(uploadLiveData != null && uploadObserver != null){
        uploadLiveData?.removeObserver(uploadObserver!!)
    }
    uploadLiveData = null
    uploadObserver = null
}