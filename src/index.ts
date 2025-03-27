import { registerPlugin } from '@capacitor/core';

import type { MediaPluginPlugin } from './definitions';

const MediaPlugin = registerPlugin<MediaPluginPlugin>('MediaPlugin', {
  web: () => import('./web').then((m) => new m.MediaPluginWeb()),
});

export * from './definitions';
export { MediaPlugin };
