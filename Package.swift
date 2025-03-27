// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TruvideoCapacitorMediaSdk",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "TruvideoCapacitorMediaSdk",
            targets: ["TruvideoSdkMediaPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "TruvideoSdkMediaPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/TruvideoSdkMediaPlugin"),
        .testTarget(
            name: "TruvideoSdkMediaPluginTests",
            dependencies: ["TruvideoSdkMediaPlugin"],
            path: "ios/Tests/TruvideoSdkMediaPluginTests")
    ]
)