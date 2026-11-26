function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
import { forwardRef } from 'react';
import ReactNative, { UIManager } from 'react-native';
import RTCView from './RTCView';
/**
 * A convenience wrapper around RTCView to handle the fallback view as a prop.
 */
const RTCPIPView = /*#__PURE__*/forwardRef((props, ref) => {
  var _rtcViewProps$iosPIP, _rtcViewProps$iosPIP2;
  const rtcViewProps = {
    ...props
  };
  const fallbackView = (_rtcViewProps$iosPIP = rtcViewProps.iosPIP) === null || _rtcViewProps$iosPIP === void 0 ? void 0 : _rtcViewProps$iosPIP.fallbackView;
  (_rtcViewProps$iosPIP2 = rtcViewProps.iosPIP) === null || _rtcViewProps$iosPIP2 === void 0 ? true : delete _rtcViewProps$iosPIP2.fallbackView;
  return /*#__PURE__*/React.createElement(RTCView, _extends({
    ref: ref
  }, rtcViewProps), fallbackView);
});
export function startIOSPIP(ref) {
  UIManager.dispatchViewManagerCommand(ReactNative.findNodeHandle(ref.current), UIManager.getViewManagerConfig('LKRNRTCVideoView').Commands.startIOSPIP, []);
}
export function stopIOSPIP(ref) {
  UIManager.dispatchViewManagerCommand(ReactNative.findNodeHandle(ref.current), UIManager.getViewManagerConfig('LKRNRTCVideoView').Commands.stopIOSPIP, []);
}
export default RTCPIPView;
//# sourceMappingURL=RTCPIPView.js.map