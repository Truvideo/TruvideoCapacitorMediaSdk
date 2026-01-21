package com.truvideo.media

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.getcapacitor.JSObject
import com.truvideo.sdk.media.TruvideoSdkMedia
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadRequest
import com.truvideo.sdk.media.model.TruvideoSdkMediaFileUploadStatus

interface ReturnData{
    fun returnData(data:List<TruvideoSdkMediaFileUploadRequest>)
}

fun streamRequest(context: AppCompatActivity,status: TruvideoSdkMediaFileUploadStatus? = null,returnData: ReturnData){
    TruvideoSdkMedia
        .streamAllFileUploadRequests(status)
        .observe(context) { requests ->
            // requests: List<TruvideoSdkMediaFileUploadRequest>
            if(requests != null ){
                returnData.returnData(requests)
                Log.d("Upload", "Received ${requests.size} requests")
            }
        }
}
//    try {
//        if (_isStreaming.value) {
//            TruvideoSdkMedia.streamAllFileUploadRequests().asFlow().collectLatest {
//                if (!isActive) return@collectLatest
//
//                _isLoading.value = false
//                _isError.value = false
//                _fileUploadRequests.value = it.toPersistentList()
//            }
//        } else {
//            _fileUploadRequests.value = TruvideoSdkMedia.getAllFileUploadRequests().toPersistentList()
//            if (!isActive) return@launch
//
//            _isLoading.value = false
//            _isError.value = false
//        }
//    } catch (exception: Exception) {
//}