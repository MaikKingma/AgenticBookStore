import { describe, expect, it } from 'vitest'
import { basketTotal, isMoney, lineTotal } from './estimate'

describe('lineTotal', () => {
  it.each([
    // A float product would give 23.970000000000002 here and 13.049999999999999 for 4.35 times 3.
    ['7.99', 3, '23.97'],
    ['12.50', 2, '25.00'],
    ['10.00', 1, '10.00'],
    ['4.35', 3, '13.05'],
    ['19.99', 1, '19.99'],
    ['999.99', 10, '9999.90'],
    ['0.05', 1, '0.05'],
    ['0.07', 3, '0.21'],
    ['5.00', 7, '35.00'],
  ])('multiplies the price %s by the quantity %i to %s', (price, quantity, expected) => {
    expect(lineTotal(price, quantity)).toBe(expected)
  })

  it('reads a price with fewer than two digits after the point without changing its value', () => {
    expect(lineTotal('10', 3)).toBe('30.00')
    expect(lineTotal('10.5', 3)).toBe('31.50')
  })

  it.each([[''], ['abc'], ['1,50'], ['-1.00'], ['1.999'], ['1.'], ['.50'], [' 1.00'], ['1.00 ']])(
    'refuses the text %j, which is not an amount of money',
    (price) => {
      expect(() => lineTotal(price, 2)).toThrow(RangeError)
    },
  )
})

describe('basketTotal', () => {
  it('adds the line totals of lines with different prices and quantities', () => {
    const lines = [
      { price: '7.99', quantity: 3 },
      { price: '12.50', quantity: 1 },
      { price: '4.35', quantity: 2 },
    ]
    expect(basketTotal(lines)).toBe('45.17')
  })

  it('adds in whole cents, where the float sum of 0.10 times 3 and 0.20 would drift', () => {
    const lines = [
      { price: '0.10', quantity: 3 },
      { price: '0.20', quantity: 1 },
      { price: '0.05', quantity: 2 },
    ]
    expect(basketTotal(lines)).toBe('0.60')
  })

  it('keeps the trailing zero and carries into the whole part', () => {
    const lines = [
      { price: '999.99', quantity: 10 },
      { price: '0.01', quantity: 1 },
      { price: '1.00', quantity: 5 },
    ]
    expect(basketTotal(lines)).toBe('10004.91')
  })

  it('is 0.00 for a basket without lines', () => {
    expect(basketTotal([])).toBe('0.00')
  })

  it('refuses a line whose price is not an amount of money', () => {
    expect(() => basketTotal([{ price: '3.50', quantity: 1 }, { price: 'free', quantity: 1 }, { price: '1.00', quantity: 2 }])).toThrow(
      RangeError,
    )
  })
})

describe('isMoney', () => {
  it.each([['0.00'], ['7.99'], ['10'], ['10.5'], ['12345678.90']])('accepts %j', (text) => {
    expect(isMoney(text)).toBe(true)
  })

  it.each([[''], ['abc'], ['1.999'], ['-1.00'], ['1e3'], ['1,50'], [' 1.00'], ['1.00\n']])('refuses %j', (text) => {
    expect(isMoney(text)).toBe(false)
  })
})
