export function formatMinor(amountMinor: number, currency: string) {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amountMinor / minorUnit(currency));
}

function minorUnit(currency: string) {
  return currency === 'JPY' ? 1 : currency === 'BHD' ? 1000 : 100;
}
