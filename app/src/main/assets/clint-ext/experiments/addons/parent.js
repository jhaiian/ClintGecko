"use strict";

this.clintAddons = class extends ExtensionAPI {
  getAPI(context) {
    return {
      clintAddons: {
        async installUnsigned(path) {
          try {
            const { AddonManager } = ChromeUtils.importESModule("resource://gre/modules/AddonManager.sys.mjs");
            const file = Cc["@mozilla.org/file/local;1"].createInstance(Ci.nsIFile);
            file.initWithPath(path);
            if (!file.exists()) return { error: "missing" };
            const addon = await AddonManager.installTemporaryAddon(file);
            if (!addon) return { error: "failed" };
            return { id: addon.id, name: addon.name || addon.id };
          } catch (e) {
            return { error: String((e && e.message) || e) };
          }
        }
      }
    };
  }
};
