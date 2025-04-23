export interface TruvideoSdkMediaPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  uploadMedia(options: { value: string }): Promise<{ value: string }>;
}
