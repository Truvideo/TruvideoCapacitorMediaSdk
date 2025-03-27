import { registerPlugin } from '@capacitor/core';
const MediaPlugin = registerPlugin('MediaPlugin', {
    web: () => import('./web').then((m) => new m.MediaPluginWeb()),
});
export * from './definitions';
export { MediaPlugin };
//# sourceMappingURL=index.js.map