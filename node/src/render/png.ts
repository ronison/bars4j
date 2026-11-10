import { deflateSync } from "node:zlib";

import type { RasterImage } from "./raster-image.ts";

/**
 * Minimal, dependency-free PNG encoder (8-bit truecolour, non-interlaced).
 *
 * <p>Bars4J images are always fully opaque, so colour type 2 (RGB) is enough. The deflate stream is
 * produced by Node's built-in {@code zlib}, keeping the raster output free of any native module.
 */
export function encodePng(image: RasterImage): Uint8Array {
  // Raw scanlines: one filter byte (0 = None) followed by the RGB triples of each row.
  const stride = image.width * 3;
  const raw = Buffer.alloc((stride + 1) * image.height);
  for (let y = 0; y < image.height; y++) {
    let out = y * (stride + 1);
    raw[out++] = 0;
    const rowBase = y * image.width;
    for (let x = 0; x < image.width; x++) {
      const argb = image.pixels[rowBase + x];
      raw[out++] = (argb >>> 16) & 0xff;
      raw[out++] = (argb >>> 8) & 0xff;
      raw[out++] = argb & 0xff;
    }
  }

  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(image.width, 0);
  ihdr.writeUInt32BE(image.height, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 2; // colour type: truecolour (RGB)
  ihdr[10] = 0; // compression method: deflate
  ihdr[11] = 0; // filter method: adaptive
  ihdr[12] = 0; // interlace method: none

  const signature = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const chunks = [
    signature,
    chunk("IHDR", ihdr),
    chunk("IDAT", deflateSync(raw)),
    chunk("IEND", Buffer.alloc(0)),
  ];
  return Buffer.concat(chunks);
}

function chunk(type: string, data: Buffer): Buffer {
  const length = Buffer.alloc(4);
  length.writeUInt32BE(data.length, 0);
  const typeBytes = Buffer.from(type, "ascii");
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([typeBytes, data])), 0);
  return Buffer.concat([length, typeBytes, data, crc]);
}

const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c >>> 0;
  }
  return table;
})();

function crc32(data: Buffer): number {
  let crc = 0xffffffff;
  for (let i = 0; i < data.length; i++) {
    crc = CRC_TABLE[(crc ^ data[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}
