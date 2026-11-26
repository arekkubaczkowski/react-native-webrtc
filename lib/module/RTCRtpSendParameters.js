function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
import RTCRtpEncodingParameters from './RTCRtpEncodingParameters';
import RTCRtpParameters from './RTCRtpParameters';
import { deepClone } from './RTCUtil';
/**
 * Class to convert degradation preference format. Native has a format such as
 * MAINTAIN_FRAMERATE whereas the web APIs expect maintain-framerate
 */
class DegradationPreference {
  static fromNative(nativeFormat) {
    const stringFormat = nativeFormat.toLowerCase().replace('_', '-');
    return stringFormat;
  }
  static toNative(format) {
    return format.toUpperCase().replace('-', '_');
  }
}
export default class RTCRtpSendParameters extends RTCRtpParameters {
  constructor(init) {
    super(init);
    _defineProperty(this, "transactionId", void 0);
    _defineProperty(this, "encodings", void 0);
    _defineProperty(this, "degradationPreference", void 0);
    this.transactionId = init.transactionId;
    this.encodings = [];
    this.degradationPreference = init.degradationPreference ? DegradationPreference.fromNative(init.degradationPreference) : null;
    for (const enc of init.encodings) {
      this.encodings.push(new RTCRtpEncodingParameters(enc));
    }
  }
  toJSON() {
    const obj = super.toJSON();
    obj['transactionId'] = this.transactionId;
    obj['encodings'] = this.encodings.map(e => deepClone(e));
    if (this.degradationPreference !== null) {
      obj['degradationPreference'] = DegradationPreference.toNative(this.degradationPreference);
    }
    return obj;
  }
}
//# sourceMappingURL=RTCRtpSendParameters.js.map