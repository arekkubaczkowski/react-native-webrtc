function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
import RTCRtcpParameters from './RTCRtcpParameters';
import RTCRtpCodecParameters from './RTCRtpCodecParameters';
import RTCRtpHeaderExtension from './RTCRtpHeaderExtension';
import { deepClone } from './RTCUtil';
export default class RTCRtpParameters {
  constructor(init) {
    _defineProperty(this, "codecs", []);
    _defineProperty(this, "headerExtensions", []);
    _defineProperty(this, "rtcp", void 0);
    for (const codec of init.codecs) {
      this.codecs.push(new RTCRtpCodecParameters(codec));
    }
    for (const ext of init.headerExtensions) {
      this.headerExtensions.push(new RTCRtpHeaderExtension(ext));
    }
    this.rtcp = new RTCRtcpParameters(init.rtcp);
  }
  toJSON() {
    return {
      codecs: this.codecs.map(c => deepClone(c)),
      headerExtensions: this.headerExtensions.map(he => deepClone(he)),
      rtcp: deepClone(this.rtcp)
    };
  }
}
//# sourceMappingURL=RTCRtpParameters.js.map