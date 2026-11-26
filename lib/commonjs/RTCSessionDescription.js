"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
class RTCSessionDescription {
  constructor(info = {
    type: null,
    sdp: ''
  }) {
    _defineProperty(this, "_sdp", void 0);
    _defineProperty(this, "_type", void 0);
    this._sdp = info.sdp;
    this._type = info.type;
  }
  get sdp() {
    return this._sdp;
  }
  get type() {
    return this._type;
  }
  toJSON() {
    return {
      sdp: this._sdp,
      type: this._type
    };
  }
}
exports.default = RTCSessionDescription;
//# sourceMappingURL=RTCSessionDescription.js.map