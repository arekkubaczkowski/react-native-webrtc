function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
export default class RTCRtpHeaderExtension {
  constructor(init) {
    _defineProperty(this, "id", void 0);
    _defineProperty(this, "uri", void 0);
    _defineProperty(this, "encrypted", void 0);
    this.id = init.id;
    this.uri = init.uri;
    this.encrypted = init.encrypted;
    Object.freeze(this);
  }
  toJSON() {
    return {
      id: this.id,
      uri: this.uri,
      encrypted: this.encrypted
    };
  }
}
//# sourceMappingURL=RTCRtpHeaderExtension.js.map