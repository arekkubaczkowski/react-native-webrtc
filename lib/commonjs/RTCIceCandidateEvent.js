"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
var _eventTargetShim = require("./vendor/event-target-shim");
function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
/**
 * @eventClass
 * This event is fired whenever the icecandidate related RTC_EVENTS changed.
 * @type {RTCIceCandidateEvent} for icecandidate related.
 * @param {RTC_ICECANDIDATE_EVENTS} type - The type of event.
 * @param {IRTCDataChannelEventInitDict} eventInitDict - The event init properties.
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection#events MDN} for details.
 */
class RTCIceCandidateEvent extends _eventTargetShim.Event {
  /** @eventProperty */

  constructor(type, eventInitDict) {
    var _eventInitDict$candid;
    super(type, eventInitDict);
    _defineProperty(this, "candidate", void 0);
    this.candidate = (_eventInitDict$candid = eventInitDict === null || eventInitDict === void 0 ? void 0 : eventInitDict.candidate) !== null && _eventInitDict$candid !== void 0 ? _eventInitDict$candid : null;
  }
}
exports.default = RTCIceCandidateEvent;
//# sourceMappingURL=RTCIceCandidateEvent.js.map