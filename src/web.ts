import { WebPlugin } from '@capacitor/core';

import type { MediaPluginPlugin } from './definitions';

export class MediaPluginWeb extends WebPlugin implements MediaPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
