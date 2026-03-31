package com.truvideo.media

import androidx.lifecycle.Observer
import com.truvideo.sdk.media.TruvideoSdkMedia
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadRequest
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaFileUploadRequestStatus
import com.truvideo.sdk.media.model.external.TruvideoSdkMediaUploadRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

interface ReturnData{
    fun returnData(data:List<TruvideoSdkMediaFileUploadRequest>)
}

interface ReturnUploadData{
    fun returnUploadData(data: List<TruvideoSdkMediaUploadRequest>)
}

interface ReturnUploadError{
    fun returnUploadError(message: String)
}

private var uploadLiveData: Flow<List<TruvideoSdkMediaFileUploadRequest>>? = null
private var uploadObserver: Observer<List<TruvideoSdkMediaFileUploadRequest>>? = null

private var uploadJob: Job? = null

private var uploadRequestsJob: Job? = null


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
        TruvideoSdkMedia.getInstance().streamAllFileUploadRequests(status)
            .collect { req ->
                returnData.returnData(req)
            }
    }
}

fun streamAllUploadRequests(
    returnData: ReturnUploadData,
    onError: ReturnUploadError
) {
    uploadRequestsJob = CoroutineScope(Dispatchers.Main).launch {
        try {
            TruvideoSdkMedia
                .getInstance()
                .streamAllUploadRequests()
                .collect { list ->
                    returnData.returnUploadData(list)
                }
        } catch (e: Exception) {
            onError.returnUploadError("❌ Failed to observe all upload requests")
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

fun stopUploadRequestsListener(){
    uploadRequestsJob?.cancel()
    uploadRequestsJob = null
}
