# truvideo-capacitor-media-sdk

Native media Plugin using capacitor js

## Install

```bash
npm install truvideo-capacitor-media-sdk
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`mediaBuilder(...)`](#mediabuilder)
* [`getFileUploadRequestById(...)`](#getfileuploadrequestbyid)
* [`getAllFileUploadRequests(...)`](#getallfileuploadrequests)
* [`cancelMedia(...)`](#cancelmedia)
* [`deleteMedia(...)`](#deletemedia)
* [`pauseMedia(...)`](#pausemedia)
* [`resumeMedia(...)`](#resumemedia)
* [`uploadMedia(...)`](#uploadmedia)
* [`search(...)`](#search)
* [`addListener(K, ...)`](#addlistenerk-)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### mediaBuilder(...)

```typescript
mediaBuilder(options: { filePath: string; tag: string; metaData: string; }) => Promise<{ value: string; }>
```

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code>{ filePath: string; tag: string; metaData: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### getFileUploadRequestById(...)

```typescript
getFileUploadRequestById(options: { id: string; }) => Promise<string>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### getAllFileUploadRequests(...)

```typescript
getAllFileUploadRequests(options: { status: string; }) => Promise<string>
```

| Param         | Type                             |
| ------------- | -------------------------------- |
| **`options`** | <code>{ status: string; }</code> |

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### cancelMedia(...)

```typescript
cancelMedia(options: { id: string; }) => Promise<{ value: string; }>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### deleteMedia(...)

```typescript
deleteMedia(options: { id: string; }) => Promise<{ value: string; }>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### pauseMedia(...)

```typescript
pauseMedia(options: { id: string; }) => Promise<{ value: string; }>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### resumeMedia(...)

```typescript
resumeMedia(options: { id: string; }) => Promise<{ value: string; }>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### uploadMedia(...)

```typescript
uploadMedia(options: { id: string; }) => Promise<{ value: string; }>
```

| Param         | Type                         |
| ------------- | ---------------------------- |
| **`options`** | <code>{ id: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### search(...)

```typescript
search(options: { tag: string; type: string; isLibrary: boolean; page: string; pageSize: string; }) => Promise<{ value: string; }>
```

| Param         | Type                                                                                            |
| ------------- | ----------------------------------------------------------------------------------------------- |
| **`options`** | <code>{ tag: string; type: string; isLibrary: boolean; page: string; pageSize: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### addListener(K, ...)

```typescript
addListener<K extends keyof MediaEventMap>(eventName: K, listenerFunc: (event: MediaEventMap[K]) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                              |
| ------------------ | ------------------------------------------------- |
| **`eventName`**    | <code>K</code>                                    |
| **`listenerFunc`** | <code>(event: MediaEventMap[K]) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### UploadProgressEvent

| Prop           | Type                |
| -------------- | ------------------- |
| **`id`**       | <code>string</code> |
| **`progress`** | <code>string</code> |


#### UploadCompleteEventData

| Prop                      | Type                |
| ------------------------- | ------------------- |
| **`id`**                  | <code>string</code> |
| **`createdDate`**         | <code>string</code> |
| **`remoteId`**            | <code>string</code> |
| **`uploadedFileURL`**     | <code>string</code> |
| **`metaData`**            | <code>any</code>    |
| **`tags`**                | <code>any</code>    |
| **`transcriptionURL`**    | <code>string</code> |
| **`transcriptionLength`** | <code>number</code> |
| **`fileType`**            | <code>string</code> |


#### UploadErrorEvent

| Prop        | Type                |
| ----------- | ------------------- |
| **`id`**    | <code>string</code> |
| **`error`** | <code>any</code>    |


### Type Aliases


#### MediaEventMap

<code>{ onProgress: <a href="#uploadprogressevent">UploadProgressEvent</a>; onComplete: <a href="#uploadcompleteeventdata">UploadCompleteEventData</a>; onError: <a href="#uploaderrorevent">UploadErrorEvent</a>; }</code>

</docgen-api>
