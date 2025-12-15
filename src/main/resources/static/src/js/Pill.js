// Pill.js
// Created: 2025-12-15
// Encapsulates pill DOM and physics logic for maintainability and code sharing.

class Pill {
  constructor(domElement) {
    this.domElement = domElement;
    this.vx = 0;
    this.vy = 0;
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

  setPosition(x, y) {
    this.domElement.style.left = `${x}px`;
    this.domElement.style.top = `${y}px`;
  }

  width() {
    return this.domElement.offsetWidth || 120;
  }

  height() {
    return this.domElement.offsetHeight || 22;
  }

  static createPillElement() {
    const el = document.createElement('div');
    el.className = 'pill';
    return el;
  }

  static removePillElement(domElement) {
    if (domElement.parentNode) {
      domElement.parentNode.removeChild(domElement);
    }
  }
}

if (typeof window !== 'undefined') {
  window.Pill = Pill;
}

// For CommonJS compatibility (tests or Node)
if (typeof module !== 'undefined' && module.exports) {
  module.exports = Pill;
}
