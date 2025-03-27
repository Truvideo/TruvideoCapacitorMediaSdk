'use strict';

var core = require('@capacitor/core');

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
//# sourceMappingURL=plugin.cjs.js.map
