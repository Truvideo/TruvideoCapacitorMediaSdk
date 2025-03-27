import { registerPlugin } from '@capacitor/core';
const TruvideoSdkMedia = registerPlugin('TruvideoSdkMedia', {
    web: () => import('./web').then((m) => new m.TruvideoSdkMediaWeb()),
});
export * from './definitions';
export { TruvideoSdkMedia };
//# sourceMappingURL=index.js.map