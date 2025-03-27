import { WebPlugin } from '@capacitor/core';

import type { TruvideoSdkMediaPlugin } from './definitions';

export class TruvideoSdkMediaWeb extends WebPlugin implements TruvideoSdkMediaPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
