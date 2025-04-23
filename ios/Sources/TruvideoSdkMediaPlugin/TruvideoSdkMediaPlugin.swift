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
        CAPPluginMethod(name: "uploadMedia", returnType: CAPPluginReturnPromise)
    ]
    //private let implementation = TruvideoSdkMedia()

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": value
        ])
    }
    
    
    
    @objc public func uploadMedia(_ call: CAPPluginCall) {
        print("🔍 Full Call Data: \(call)")
        let filepath = call.getString("filePath") ?? ""
        let tag = call.getString("tag") ?? ""
        let metaData = call.getString("metaData") ?? ""
        
        print("📂 Filepath Received: \(filepath)")  // Debugging
        print("🏷 Tag: \(tag)")
        print("📜 MetaData: \(metaData)")
         
        
         guard !filepath.isEmpty else {
             call.reject("INVALID_FILE_PATH", "File path is empty")
             return
         }
         
         let fileURL = URL(fileURLWithPath: filepath) // ✅ Correct way to create a file URL
        

            do {
                let builder = try createFileUploadRequestBuilder(fileURL: fileURL, tag: tag, metaData: metaData)
                try executeUploadRequest(builder: builder,call)
            } catch {
                call.reject("UPLOAD_ERROR", "Upload failed", error)
            }
        }
        
        private func createFileUploadRequestBuilder(fileURL: URL, tag: String, metaData: String) throws -> TruvideoSdkMedia.FileUploadRequestBuilder {
            let builder = TruvideoSdkMedia.FileUploadRequestBuilder(fileURL: fileURL)
            
            // Convert tag JSON string to dictionary
            let tagDict = try convertToDictionary(from: tag)
            for (key, value) in tagDict {
                builder.addTag(key, value)
            }
            
            // Convert metadata JSON string to Metadata type
//            let metadataObj = try convertToDictionary(from: metaData)
            for (key, value) in tagDict {
                builder.addMetadata(key, value)
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
                    let metadataDict = uploadedResult.metadata
                    let tags = uploadedResult.tags
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
        private func sendEvent(withName name: String, body: [String: Any]) {
//            guard let bridge = RCTBridge.current() else { return }
//            bridge.eventDispatcher().sendAppEvent(withName: name, body: body)
            self.notifyListeners(name, data: body)
        }
}
