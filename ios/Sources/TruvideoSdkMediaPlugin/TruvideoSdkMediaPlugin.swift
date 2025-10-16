import Foundation
import Capacitor
import Combine
import TruvideoSdkMedia

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(TruvideoSdkMediaPlugin)
public class TruvideoSdkMediaPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "TruvideoSdkMediaPlugin"
    public let jsName = "TruvideoSdkMedia"
    private var disposeBag = Set<AnyCancellable>()
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "mediaBuilder", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "uploadMedia", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getFileUploadRequestById", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "cancelMedia", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteMedia", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "pauseMedia", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resumeMedia", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "search", returnType: CAPPluginReturnPromise)
        
    ]
    //private let implementation = TruvideoSdkMedia()
    
    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": value
        ])
    }
    
    @objc public func mediaBuilder(_ call: CAPPluginCall) {
        let filePath = call.getString("filePath") ?? ""
        let tag = call.getString("tag") ?? ""
        let metaData = call.getString("metaData") ?? ""
        
        guard let fileURL = URL(string: "file://\(filePath)") else {
            call.reject("INVALID_URL", "The file URL is invalid", nil)
            return
        }
        
        do {
            let builder = try createFileUploadRequestBuilder(fileURL: fileURL, tag: tag, metaData: metaData)
            let request = try builder.build()
            
//            let mainResponse: [String: String] = [
//                "id": request.id.uuidString, // Generate a unique ID for the event
//                "filePath": request.filePath,
//                "fileType": request.fileType.rawValue,
//                "durationMilliseconds":  "\(String(describing: request.durationMilliseconds))",
//                "remoteId" : request.remoteId ?? "",
//                "remoteURL" : request.remoteURL?.absoluteString ?? "",
//                "transcriptionURL" : request.transcriptionURL ?? "",
//                "transcriptionLength" : "\(String(describing: request.transcriptionLength))" ,
//                "status" : "\(request.status.rawValue)",
//                "progress" : "\(request.uploadProgress)"
//            ]
            let jsonData = try JSONSerialization.data(withJSONObject: returnRequest(request), options: [])
            
            
            if let jsonString = String(data: jsonData, encoding: .utf8) {
                print("mainResponse as JSON string: \(jsonString)")
                //call.resolve(jsonString) // Or wherever you need to use this JSON string
                let response: [String: String] = [
                    "value": jsonString,
                ]
                if JSONSerialization.isValidJSONObject(response) {
                    call.resolve(response)
                } else {
                    call.reject("JSON_ERROR", "Response is not serializable")
                }
            } else {
                print("Error: Could not convert JSON data to string.")
                call.reject("UPLOAD_ERROR", "Upload failed")
                // Handle error: e.g., reject(error)
            }
            //try executeUploadRequest(builder: builder, resolve: resolve, reject: reject)
        } catch {
            call.reject("UPLOAD_ERROR", "Upload failed", error)
        }
    }
    
    @objc public func uploadMedia(_ call: CAPPluginCall){
        let id = call.getString("id") ?? ""
        let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
        
        // Print the file upload request for debugging
        //print("fileUploadRequest: ", request.id.uuidString)
        
        // Completion of request
        let completeCancellable = request?.completionHandler
            .receive(on: DispatchQueue.main)
            .sink(receiveCompletion: { receiveCompletion in
                switch receiveCompletion {
                case .finished:
                    print("Upload finished")
                case .failure(let error):
                    // Print any errors that occur during the upload process
                    print("Upload failure:", error)
                    call.reject("UPLOAD_ERROR", "Upload failed", error)
                }
            }, receiveValue: { uploadedResult in
                // Upon successful upload, retrieve the uploaded file URL
                let uploadedFileURL = uploadedResult.uploadedFileURL
                let metadataDict = uploadedResult.metadata
                let tags = uploadedResult.tags
                let transcriptionURL = uploadedResult.transcriptionURL
                let transcriptionLength = uploadedResult.transcriptionLength
                let id = request?.id.uuidString
                print("uploadedResult: ", uploadedResult)
                
                print("tags: " , tags.dictionary)
                print("metaData: " , metadataDict.dictionary)
                // Send completion event
                let dateFormatter = ISO8601DateFormatter()
                
                
                do {
                    let tagJsonData = try JSONSerialization.data(withJSONObject: tags.dictionary, options: [])
                    if let tagJsonString = String(data: tagJsonData, encoding: .utf8) {
                        let mainResponse: [String: String] = [
                            "id": id ?? "", // Generate a unique ID for the event
                            "createdDate" : dateFormatter.string(from: uploadedResult.createdDate),
                            "remoteId" : uploadedResult.remoteId,
                            "uploadedFileURL": uploadedFileURL.absoluteString,
                            "metaData": try self.convertToJsonString(from : metadataDict.dictionary),
                            "tags":  tagJsonString,
                            "transcriptionURL": transcriptionURL?.absoluteString ?? "",
                            "transcriptionLength": "\(transcriptionLength)",
                            "fileType" : uploadedResult.type.rawValue
                        ]
                        let jsonData = try JSONSerialization.data(withJSONObject: mainResponse, options: [])
                        
                        if let jsonString = String(data: jsonData, encoding: .utf8) {
                            print("mainResponse as JSON string: \(jsonString)")
                            call.resolve(["value" : jsonString]) // Or wherever you need to use this JSON string
                            
                            self.sendEvent(withName: "onComplete", body: mainResponse)
                        } else {
                            
                            print("Error: Could not convert JSON data to string.")
                            call.reject("INVALID_JSON", "Error: Could not convert JSON data to string", nil)
                            // Handle error: e.g., reject(error)
                        }
                    }else {
                        call.reject("INVALID_JSON", "Error: Could not convert JSON data to string", nil)
                    }
                    
                }catch{
                    call.reject("INVALID_JSON", "Error: Could not convert JSON data to string", nil)
                }
                
            })
        
        // Store the completion handler in the dispose bag to avoid premature deallocation
        completeCancellable?.store(in: &disposeBag)
        
        // Progress of request
        let progress = request?.progressHandler
            .receive(on: DispatchQueue.main)
            .sink(receiveValue: { progress in
                let mainResponse: [String: String] = [
                    "id": id, // Generate a unique ID for the event
                    "progress": String(format: " %.2f %", progress.percentage * 100)
                ]
                self.sendEvent(withName: "onProgress", body: mainResponse)
            })
        
        // Store the progress handler in the dispose bag to avoid premature deallocation
        progress?.store(in: &disposeBag)
        
        try? request?.upload()
    }
    
    private func createFileUploadRequestBuilder(fileURL: URL, tag: String, metaData: String) throws -> TruvideoSdkMedia.FileUploadRequestBuilder {
        let builder = TruvideoSdkMedia.FileUploadRequestBuilder(fileURL: fileURL)
        
        // Convert tag JSON string to dictionary
        let tagDict = try convertToDictionary(from: tag)
        for (key, value) in tagDict {
            builder.addTag(key, "\(value)")
        }
        
        // Convert metadata JSON string to Metadata type
        let metadataObj = try convertToDictionary(from: metaData)
        for (key, value) in metadataObj {
            builder.addMetadata(key, "\(value)")
        }
        return builder
    }
    
    private func executeUploadRequest(builder: TruvideoSdkMedia.FileUploadRequestBuilder,_ call: CAPPluginCall) throws {
        let request = try builder.build()
        
        // Print the file upload request for debugging
        print("fileUploadRequest: ", request.id.uuidString)
        
        // Completion of request
        let completeCancellable = request.completionHandler
            .receive(on: DispatchQueue.main)
            .sink(receiveCompletion: { receiveCompletion in
                switch receiveCompletion {
                case .finished:
                    print("Upload finished")
                case .failure(let error):
                    // Print any errors that occur during the upload process
                    print("Upload failure:", error)
                    call.reject("UPLOAD_ERROR", "Upload failed", error)
                }
            }, receiveValue: { uploadedResult in
                print("uploadedResult" , uploadedResult);
                // Upon successful upload, retrieve the uploaded file URL
                let uploadedFileURL = uploadedResult.uploadedFileURL
                let transcriptionURL = uploadedResult.transcriptionURL
                
                let transcriptionLength = uploadedResult.transcriptionLength
                let id = request.id.uuidString
                
                // ✅ Convert metadata and tags into JSON-safe dictionaries
                var metadataSafeDict: [String: Any] = [:]
                if let metadata = uploadedResult.metadata as? Encodable {
                    metadataSafeDict = (try? JSONSerialization.jsonObject(with: JSONEncoder().encode(metadata), options: [])) as? [String: Any] ?? [:]
                } else {
                    print("❌ Failed to convert metadata")
                }
                
                var tagsSafeDict: [String: Any] = [:]
                if let tags = uploadedResult.tags as? Encodable {
                    tagsSafeDict = (try? JSONSerialization.jsonObject(with: JSONEncoder().encode(tags), options: [])) as? [String: Any] ?? [:]
                } else {
                    print("❌ Failed to convert tags")
                }
                
                
                // Send completion event
                let responseDict: [String: Any] = [
                    "id": id, // Generate a unique ID for the event
                    "uploadedFileURL": uploadedFileURL.absoluteString,
                    "metaData": metadataSafeDict,
                    "tags": tagsSafeDict,
                    "transcriptionURL": transcriptionURL ?? "",
                    "transcriptionLength": transcriptionLength
                ]
                
                let mainResponse: [String: Any] = [
                    "id": request.id.uuidString,
                    "response": responseDict  // ✅ Converted to a Dictionary
                ]
                
                
                // ✅ Ensure JSON serializability
                if JSONSerialization.isValidJSONObject(mainResponse) {
                    call.resolve(mainResponse)
                    self.sendEvent(withName: "onComplete", body: mainResponse)
                } else {
                    print("❌ JSON Serialization Failed:", mainResponse)
                    call.reject("JSON_ERROR", "Response is not serializable")
                }
            })
        
        // Store the completion handler in the dispose bag to avoid premature deallocation
        completeCancellable.store(in: &disposeBag)
        
        // Progress of request
        let progress = request.progressHandler
            .receive(on: DispatchQueue.main)
            .sink(receiveValue: { progress in
                let mainResponse: [String: Any] = [
                    "id": UUID().uuidString, // Generate a unique ID for the event
                    "progress": String(format: " %.2f %", progress.percentage * 100)
                ]
                self.sendEvent(withName: "onProgress", body: mainResponse)
            })
        
        // Store the progress handler in the dispose bag to avoid premature deallocation
        progress.store(in: &disposeBag)
        
        try request.upload()
    }
    
    private func convertToDictionary(from jsonString: String) throws -> [String: String] {
        guard let jsonData = jsonString.data(using: .utf8) else {
            throw NSError(domain: "Invalid JSON string", code: 0, userInfo: nil)
        }
        
        return try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: String] ?? [:]
    }
    
    //    private func convertToMetadata(from jsonString: String) throws -> Metadata {
    //        guard let jsonData = jsonString.data(using: .utf8) else {
    //            throw NSError(domain: "Invalid JSON string", code: 0, userInfo: nil)
    //        }
    //
    //        guard let metadataDict = try JSONSerialization.jsonObject(with: jsonData, options: []) as? [String: Any] else {
    //            throw NSError(domain: "Invalid JSON format", code: 0, userInfo: nil)
    //        }
    //
    //        return convertToMetadata(metadataDict)
    //    }
    //
    //    private func convertToMetadata(_ dict: [String: Any]) -> Metadata {
    //        var metadata = Metadata()
    //        for (key, value) in dict {
    //            if let metadataValue = convertToMetadataValue(value) {
    //                metadata[key] = metadataValue
    //            }
    //        }
    //        return metadata
    //    }
    //
    //    private func convertToMetadataValue(_ value: Any) -> MetadataValue? {
    //        if value is NSNull {
    //            return nil
    //        } else if let value = value as? String {
    //            return .string(value)
    //        } else if let value = value as? Int {
    //            return .int(value)
    //        } else if let value = value as? Float {
    //            return .float(value)
    //        } else if let value = value as? [Any] {
    //            return .array(value.compactMap { convertToMetadataValue($0) })
    //        } else if let value = value as? [String: Any] {
    //            return .dictionary(convertToMetadata(value))
    //        }
    //        return nil
    //    }
    //
    //    private func convertMetadataToDictionary(_ metadata: Metadata) -> [String: Any] {
    //        var dict = [String: Any]()
    //        for (key, value) in metadata {
    //            dict[key] = convertMetadataValueToAny(value)
    //        }
    //        return dict
    //    }
    //
    //    private func convertMetadataValueToAny(_ value: MetadataValue) -> Any {
    //        switch value {
    //        case .string(let stringValue):
    //            return stringValue
    //        case .int(let intValue):
    //            return intValue
    //        case .float(let floatValue):
    //            return floatValue
    //        case .array(let arrayValue):
    //            return arrayValue.map { convertMetadataValueToAny($0) }
    //        case .dictionary(let dictValue):
    //            return convertMetadataToDictionary(dictValue)
    //        }
    //  }
    
    // Function to send events to React Native
    
    func returnRequest(_ request : TruvideoSdkMediaUploadRequest) -> [String:String]{
        let dateFormatter = ISO8601DateFormatter()
        return [
            "id": request.id.uuidString, // Generate a unique ID for the event
            "filePath": request.filePath,
            "fileType": request.fileType.rawValue,
            "durationMilliseconds":  "\(String(describing: request.durationMilliseconds))",
            "remoteId" : request.remoteId ?? "",
            "remoteURL" : request.remoteURL?.absoluteString ?? "",
            "transcriptionURL" : request.transcriptionURL ?? "",
            "transcriptionLength" : "\(String(describing: request.transcriptionLength))" ,
            "status" : "\(request.status.rawValue)",
            "progress" : "\(request.uploadProgress)",
            "tag" : "\(request.tags.dictionary)",
            "metadata" : "\(request.metadata.dictionary)",
            "createdAt" : request.createdAt != nil ? dateFormatter.string(from: request.createdAt!) : "",
            "updatedAt" : request.updatedAt != nil ? dateFormatter.string(from: request.updatedAt!) : "",
            "errorMessage" : request.errorMessage ?? ""
        ]
    }
    
    
    @objc public func getFileUploadRequestById(_ call : CAPPluginCall){
        let id = call.getString("id") ?? ""
        do {
            let request =  try TruvideoSdkMedia.getFileUploadRequest(withId : id)
//            let mainResponse: [String: String] = [
//                "id": request.id.uuidString, // Generate a unique ID for the event
//                "filePath": request.filePath,
//                "fileType": request.fileType.rawValue,
//                "durationMilliseconds":  "\(String(describing: request.durationMilliseconds))",
//                "remoteId" : request.remoteId ?? "",
//                "remoteURL" : request.remoteURL?.absoluteString ?? "",
//                "transcriptionURL" : request.transcriptionURL ?? "",
//                "transcriptionLength" : "\(String(describing: request.transcriptionLength))" ,
//                "status" : "\(request.status.rawValue)",
//                "progress" : "\(request.uploadProgress)"
//            ]
            let jsonData = try JSONSerialization.data(withJSONObject: returnRequest(request), options: [])
            
            if let jsonString = String(data: jsonData, encoding: .utf8) {
                print("mainResponse as JSON string: \(jsonString)")
                let response: [String: String] = [
                    "request": jsonString,
                ]
                if JSONSerialization.isValidJSONObject(response) {
                    call.resolve(response)
                } else {
                    call.reject("JSON_ERROR", "Response is not serializable")
                }// Or wherever you need to use this JSON string
            } else {
                print("Error: Could not convert JSON data to string.")
                // Handle error: e.g., reject(error)
            }
            //try executeUploadRequest(builder: builder, resolve: resolve, reject: reject)
        } catch {
            let response: [String: String] = [
                "request": "{}",
            ]
            if JSONSerialization.isValidJSONObject(response) {
                call.resolve(response)
            } else {
                call.reject("JSON_ERROR", "Response is not serializable")
            }
        }
        
        //TruvideoSdkMedia.FileUploadRequestBuilder(fileURL: fileURL)
    }
    
    @objc public func getAllFileRequests(status: String,_ call : CAPPluginCall){
        do {
          var statusData : TruvideoSdkMediaUploadRequest.Status?
          if status == "COMPLETED" {
            statusData = .completed
          } else if status == "CANCELED" {
            statusData = .cancelled
          }else if status == "PAUSED" {
            statusData = .paused
          }else if status == "SYNCHRONIZING" {
            statusData = .synchronizing
          }else if status == "IDLE" {
            statusData = .idle
          }else if status == "UPLOADING" {
            statusData = .processing
          }else if status == "ERROR" {
            statusData = .error
          }else {
            statusData = nil
          }
          let requests =  try TruvideoSdkMedia.getFileUploadRequests(byStatus: statusData)
          //let dateFormatter = DateFormatter()
          //let dateFormatter = ISO8601DateFormatter()
          var responseArray: [[String: String]] = []

              for request in requests {
                  //var tagString = ""
                  //let tagJsonData = try JSONSerialization.data(withJSONObject: request.tags.dictionary, options: [])
//                  if let tagJsonString = String(data: tagJsonData, encoding: .utf8) {
//                    tagString = tagJsonString
//                  }

                  //var metadataString = ""
                  //let metadataJsonData = try JSONSerialization.data(withJSONObject: request.metadata.dictionary, options: [])
//                  if let metadataJsonString = String(data: metadataJsonData, encoding: .utf8) {
//                    metadataString = metadataJsonString
//                  }

//                  let mainResponse: [String: String] = [
//                      "id": request.id.uuidString,
//                      "filePath": request.filePath,
//                      "fileType": request.fileType.rawValue,
//                      "createdAt": request.createdAt != nil ? dateFormatter.string(from: request.createdAt!) : "",
//                      "updatedAt": request.updatedAt != nil ? dateFormatter.string(from: request.updatedAt!) : "",
//                      "tags": tagString,
//                      "metadata": metadataString,
//                      "durationMilliseconds": "\(String(describing: request.durationMilliseconds))",
//                      "remoteId": request.remoteId ?? "",
//                      "remoteURL": request.remoteURL?.absoluteString ?? "",
//                      "transcriptionURL": request.transcriptionURL ?? "",
//                      "transcriptionLength": "\(String(describing: request.transcriptionLength))",
//                      "status": "\(request.status.rawValue)",
//                      "progress": "\(request.uploadProgress)"
//                  ]

                  responseArray.append(returnRequest(request))
              }

              let jsonData = try JSONSerialization.data(withJSONObject: responseArray, options: [])
              if let jsonString = String(data: jsonData, encoding: .utf8) {
                  print("responseArray as JSON string: \(jsonString)")
                  let response: [String: String] = [
                      "requests": jsonString,
                  ]
                  if JSONSerialization.isValidJSONObject(response) {
                      call.resolve(response)
                  } else {
                      call.reject("JSON_ERROR", "Response is not serializable")
                  }// Or wherever you need to use this JSON string
                  //call.resolve(jsonString) // return the whole array JSON string
              } else {
                  print("Error: Could not convert JSON data to string.")
                  // reject(error) or handle appropriately
              }

        } catch {
            let response: [String: String] = [
                "requests": "{}",
            ]
            if JSONSerialization.isValidJSONObject(response) {
                call.resolve(response)
            } else {
                call.reject("JSON_ERROR", "Response is not serializable")
            }
        }

        //TruvideoSdkMedia.FileUploadRequestBuilder(fileURL: fileURL)
      }
    @objc public func cancelMedia(_ call : CAPPluginCall){
        let id  = call.getString("id") ?? ""
        let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
        try? request?.cancel()
        let response: [String: String] = [
            "message": "Cancel Success",
        ]
        if JSONSerialization.isValidJSONObject(response) {
            call.resolve(response)
        } else {
            call.reject("JSON_ERROR", "Response is not serializable")
        }
    }
    
    @objc public func deleteMedia(_ call : CAPPluginCall){
        let id  = call.getString("id") ?? ""
        let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
        try? request?.delete()
        let response: [String: String] = [
            "message": "Delete Success",
        ]
        if JSONSerialization.isValidJSONObject(response) {
            call.resolve(response)
        } else {
            call.reject("JSON_ERROR", "Response is not serializable")
        }
    }
    
    @objc public func pauseMedia(_ call : CAPPluginCall){
        let id  = call.getString("id") ?? ""
        let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
        try? request?.pause()
        let response: [String: String] = [
            "message": "Pause Success",
        ]
        if JSONSerialization.isValidJSONObject(response) {
            call.resolve(response)
        } else {
            call.reject("JSON_ERROR", "Response is not serializable")
        }
    }
    
    @objc public func resumeMedia(_ call : CAPPluginCall){
        let id  = call.getString("id") ?? ""
        let request = try? TruvideoSdkMedia.getFileUploadRequest(withId : id)
        try? request?.resume()
        let response: [String: String] = [
            "message": "Resume Success",
        ]
        if JSONSerialization.isValidJSONObject(response) {
            call.resolve(response)
        } else {
            call.reject("JSON_ERROR", "Response is not serializable")
        }
    }
    
    @objc public func search(_ call : CAPPluginCall){
        let tag = call.getString("tag") ?? ""
        let type = call.getString("type") ?? ""
        let page = call.getString("page") ?? ""
        let pageSize = call.getString("pageSize") ?? ""
        let isLibrary = call.getBool("isLibrary") ?? false
        let tagDict = try? convertToDictionary(from: tag)
        let tagBuild = TruvideoSdkMediaTags.builder()
        for (key, value) in tagDict! {
            _ = tagBuild.set(key, "\(value)")
        }
        var typeData : TruvideoSdkMediaType?
        if(type == "Image"){
          typeData = .image
        }else if(type == "Video"){
          typeData = .video
        }else if(type == "Audio"){
          typeData = .audio
        }else if(type == "PDF"){
          typeData = .document
        }else{
          typeData = nil
        }
        Task{
            let request = try? await TruvideoSdkMedia.search(type: typeData,tags: tagBuild.build(), isLibrary: isLibrary, pageNumber: Int(page) ?? 0, size: Int(pageSize) ?? 0)
            let mediaList: [TruvideoSDKMedia]? = request?.content
            if(mediaList == nil){
                let response: [String: String] = [
                    "response": "[]",
                ]
                if JSONSerialization.isValidJSONObject(response) {
                    call.resolve(response)
                } else {
                    call.reject("JSON_ERROR", "Response is not serializable")
                }
            }else{
                var list = [String]()
                let dateFormatter = ISO8601DateFormatter()
                for media in mediaList! {
                    let tagJsonData = try JSONSerialization.data(withJSONObject: media.tags.dictionary, options: [])
                    if let tagJsonString = String(data: tagJsonData, encoding: .utf8) {
                        let mediaDict: [String: String] = [
                            "id": media.remoteId,
                            "createdDate":dateFormatter.string(from: media.createdDate),
                            "remoteId": media.remoteId,
                            "uploadedFileURL": media.uploadedFileURL.absoluteString,
                            "metaData": try self.convertToJsonString(from : media.metadata.dictionary),  // must return [String: Any]
                            "tags": tagJsonString,          // must return [String: Any]
                            "transcriptionURL": media.transcriptionURL?.absoluteString ?? "",
                            "transcriptionLength": "\(media.transcriptionLength)",
                            "fileType": media.type.rawValue,
                            "thumbnailUrl" : media.thumbnailUrl?.absoluteString ?? "",
                            "previewUrl" : media.previewUrl?.absoluteString ?? "",

                        ]
                        let jsonData = try JSONSerialization.data(withJSONObject: mediaDict, options: [])
                        if let jsonString = String(data: jsonData, encoding: .utf8) {
                            list.append(jsonString)
                        }
                    }
                }
                let jsonData = try JSONSerialization.data(withJSONObject: list, options: [])
                if let jsonString = String(data: jsonData, encoding: .utf8) {
                    let response: [String: String] = [
                        "response": jsonString,
                    ]
                    if JSONSerialization.isValidJSONObject(response) {
                        call.resolve(response)
                    } else {
                        call.reject("JSON_ERROR", "Response is not serializable")
                    }
                }else{
                    call.reject("ERROR","JSON_ERROR",nil)
                }
                
            }
            
            
        }
        //try? request?.resume()
    }
    
    private func convertToJsonString(from dictionary: [String: Any]) throws -> String {
        let jsonData = try JSONSerialization.data(withJSONObject: dictionary, options: [])
        
        guard let jsonString = String(data: jsonData, encoding: .utf8) else {
            throw NSError(domain: "Unable to encode JSON string", code: 2, userInfo: nil)
        }
        
        return jsonString
    }
    
    private func sendEvent(withName name: String, body: [String: Any]) {
        //            guard let bridge = RCTBridge.current() else { return }
        //            bridge.eventDispatcher().sendAppEvent(withName: name, body: body)
        self.notifyListeners(name, data: body)
    }
}
