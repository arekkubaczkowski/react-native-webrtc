"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;
exports.startIOSPIP = startIOSPIP;
exports.stopIOSPIP = stopIOSPIP;
var _react = require("react");
var _reactNative = _interopRequireWildcard(require("react-native"));
var _RTCView = _interopRequireDefault(require("./RTCView"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
var cache;
function _interopRequireWildcard(e, t) { if (!t && e && e.__esModule) return e; var r, c, a = Object.defineProperty, n = { __proto__: null, default: e }; if (Object(e) !== e) return n; if (cache || "function" != typeof WeakMap || (cache = new WeakMap()), cache) { if (cache.has(e)) return cache.get(e); cache.set(e, n); } for (c in e) "default" !== c && {}.hasOwnProperty.call(e, c) && ((r = a && Object.getOwnPropertyDescriptor(e, c)) && (r.get || r.set) ? a(n, c, r) : n[c] = e[c]); return n; }
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * A convenience wrapper around RTCView to handle the fallback view as a prop.
 */
const RTCPIPView = /*#__PURE__*/(0, _react.forwardRef)((props, ref) => {
  var _rtcViewProps$iosPIP, _rtcViewProps$iosPIP2;
  const rtcViewProps = {
    ...props
  };
  const fallbackView = (_rtcViewProps$iosPIP = rtcViewProps.iosPIP) === null || _rtcViewProps$iosPIP === void 0 ? void 0 : _rtcViewProps$iosPIP.fallbackView;
  (_rtcViewProps$iosPIP2 = rtcViewProps.iosPIP) === null || _rtcViewProps$iosPIP2 === void 0 ? true : delete _rtcViewProps$iosPIP2.fallbackView;
  return /*#__PURE__*/React.createElement(_RTCView.default, _extends({
    ref: ref
  }, rtcViewProps), fallbackView);
});
function startIOSPIP(ref) {
  _reactNative.UIManager.dispatchViewManagerCommand(_reactNative.default.findNodeHandle(ref.current), _reactNative.UIManager.getViewManagerConfig('LKRNRTCVideoView').Commands.startIOSPIP, []);
}
function stopIOSPIP(ref) {
  _reactNative.UIManager.dispatchViewManagerCommand(_reactNative.default.findNodeHandle(ref.current), _reactNative.UIManager.getViewManagerConfig('LKRNRTCVideoView').Commands.stopIOSPIP, []);
}
var _default = exports.default = RTCPIPView;
//# sourceMappingURL=RTCPIPView.js.map