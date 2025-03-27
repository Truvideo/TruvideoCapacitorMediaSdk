var capacitorMediaPlugin = (function (exports, core) {
    'use strict';

    const MediaPlugin = core.registerPlugin('MediaPlugin', {
        web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.MediaPluginWeb()),
    });

    class MediaPluginWeb extends core.WebPlugin {
        async echo(options) {
            console.log('ECHO', options);
            return options;
        }
    }

    var web = /*#__PURE__*/Object.freeze({
        __proto__: null,
        MediaPluginWeb: MediaPluginWeb
    });

    exports.MediaPlugin = MediaPlugin;

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
