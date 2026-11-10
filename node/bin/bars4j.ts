#!/usr/bin/env node
import { writeFileSync } from "node:fs";

import {
  BarcodeGenerator,
  BarcodeRegistry,
  encodePng,
  linearDefaults,
  LinearBarcodeFormat,
  renderSpec,
} from "../src/index.ts";

/**
 * Command-line interface for {@code @bars4j/core}.
 *
 * <pre>
 * bars4j &lt;format&gt; &lt;text&gt; [--svg|--png FILE] [--module-size N] [--no-text]
 * bars4j --list
 * </pre>
 */

interface Options {
  id: string;
  text: string;
  png?: string;
  out?: string;
  moduleSize: number;
  noText: boolean;
}

function usage(message?: string): never {
  if (message !== undefined) {
    process.stderr.write(`bars4j: ${message}\n\n`);
  }
  process.stdout.write(
    "Usage: bars4j <format> <text> [options]\n"
      + "       bars4j --list\n\n"
      + "Options:\n"
      + "  --svg              emit the SVG to stdout (default)\n"
      + "  --png <file>       write a PNG raster to <file>\n"
      + "  --out <file>       write the SVG to <file> instead of stdout\n"
      + "  --module-size <n>  module size in pixels for 2D formats (default 4)\n"
      + "  --no-text          do not draw the human-readable text (1D formats)\n"
      + "  --list             list the registered format ids\n",
  );
  process.exit(message === undefined ? 0 : 1);
}

function parse(argv: string[]): Options {
  const positional: string[] = [];
  let png: string | undefined;
  let out: string | undefined;
  let moduleSize = 4;
  let noText = false;

  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    switch (arg) {
      case "--png":
        png = argv[++i];
        break;
      case "--out":
        out = argv[++i];
        break;
      case "--module-size":
        moduleSize = Number(argv[++i]);
        break;
      case "--no-text":
        noText = true;
        break;
      case "--svg":
        break;
      case "--list":
        process.stdout.write(BarcodeRegistry.getInstance().ids().sort().join("\n") + "\n");
        process.exit(0);
        break;
      default:
        if (arg.startsWith("--")) {
          usage(`unknown option ${arg}`);
        }
        positional.push(arg);
    }
  }

  if (positional.length < 2) {
    usage("missing <format> and <text>");
  }
  if (!Number.isInteger(moduleSize) || moduleSize <= 0) {
    usage(`invalid --module-size ${moduleSize}`);
  }
  return { id: positional[0], text: positional[1], png, out, moduleSize, noText };
}

const options = parse(process.argv.slice(2));
const spec = renderSpec(1, 70, 2.0, options.moduleSize, "#000000", "#ffffff");

try {
  const format = BarcodeRegistry.getInstance().get(options.id);
  const bare = options.noText && format instanceof LinearBarcodeFormat;

  const svg = bare
    ? format.withOptions({ ...format.options(), showText: false }).toSvg(options.text, linearDefaults())
    : BarcodeGenerator.of(options.id).with(spec).toSvg(options.text);

  if (options.out !== undefined) {
    writeFileSync(options.out, svg);
    process.stderr.write(`bars4j: wrote ${options.out}\n`);
  } else if (options.png === undefined) {
    process.stdout.write(svg);
  }

  if (options.png !== undefined) {
    const image = bare
      ? format.withOptions({ ...format.options(), showText: false }).toImage(options.text, linearDefaults())
      : BarcodeGenerator.of(options.id).with(spec).toImage(options.text);
    writeFileSync(options.png, encodePng(image));
    process.stderr.write(`bars4j: wrote ${options.png} (${image.width}x${image.height})\n`);
  }
} catch (error) {
  usage(error instanceof Error ? error.message : String(error));
}
