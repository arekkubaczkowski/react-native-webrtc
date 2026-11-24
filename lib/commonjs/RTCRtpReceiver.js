"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _reactNative = require("react-native");
var _Logger = _interopRequireDefault(require("./Logger"));
var _RTCRtpReceiveParameters = _interopRequireDefault(require("./RTCRtpReceiveParameters"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
const {
  LKRNWebRTCModule: WebRTCModule
} = _reactNative.NativeModules;
const log = new _Logger.default('receiver');
class RTCRtpReceiver {
  constructor(info) {
    _defineProperty(this, "_id", void 0);
    _defineProperty(this, "_peerConnectionId", void 0);
    _defineProperty(this, "_track", null);
    _defineProperty(this, "_rtpParameters", void 0);
    this._id = info.id;
    this._peerConnectionId = info.peerConnectionId;
    this._rtpParameters = new _RTCRtpReceiveParameters.default(info.rtpParameters);
    if (info.track) {
      this._track = info.track;
    }
  }
  static getCapabilities(kind) {
    return WebRTCModule.receiverGetCapabilities(kind);
  }
  getStats() {
    log.debug(`${this._peerConnectionId} ${this._id} getStats`);
    return WebRTCModule.receiverGetStats(this._peerConnectionId, this._id).then(data => {
      log.debug(`${this._peerConnectionId} ${this._id} getStats OK`);

      /* On both Android and iOS it is faster to construct a single
      JSON string representing the Map of StatsReports and have it
      pass through the React Native bridge rather than the Map of
      StatsReports. While the implementations do try to be faster in
      general, the stress is on being faster to pass through the React
      Native bridge which is a bottleneck that tends to be visible in
      the UI when there is congestion involving UI-related passing.
      */
      return new Map(JSON.parse(data));
    });
  }
  getParameters() {
    return this._rtpParameters;
  }
  get id() {
    return this._id;
  }
  get track() {
    return this._track;
  }
}
exports.default = RTCRtpReceiver;
//# sourceMappingURL=RTCRtpReceiver.js.map