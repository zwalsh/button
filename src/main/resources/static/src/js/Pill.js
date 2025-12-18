// Pill.js
// Created: 2025-12-15
// Encapsulates pill DOM and physics logic for maintainability and code sharing.

class Pill {
  constructor(domElement) {
    this.domElement = domElement;
    this.vx = 0;
    this.vy = 0;
    this.initialPositionSet = false;
  }

  isInitialized() {
    return this.initialPositionSet;
  }

  centerX() {
    return this.leftX() + this.width() / 2;
  }

  centerY() {
    return this.topY() + this.height() / 2;
  }

  leftX() {
    return parseFloat(this.domElement.style.left) || 0;
  }

  topY() {
    return parseFloat(this.domElement.style.top) || 0;
  }

  rightX() {
    return this.leftX() + this.width();
  }

  bottomY() {
    return this.topY() + this.height();
  }

  setCenter(x, y, containerWidth, containerHeight) {
    let left = this.safeX(x, containerWidth) - this.width() / 2;
    let top = this.safeY(y, containerHeight) - this.height() / 2;
    this.domElement.style.left = `${left}px`;
    this.domElement.style.top = `${top}px`;
    this.domElement.style.display = 'block';
    this.initialPositionSet = true;
  }

  safeX(x, containerWidth) {
      return Math.max(this.width() / 2, Math.min(containerWidth - this.width() / 2, x));
  }

  safeY(y, containerHeight) {
      return Math.max(this.height() / 2, Math.min(containerHeight - this.height() / 2, y));
  }

  setVelocity(vx, vy) {
    this.vx = vx;
    this.vy = vy;
  }

  width() {
    return this.domElement.offsetWidth || 120;
  }

  height() {
    return this.domElement.offsetHeight || 22;
  }

  static createPill(name) {
        const pillEl = document.createElement('div');
        pillEl.className = 'floating-presser-pill';
        pillEl.textContent = truncateName(name);
        pillEl.style.position = 'absolute';
        pillEl.style.display = 'none';
        return new Pill(pillEl);
  }

  remove() {
    if (this.domElement.parentNode) {
      this.domElement.classList.add('floating-presser-pill--out');
      setTimeout(() => {
        if (this.domElement.parentNode) {
          this.domElement.parentNode.removeChild(this.domElement);
        }
      }, 200); // Match CSS animation duration
    }
  }
}

function truncateName(name) {
    return name.length > 16 ? name.slice(0, 16) + '…' : name;
}

if (typeof window !== 'undefined') {
  window.Pill = Pill;
}

// For CommonJS compatibility (tests or Node)
if (typeof module !== 'undefined' && module.exports) {
  module.exports = Pill;
}
