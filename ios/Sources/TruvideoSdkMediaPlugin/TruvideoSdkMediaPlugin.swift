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
        CAPPluginMethod(name: "stopAllFileUploadRequests", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopFileUploadRequestById", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getAllFileUploadRequests", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "streamAllFileUploadRequests", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "streamFileUploadRequestById", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "cancelMedia", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteMedia", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "pauseMedia", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resumeMedia", returnType: CAPPluginReturnPromise),

        CAPPluginMethod(name: "getAllUploadRequests", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getUploadRequestById", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "streamAllUploadRequests", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopAllUploadRequests", returnType: CAPPluginReturnPromise),
        
        CAPPluginMethod(name: "uploadStreamUploadRequest", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "pauseStream", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "resumeStream", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "retryStream", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteStream", returnType: CAPPluginReturnPromise),
        
        CAPPluginMethod(name: "search", returnType: CAPPluginReturnPromise)
       
    ]
    
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
            Task{
                let builder = try createFileUploadRequestBuilder(fileURL: fileURL, tag: tag, metaData: metaData)
                let request = try builder.build()
                let jsonData = try JSONSerialization.data(withJSONObject: await returnRequest(request), options: [])
                
                
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
            }
        
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

    func returnRequest(_ request : TruvideoSdkMediaUploadRequest) async -> [String:String]{
        let dateFormatter = ISO8601DateFormatter()

        let statusString: String
        switch request.status {
        case .idle:
            statusString = "IDLE"
        case .completed:
            statusString = "COMPLETED"
        case .cancelled:
            statusString = "CANCELED"
        case .paused:
            statusString = "PAUSED"
        case .synchronizing:
            statusString = "SYNCHRONIZING"
        case .processing:
            statusString = "UPLOADING"
        case .error:
            statusString = "ERROR"
        @unknown default:
            statusString = "\(request.status.rawValue)"
        }
        
        return [
            "id": request.id.uuidString,
            "filePath": request.filePath,
            "fileType": request.fileType.rawValue,
            "durationMilliseconds":  "\(await String(describing: request.durationMilliseconds))",
            "remoteId" : request.remoteId ?? "",
            "remoteURL" : request.remoteURL?.absoluteString ?? "",
            "transcriptionURL" : request.transcriptionURL ?? "",
            "transcriptionLength" : "\(String(describing: request.transcriptionLength))" ,
            "status" : statusString,
            "statusString": statusString,
            "progress" : "\(request.uploadProgress)",
            "tag" : "\(request.tags.dictionary)",
            "metadata" : "\(request.metadata.dictionary)",
            "createdAt" : request.createdAt != nil ? dateFormatter.string(from: request.createdAt!) : "",
            "updatedAt" : request.updatedAt != nil ? dateFormatter.string(from: request.updatedAt!) : "",
            "errorMessage" : request.errorMessage ?? ""
        ]
    }

    private func buildStreamTagsFromJson(_ jsonString: String) -> [String: String] {
        guard
            let jsonData = jsonString.data(using: .utf8),
            let dict = (try? JSONSerialization.jsonObject(with: jsonData, options: [])) as? [String: Any]
        else {
            return [:]
        }

        var result: [String: String] = [:]
        for (key, value) in dict {
            result[key] = String(describing: value)
        }
        return result
    }

    private func sanitizeMetadataForBuilder(_ value: Any) -> Any {
        if value is NSNull { return "" }

        if let dict = value as? [String: Any] {
            var sanitized: [String: Any] = [:]
            for (k, v) in dict {
                sanitized[k] = sanitizeMetadataForBuilder(v)
            }
            return sanitized
        }

        if let array = value as? [Any] {
            return array.map { sanitizeMetadataForBuilder($0) }
        }

        // Keep JSON-safe primitives, stringify everything else.
        if value is String || value is NSNumber || value is Bool {
            return value
        }

        return String(describing: value)
    }

    private func buildStreamMetadataFromJson(_ jsonString: String) -> TruvideoSdkMediaMetadata {
        guard
            let jsonData = jsonString.data(using: .utf8),
            let dict = (try? JSONSerialization.jsonObject(with: jsonData, options: [])) as? [String: Any]
        else {
            return TruvideoSdkMediaMetadata.builder().build()
        }

        let sanitized = sanitizeMetadataForBuilder(dict) as? [String: Any] ?? [:]
        return TruvideoSdkMediaMetadata.builder(dictionary: sanitized).build()
    }

    private func returnStreamUploadRequest(_ request: TruvideoSdkMediaStreamRequest) -> [String: String] {
        let dateFormatter = ISO8601DateFormatter()
        return [
            "id": request.id.uuidString,
            "status": request.status.rawValue.uppercased(),
            "fileType": request.fileType.rawValue.uppercased(),
            "remoteId": request.remoteId ?? "",
            "tags": "\(request.tags.dictionary)",
            "metaData": "\(request.metadata.dictionary)",
            "includeInReport": "\(request.isIncludedInReport)",
            "isLibrary": "\(request.isLibrary)",
            "createdAt": dateFormatter.string(from: request.createdAt),
        ]
    }

    private func returnFullStreamUploadRequest(_ request: TruvideoSdkMediaStreamRequest) -> [String: Any] {
        let dateFormatter = ISO8601DateFormatter()
        return [
            "id": request.id.uuidString,
            "status": request.status.rawValue.uppercased(),
            "fileType": request.fileType.rawValue.uppercased(),
            "remoteId": request.remoteId ?? "",
            "tags": request.tags.dictionary,
            "tag": request.tags.dictionary,
            "metaData": request.metadata.dictionary,
            "metadata": request.metadata.dictionary,
            "includeInReport": request.isIncludedInReport,
            "isLibrary": request.isLibrary,
            "createdAt": dateFormatter.string(from: request.createdAt),
            // Backward-compatible placeholders expected by existing TS parsing.
            "filePath": "",
            "durationMilliseconds": "0",
            "remoteURL": "",
            "transcriptionURL": "",
            "transcriptionLength": "0",
            "progress": "0",
            "updatedAt": "",
            "errorMessage": ""
        ]
    }
    
    @objc public func getFileUploadRequestById(_ call : CAPPluginCall){
        let id = call.getString("id") ?? ""
        do {
            Task {
                let request =  try TruvideoSdkMedia.getFileUploadRequest(withId : id)
                let jsonData = try JSONSerialization.data(withJSONObject: await returnRequest(request), options: [])
                
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
            }
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
    
    private var uploadRequestsCancellableById: AnyCancellable? = nil
    
    
    @objc public func streamFileUploadRequestById(_ call : CAPPluginCall){
        let id = call.getString("id") ?? ""
        do{
            // Cancel any previous stream before starting a new one.
            uploadRequestsCancellableById?.cancel()
            uploadRequestsCancellableById = nil

            uploadRequestsCancellableById = try TruvideoSdkMedia.streamFileUploadRequest(withId: id)
                .sink { completion in
                    switch completion {
                    case .finished:
                        print("Upload finished")
                    case .failure(let error):
                        self.sendEvent(withName: "onError", body: [
                            "id": id,
                            "error": error.localizedDescription
                        ])
                    }
                } receiveValue: { request in
                    Task{
                        do {
                            let jsonData = try JSONSerialization.data(withJSONObject: await self.returnRequest(request), options: [])
                            
                            if let jsonString = String(data: jsonData, encoding: .utf8) {
                                print("mainResponse as JSON string: \(jsonString)")
                                let response: [String: String] = [
                                    "request": jsonString,
                                ]
                                if JSONSerialization.isValidJSONObject(response) {
                                    DispatchQueue.main.async {
                                        self.sendEvent(withName: "stream", body: response)
                                    }
                                }
                            }
                        }catch{
                            self.sendEvent(withName: "onError", body: [
                                "id": id,
                                "error": error.localizedDescription
                            ])
                        }
                    }
                }

            // Resolve immediately; updates are delivered through "stream" events.
            call.resolve(["message": "Stream subscription started", "id": id])
        }catch{
            call.reject(error.localizedDescription)
        }
    }
     
    private var uploadRequestsCancellable: AnyCancellable? = nil
    private var uploadRequestsTask: Task<Void, Never>? = nil
        
    @objc public func streamAllFileUploadRequests(_ call : CAPPluginCall){
        let status = call.getString("status") ?? ""
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
        
        // Cancel any previous stream before starting a new one.
        uploadRequestsCancellable?.cancel()
        uploadRequestsCancellable = nil

//        var cancellables = Set<AnyCancellable>()
        uploadRequestsCancellable = TruvideoSdkMedia.streamFileUploadRequests(byStatus: statusData) // or provide a status
            .sink { completion in
                switch completion {
                case .finished:
                    print("Upload finished")
                case .failure(let error):
                    self.sendEvent(withName: "onError", body: [
                        "id": "",
                        "error": error.localizedDescription
                    ])
                }
            } receiveValue: { requests in
                // requests is [TruvideoSdkMediaUploadRequest]
                Task{
                    print("Received \(requests.count) upload requests")
                    var responseArray: [[String: String]] = []
                    for request in requests {
                        responseArray.append(await self.returnRequest(request))
                    }
                    do{
                        let jsonData = try JSONSerialization.data(withJSONObject: responseArray, options: [])
                        if let jsonString = String(data: jsonData, encoding: .utf8) {
                            print("responseArray as JSON string: \(jsonString)")
                            let response: [String: String] = [
                                "requests": jsonString,
                            ]
                            if JSONSerialization.isValidJSONObject(response) {
                                self.sendEvent(withName: "AllStream", body: response)
                            }
                        }
                    }catch let error {
                        self.sendEvent(withName: "onError", body: [
                            "id": "",
                            "error": error.localizedDescription
                        ])
                    }
                }
                
            }
            //.store(in: &cancellables)

        // Resolve immediately; updates are delivered through "AllStream" events.
        call.resolve(["message": "All stream subscription started"])
    }


    @objc public func streamAllUploadRequests(_ call : CAPPluginCall) {
        // Cancel any previous stream before starting a new one.
        uploadRequestsTask?.cancel()
        uploadRequestsTask = nil
        uploadRequestsCancellable?.cancel()
        uploadRequestsCancellable = nil

        var resolvedOnce = false
        uploadRequestsTask = Task {
            do {
                let stream = TruvideoSdkMedia.streamAllUploadRequests()

                for try await requests in stream {
                    var responseArray: [[String: String]] = []
                    for request in requests {
                        responseArray.append(self.returnStreamUploadRequest(request))
                    }

                    do {
                        let jsonData = try JSONSerialization.data(withJSONObject: responseArray, options: [])
                        let jsonString = String(data: jsonData, encoding: .utf8) ?? "[]"

                        // Keep existing event contract for callers that listen.
                        self.sendEvent(withName: "AllUploadStream", body: [
                            "requests": jsonString,
                        ])

                        // Settle TS Promise on first emission.
                        if !resolvedOnce {
                            resolvedOnce = true
                            call.resolve(["requests": jsonString])
                        }
                    } catch let error {
                        self.sendEvent(withName: "onError", body: [
                            "id": "",
                            "error": error.localizedDescription
                        ])
                    }
                }
            } catch let error {
                self.sendEvent(withName: "onError", body: [
                    "id": "",
                    "error": error.localizedDescription
                ])
            }
        }
    }

    @objc public func stopAllUploadRequests(_ call : CAPPluginCall){
        uploadRequestsTask?.cancel()
        uploadRequestsTask = nil
        uploadRequestsCancellable?.cancel()
        uploadRequestsCancellable = nil
        call.resolve()
    }
    
    @objc public func stopAllFileUploadRequests(_ call : CAPPluginCall){
        if(uploadRequestsCancellable == nil){
            return
        }
        uploadRequestsCancellable?.cancel()
        uploadRequestsCancellable = nil
    }
    
    @objc public func stopFileUploadRequestById(_ call : CAPPluginCall){
        if(uploadRequestsCancellableById == nil){
            return
        }
        uploadRequestsCancellableById?.cancel()
        uploadRequestsCancellableById = nil
    }
    
    @objc public func getAllFileUploadRequests(_ call : CAPPluginCall){
        do {
            let status = call.getString("status") ?? ""
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
            let req = try TruvideoSdkMedia.getFileUploadRequests()
            let requests = if(status == ""){
                try TruvideoSdkMedia.getFileUploadRequests()
            }else{
                try TruvideoSdkMedia.getFileUploadRequests(byStatus: statusData)
            }
            
          //let dateFormatter = DateFormatter()
          //let dateFormatter = ISO8601DateFormatter()
          var responseArray: [[String: String]] = []
            Task{
                for request in requests {
                
                    responseArray.append(await returnRequest(request))
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

    @objc public func getAllUploadRequests(_ call: CAPPluginCall) {
        Task {
            do {
                let requests = try await TruvideoSdkMedia.getAllUploadRequests()
                var responseArray: [[String: String]] = []
                for request in requests {
                    responseArray.append(returnStreamUploadRequest(request))
                }

                let jsonData = try JSONSerialization.data(withJSONObject: responseArray, options: [])
                let jsonString = String(data: jsonData, encoding: .utf8) ?? "[]"
                call.resolve(["requests": jsonString])
            } catch {
                call.reject("GET_ALL_STREAM_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func getUploadRequestById(_ call: CAPPluginCall) {
        let id = call.getString("id") ?? ""
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                print("==========Request========", request)
                let dict = returnFullStreamUploadRequest(request)
                let jsonData = try JSONSerialization.data(withJSONObject: dict, options: [])
                let jsonString = String(data: jsonData, encoding: .utf8) ?? "{}"
                call.resolve(["request": jsonString])
            } catch {
                // Match Android behavior: return empty object for "not found"
                call.resolve(["request": "{}"])
            }
        }
    }

    @objc public func uploadStreamUploadRequest(_ call: CAPPluginCall) {
        let id = call.getString("id") ?? ""
        let title = call.getString("title") ?? ""
        let tags = call.getString("tags") ?? "{}"
        let metadata = call.getString("metadata") ?? "{}"
        let includeInReport = call.getBool("includeInReport") ?? false
        let isLibrary = call.getBool("isLibrary") ?? false

        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                let options = TruvideoSdkMediaStreamRequest.Options(
                    isIncludedInReport: includeInReport,
                    isLibrary: isLibrary,
                    metadata: buildStreamMetadataFromJson(metadata),
                    tags: buildStreamTagsFromJson(tags),
                    title: title
                )

                try request.upload(with: options)

                let dict = returnStreamUploadRequest(request)
                let jsonData = try JSONSerialization.data(withJSONObject: dict, options: [])
                let jsonString = String(data: jsonData, encoding: .utf8) ?? "{}"
                call.resolve(["request": jsonString])
            } catch {
                call.reject("STREAM_UPLOAD_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func pauseStream(_ call: CAPPluginCall) {
        let id = call.getString("id") ?? ""
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                try await request.pause()

                let dict = returnStreamUploadRequest(request)
                let jsonData = try JSONSerialization.data(withJSONObject: dict, options: [])
                let jsonString = String(data: jsonData, encoding: .utf8) ?? "{}"
                call.resolve(["request": jsonString])
            } catch let error {
                call.reject("STREAM_PAUSE_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func resumeStream(_ call: CAPPluginCall) {
        let id = call.getString("id") ?? ""
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                try await request.resume()

                let dict = returnStreamUploadRequest(request)
                let jsonData = try JSONSerialization.data(withJSONObject: dict, options: [])
                let jsonString = String(data: jsonData, encoding: .utf8) ?? "{}"
                call.resolve(["request": jsonString])
            } catch let error {
                call.reject("STREAM_RESUME_ERROR", error.localizedDescription, error)
            }
        }
    }

    @objc public func retryStream(_ call: CAPPluginCall) {
        let id = call.getString("id") ?? ""
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                try await request.retry()

                let dict = returnStreamUploadRequest(request)
                let jsonData = try JSONSerialization.data(withJSONObject: dict, options: [])
                let jsonString = String(data: jsonData, encoding: .utf8) ?? "{}"
                call.resolve(["request": jsonString])
            } catch {
                call.resolve(["request": "{}"])
            }
        }
    }

    @objc public func deleteStream(_ call: CAPPluginCall) {
        let id = call.getString("id")?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if id.isEmpty {
            call.reject("STREAM_DELETE_ERROR", "Missing or invalid stream upload request id.")
            return
        }
        Task {
            do {
                let request = try await TruvideoSdkMedia.getUploadRequestById(id)
                try await request.delete()

                let dict = returnStreamUploadRequest(request)
                let jsonData = try JSONSerialization.data(withJSONObject: dict, options: [])
                let jsonString = String(data: jsonData, encoding: .utf8) ?? "{}"
                call.resolve(["request": jsonString])
            } catch let error {
                call.reject("STREAM_DELETE_ERROR", error.localizedDescription, error)
            }
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
                    let response: [String: Any] = [
                        "response": jsonString,
                        "totalPages" : request?.totalPages ?? 0 ,
                        "totalElements" : request?.totalElements ?? 0,
                        "numberOfElements" : request?.numberOfElements ?? 0,
                        "size" : request?.size ?? 0,
                        "number" : request?.number ?? 0,
                        "first" : request?.first ?? false,
                        "empty" : request?.empty ?? false,
                        "last" : request?.last ?? false,
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
