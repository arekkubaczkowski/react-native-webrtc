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
 * This event is fired whenever the RTCDataChannel send message.
 * @param {MESSAGE_EVENTS} type - The type of event.
 * @param {IMessageEventInitDict} eventInitDict - The event init properties.
 * @see
 * {@link https://developer.mozilla.org/en-US/docs/Web/API/RTCDataChannel/message_event#event_type MDN} for details.
 */
class MessageEvent extends _eventTargetShim.Event {
  /** @eventProperty */

  constructor(type, eventInitDict) {
    super(type, eventInitDict);
    _defineProperty(this, "data", void 0);
    this.data = eventInitDict.data;
  }
}
exports.default = MessageEvent;
//# sourceMappingURL=MessageEvent.js.map