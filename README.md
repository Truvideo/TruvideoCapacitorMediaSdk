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
* [`uploadMedia(...)`](#uploadmedia)
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


### uploadMedia(...)

```typescript
uploadMedia(options: object) => Promise<{}>
```

| Param         | Type                |
| ------------- | ------------------- |
| **`options`** | <code>object</code> |

**Returns:** <code>Promise&lt;{}&gt;</code>

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


### Type Aliases


#### MediaEventMap

<code>{ onProgress: { id: string; progress: number }; onComplete: { id: string; response: any }; onError: { id: string; error: any }; }</code>

</docgen-api>
