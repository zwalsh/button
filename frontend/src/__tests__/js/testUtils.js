// Utility for creating mock pill objects for floatingPresserPhysics tests
export function makeMockPill({ x, y, w = 40, h = 40, vx = 0, vy = 0, initialized = true } = {}) {
  let _x = x ?? 0, _y = y ?? 0;
  return {
    vx, vy,
    width: () => w,
    height: () => h,
    centerX: () => _x,
    centerY: () => _y,
    leftX: () => _x - w/2,
    rightX: () => _x + w/2,
    topY: () => _y - h/2,
    bottomY: () => _y + h/2,
    isInitialized: () => initialized,
    setCenter: (nx, ny) => { _x = nx; _y = ny; },
    x: _x,
    y: _y
  };
}
