"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _RTCRtcpParameters = _interopRequireDefault(require("./RTCRtcpParameters"));
var _RTCRtpCodecParameters = _interopRequireDefault(require("./RTCRtpCodecParameters"));
var _RTCRtpHeaderExtension = _interopRequireDefault(require("./RTCRtpHeaderExtension"));
var _RTCUtil = require("./RTCUtil");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
class RTCRtpParameters {
  constructor(init) {
    _defineProperty(this, "codecs", []);
    _defineProperty(this, "headerExtensions", []);
    _defineProperty(this, "rtcp", void 0);
    for (const codec of init.codecs) {
      this.codecs.push(new _RTCRtpCodecParameters.default(codec));
    }
    for (const ext of init.headerExtensions) {
      this.headerExtensions.push(new _RTCRtpHeaderExtension.default(ext));
    }
    this.rtcp = new _RTCRtcpParameters.default(init.rtcp);
  }
  toJSON() {
    return {
      codecs: this.codecs.map(c => (0, _RTCUtil.deepClone)(c)),
      headerExtensions: this.headerExtensions.map(he => (0, _RTCUtil.deepClone)(he)),
      rtcp: (0, _RTCUtil.deepClone)(this.rtcp)
    };
  }
}
exports.default = RTCRtpParameters;
//# sourceMappingURL=RTCRtpParameters.js.map