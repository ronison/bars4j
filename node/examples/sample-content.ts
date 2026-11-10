/**
 * Sample payloads for the demos, mirroring {@code org.bars4j.demo.SampleContent} in the Java
 * project (and the codes used by the golden tests).
 */
export const SAMPLES: ReadonlyMap<string, string> = new Map<string, string>([
  ["EAN13", "789100031550"],
  ["EAN8", "9788515"],
  ["EAN5", "54495"],
  ["UPCA", "07567816415"],
  ["UPCE", "0756785"],
  ["CODABAR", "97885150040-85"],
  ["CODE39", "BARS4J-39"],
  ["CODE39EXT", "Bars4J-39Ext"],
  ["CODE93", "BARS4J-93"],
  ["CODE93EXT", "Bars4J-93Ext"],
  ["CODE128", "Bars4J-128"],
  ["CODE11", "9788515004041"],
  ["MSIPLESSEY", "9788515004041"],
  ["STANDARD2OF5", "978851500404"],
  ["IATA2OF5", "978851500404"],
  ["MATRIX2OF5", "123456"],
  ["DATALOGIC2OF5", "123456"],
  ["INTERLEAVED2OF5", "9876543210921"],
  ["ITF14", "1540014128876"],
  ["POSTNET", "805365961"],
  ["QRCODE", "https://github.com/ronison/bars4j"],
  ["DATAMATRIX", "https://github.com/ronison/bars4j"],
  ["PDF417", "https://github.com/ronison/bars4j"],
  ["AZTEC", "https://github.com/ronison/bars4j"],
]);

/** Returns a sample that encodes with the given format id, or {@code "Bars4J"} as fallback. */
export function sampleFor(formatId: string): string {
  return SAMPLES.get(formatId.toUpperCase()) ?? "Bars4J";
}
