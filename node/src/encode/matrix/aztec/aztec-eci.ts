/**
 * Maps character-set names to the ECI assignment numbers used by the Aztec {@code FLG(n)} escape.
 *
 * <p>Only the character sets commonly seen in barcodes are listed. When a character set has no
 * assignment number the caller should encode without an ECI and fall back to the default Latin-1
 * interpretation.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecEci}.
 */
const ASSIGNMENTS: Record<string, number> = {
  "ISO-8859-1": 1,
  "ISO8859-1": 1,
  ISO88591: 1,
  "UTF-8": 26,
  UTF8: 26,
  "US-ASCII": 27,
  ASCII: 27,
  "UTF-16BE": 25,
  UNICODEBIGUNMARKED: 25,
  "WINDOWS-1250": 21,
  CP1250: 21,
  "WINDOWS-1251": 22,
  CP1251: 22,
  "WINDOWS-1252": 23,
  CP1252: 23,
  "WINDOWS-1256": 24,
  CP1256: 24,
  BIG5: 28,
  GB18030: 29,
  GB2312: 29,
  GBK: 29,
  "EUC-KR": 30,
  EUCKR: 30,
};

/**
 * Returns the ECI assignment number for {@code charsetName}.
 *
 * @throws Error if the character set has no known ECI number
 */
export function eciValueFor(charsetName: string): number {
  const assignment = ASSIGNMENTS[charsetName.toUpperCase()];
  if (assignment === undefined) {
    throw new Error("No ECI code for character set " + charsetName);
  }
  return assignment;
}
