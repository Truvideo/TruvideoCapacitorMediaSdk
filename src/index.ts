import { registerPlugin } from '@capacitor/core';

import type { TruvideoSdkMediaPlugin } from './definitions';

const TruvideoSdkMedia = registerPlugin<TruvideoSdkMediaPlugin>('TruvideoSdkMedia', {
  web: () => import('./web').then((m) => new m.TruvideoSdkMediaWeb()),
});

export * from './definitions';
export { TruvideoSdkMedia };
