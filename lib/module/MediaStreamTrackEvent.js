function _defineProperty(e, r, t) { return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: !0, configurable: !0, writable: !0 }) : e[r] = t, e; }
function _toPropertyKey(t) { var i = _toPrimitive(t, "string"); return "symbol" == typeof i ? i : i + ""; }
function _toPrimitive(t, e) { if ("object" != typeof t || !t) return t; var r; if ("undefined" != typeof Symbol && void 0 !== (r = t[Symbol.toPrimitive])) { var i = r.call(t, e || "default"); if ("object" != typeof i) return i; throw new TypeError("@@toPrimitive must return a primitive value."); } return ("string" === e ? String : Number)(t); }
import { Event } from './vendor/event-target-shim';
/**
 * @eventClass
 * This event is fired whenever the MediaStreamTrack has changed in any way.
 * @param {MEDIA_STREAM_EVENTS} type - The type of event.
 * @param {IMediaStreamTrackEventInitDict} eventInitDict - The event init properties.
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/API/MediaStream#events MDN} for details.
 */
export default class MediaStreamTrackEvent extends Event {
  /** @eventProperty */

  constructor(type, eventInitDict) {
    super(type, eventInitDict);
    _defineProperty(this, "track", void 0);
    this.track = eventInitDict.track;
  }
}
//# sourceMappingURL=MediaStreamTrackEvent.js.map