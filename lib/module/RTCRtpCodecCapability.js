function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
export default class RTCRtpCodecCapability {
  constructor(init) {
    _defineProperty(this, "_mimeType", void 0);
    this._mimeType = init.mimeType;
    Object.freeze(this);
  }
  get mimeType() {
    return this._mimeType;
  }
}
//# sourceMappingURL=RTCRtpCodecCapability.js.map