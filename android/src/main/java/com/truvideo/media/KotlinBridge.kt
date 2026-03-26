package com.truvideo.media

import androidx.lifecycle.Observer
import com.truvideo.sdk.media.TruvideoSdkMedia
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadRequest
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadRequestStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

interface ReturnData{
    fun returnData(data:List<TruvideoSdkMediaFileUploadRequest>)
}

private var uploadLiveData: Flow<List<TruvideoSdkMediaFileUploadRequest>>? = null
private var uploadObserver: Observer<List<TruvideoSdkMediaFileUploadRequest>>? = null

private var uploadJob: Job? = null


//fun streamRequest(status: TruvideoSdkMediaFileUploadRequestStatus? = null, returnData: ReturnData){
//    uploadLiveData = TruvideoSdkMedia.streamAllFileUploadRequests(status)
//    uploadObserver = Observer { req ->
//        returnData.returnData(req)
//    }
//    Handler(Looper.getMainLooper()).post {
//
//    }
//}

fun collectUploadFlow(
    flow: Flow<TruvideoSdkMediaFileUploadRequest>,
    onEach: (TruvideoSdkMediaFileUploadRequest) -> Unit
): Job {
    return CoroutineScope(Dispatchers.Main).launch {
        flow.collect {
            onEach(it)
        }
    }
}

fun streamRequest(
    status: TruvideoSdkMediaFileUploadRequestStatus? = null,
    returnData: ReturnData
) {
    uploadJob = CoroutineScope(Dispatchers.Main).launch {
        TruvideoSdkMedia.streamAllFileUploadRequests(status)
            .collect { req ->
                returnData.returnData(req)
            }
    }
}

fun stopListner(){
    uploadJob?.cancel()
    uploadJob = null
//    if(uploadLiveData != null && uploadObserver != null){
//        uploadLiveData?.removeObserver(uploadObserver!!)
//    }
//    uploadLiveData = null
//    uploadObserver = null
}