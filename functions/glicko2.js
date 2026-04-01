/**
 * Glicko-2 rating algorithm — JavaScript port of Ryan Kirkman's Python implementation.
 * https://www.glicko.net/glicko/glicko2.pdf
 *
 * Usage:
 *   const { Player } = require("./glicko2");
 *   const p = new Player(1500, 350, 0.06);
 *   p.updatePlayer([1400, 1550, 1700], [30, 100, 300], [1, 0, 0]);
 *   console.log(p.rating, p.rd, p.vol);
 */

const TAU = 0.5; // System constant — constrains volatility change over time

class Player {
  /**
   * @param {number} rating  Public Glicko-2 rating       (default 1500)
   * @param {number} rd      Rating Deviation              (default  350)
   * @param {number} vol     Volatility                    (default 0.06)
   */
  constructor(rating = 1500, rd = 350, vol = 0.06) {
    // Store on the internal µ / φ scale used by the algorithm
    this._mu  = (rating - 1500) / 173.7178;
    this._phi = rd / 173.7178;
    this.vol  = vol;
  }

  // ── Public accessors (external scale) ───────────────────────────────────

  get rating() { return this._mu  * 173.7178 + 1500; }
  set rating(r) { this._mu  = (r  - 1500) / 173.7178; }

  get rd()     { return this._phi * 173.7178; }
  set rd(r)    { this._phi = r / 173.7178; }

  // ── Private helpers ──────────────────────────────────────────────────────

  /** g(φ) reduction function */
  _g(phi) {
    return 1 / Math.sqrt(1 + (3 * phi ** 2) / Math.PI ** 2);
  }

  /** Expected score E(µ, µⱼ, φⱼ) — uses internal this._mu */
  _E(p2mu, p2phi) {
    return 1 / (1 + Math.exp(-this._g(p2phi) * (this._mu - p2mu)));
  }

  /** Estimated variance v */
  _v(muList, phiList) {
    let sum = 0;
    for (let i = 0; i < muList.length; i++) {
      const e = this._E(muList[i], phiList[i]);
      sum += this._g(phiList[i]) ** 2 * e * (1 - e);
    }
    return 1 / sum;
  }

  /** Estimated improvement Δ */
  _delta(muList, phiList, outcomeList, v) {
    let sum = 0;
    for (let i = 0; i < muList.length; i++) {
      sum += this._g(phiList[i]) * (outcomeList[i] - this._E(muList[i], phiList[i]));
    }
    return v * sum;
  }

  /** New volatility σ′ via iterative algorithm (Illinois / Newton-Raphson) */
  _newVol(muList, phiList, outcomeList, v) {
    const delta = this._delta(muList, phiList, outcomeList, v);
    const a     = Math.log(this.vol ** 2);
    let x0 = a;
    let x1 = 0;
    let iterations = 0;

    // Mirrors the original Python: converge until x0 === x1 (floating-point equality)
    // Cap at 500 iterations to guard against edge-case divergence.
    while (x0 !== x1 && iterations < 500) {
      x0 = x1;
      const d  = this._mu ** 2 + v + Math.exp(x0);
      const h1 = -(x0 - a) / TAU ** 2
        - 0.5 * Math.exp(x0) / d
        + 0.5 * Math.exp(x0) * (delta / d) ** 2;
      const h2 = -1 / TAU ** 2
        - 0.5 * Math.exp(x0) * (this._mu ** 2 + v) / d ** 2
        + 0.5 * delta ** 2 * Math.exp(x0) * (this._mu ** 2 + v - Math.exp(x0)) / d ** 3;
      x1 = x0 - h1 / h2;
      iterations++;
    }
    return Math.exp(x1 / 2);
  }

  /** Step 6 of the algorithm — pre-rating period RD update */
  _preRatingRD() {
    this._phi = Math.sqrt(this._phi ** 2 + this.vol ** 2);
  }

  // ── Public API ───────────────────────────────────────────────────────────

  /**
   * Update player rating after a rating period.
   *
   * @param {number[]} ratingList  Opponent ratings (external scale)
   * @param {number[]} rdList      Opponent RDs     (external scale)
   * @param {number[]} outcomeList Outcomes:  1 = win, 0.5 = draw, 0 = loss
   */
  updatePlayer(ratingList, rdList, outcomeList) {
    // Convert opponents to internal scale
    const muList  = ratingList.map(r  => (r  - 1500) / 173.7178);
    const phiList = rdList.map(rd => rd / 173.7178);

    const v   = this._v(muList, phiList);
    this.vol  = this._newVol(muList, phiList, outcomeList, v);
    this._preRatingRD();

    // New φ′
    this._phi = 1 / Math.sqrt(1 / this._phi ** 2 + 1 / v);

    // New µ′
    let sum = 0;
    for (let i = 0; i < muList.length; i++) {
      sum += this._g(phiList[i]) * (outcomeList[i] - this._E(muList[i], phiList[i]));
    }
    this._mu += this._phi ** 2 * sum;
  }

  /**
   * Apply Step 6 only — use for players who did not compete in a rating period.
   * Their RD increases (more uncertainty) but rating stays the same.
   */
  didNotCompete() {
    this._preRatingRD();
  }

  /** Returns plain object suitable for Firestore / JSON serialisation */
  toObject() {
    return {
      glickoRating : Math.round(this.rating * 100) / 100,
      glickoRd     : Math.round(this.rd     * 100) / 100,
      glickoVol    : Math.round(this.vol    * 10000) / 10000,
    };
  }
}

module.exports = { Player };

