'use strict';

var core = require('@capacitor/core');

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
//# sourceMappingURL=plugin.cjs.js.map
