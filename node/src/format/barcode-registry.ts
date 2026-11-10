import type { BarcodeFormat } from "./barcode-format.ts";
import { registerBuiltinFormats } from "./builtin-formats.ts";

/**
 * Registry of the barcode formats known to the library. Formats can be looked up by id
 * (case-insensitive) and new formats can be registered at runtime.
 *
 * Port of {@code org.bars4j.format.BarcodeRegistry}.
 */
export class BarcodeRegistry {
  private static INSTANCE: BarcodeRegistry | undefined;

  private readonly formatsMap = new Map<string, BarcodeFormat>();

  private constructor() {
    registerBuiltinFormats(this);
  }

  static getInstance(): BarcodeRegistry {
    if (BarcodeRegistry.INSTANCE === undefined) {
      BarcodeRegistry.INSTANCE = new BarcodeRegistry();
    }
    return BarcodeRegistry.INSTANCE;
  }

  register(format: BarcodeFormat): void {
    this.formatsMap.set(normalize(format.id()), format);
  }

  get(id: string): BarcodeFormat {
    const format = this.formatsMap.get(normalize(id));
    if (format === undefined) {
      throw new Error("Unknown barcode format: " + id);
    }
    return format;
  }

  contains(id: string): boolean {
    return this.formatsMap.has(normalize(id));
  }

  ids(): string[] {
    return [...this.formatsMap.keys()];
  }

  formats(): BarcodeFormat[] {
    return [...this.formatsMap.values()];
  }
}

function normalize(id: string): string {
  return id.trim().toUpperCase();
}
