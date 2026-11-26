"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
class RTCCertificate {
  constructor(info) {
    _defineProperty(this, "_expires", void 0);
    _defineProperty(this, "_fingerprints", void 0);
    _defineProperty(this, "_id", void 0);
    this._id = info.certificateId;
    this._expires = info.expires;
    this._fingerprints = info.fingerprints;
  }
  get expires() {
    return this._expires;
  }
  getFingerprints() {
    return this._fingerprints;
  }
}
exports.default = RTCCertificate;
//# sourceMappingURL=RTCCertificate.js.map