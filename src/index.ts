import { registerPlugin } from '@capacitor/core';

import type { TruvideoSdkMediaPlugin } from './definitions';

const TruvideoSdkMedia = registerPlugin<TruvideoSdkMediaPlugin>('TruvideoSdkMedia');

export * from './definitions';
export { TruvideoSdkMedia };
