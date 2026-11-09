// The only money arithmetic of the storefront. Totals shown before an order is placed are estimates: the bookstore
// works out the real total when it takes the order. An amount is a decimal string as the bookstore sends it ("7.99").
// It is turned into whole cents, so that adding and multiplying never meets a floating point rounding error, and is
// written back as text with two digits after the point.

const MONEY = /^(\d+)(?:\.(\d{1,2}))?$/

// True for a plain decimal amount with at most two digits after the point (no sign, no thousands separator).
export function isMoney(text: string): boolean {
  return MONEY.test(text)
}

function toCents(price: string): number {
  const match = MONEY.exec(price)
  if (match === null) {
    throw new RangeError(`Not an amount of money: ${JSON.stringify(price)}`)
  }
  return Number(match[1]) * 100 + Number((match[2] ?? '').padEnd(2, '0'))
}

function fromCents(cents: number): string {
  const rest = cents % 100
  return `${(cents - rest) / 100}.${String(rest).padStart(2, '0')}`
}

export function lineTotal(price: string, quantity: number): string {
  return fromCents(toCents(price) * quantity)
}

export function basketTotal(lines: ReadonlyArray<{ price: string; quantity: number }>): string {
  return fromCents(lines.reduce((sum, line) => sum + toCents(line.price) * line.quantity, 0))
}
