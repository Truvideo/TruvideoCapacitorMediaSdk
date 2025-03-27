export interface MediaPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
