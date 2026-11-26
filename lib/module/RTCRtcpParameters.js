function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
export default class RTCRtcpParameters {
  constructor(init) {
    _defineProperty(this, "cname", void 0);
    _defineProperty(this, "reducedSize", void 0);
    this.cname = init.cname;
    this.reducedSize = init.reducedSize;
    Object.freeze(this);
  }
  toJSON() {
    return {
      cname: this.cname,
      reducedSize: this.reducedSize
    };
  }
}
//# sourceMappingURL=RTCRtcpParameters.js.map