var capacitorTruvideoSdkMedia = (function (exports, core) {
    'use strict';

    const TruvideoSdkMedia = core.registerPlugin('TruvideoSdkMedia', {
        web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.TruvideoSdkMediaWeb()),
    });

    class TruvideoSdkMediaWeb extends core.WebPlugin {
        async echo(options) {
            console.log('ECHO', options);
            return options;
        }
    }

    var web = /*#__PURE__*/Object.freeze({
        __proto__: null,
        TruvideoSdkMediaWeb: TruvideoSdkMediaWeb
    });

    exports.TruvideoSdkMedia = TruvideoSdkMedia;

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
