import type { DataMatrixPlacement } from "./data-matrix-placement.ts";
import type { DataMatrixSymbolInfo } from "./data-matrix-symbol-info.ts";

/**
 * Draws the complete ECC200 symbol by framing every data region with its finder pattern.
 *
 * <p>An ECC200 symbol is tiled from data regions. Each region is bounded by a solid dark "L" along
 * its left and bottom sides and by an alternating light/dark border along its top and right sides.
 * The assembler visits the regions in row-major order, paints those borders and copies the placed
 * data modules into the interior.
 *
 * Port of {@code org.bars4j.encode.matrix.datamatrix.DataMatrixSymbolAssembler}.
 */

/**
 * Produces the module grid of the finished symbol.
 *
 * @param placement  the placed data regions
 * @param symbolInfo the symbol geometry
 * @return {@code symbolHeight()} rows of {@code symbolWidth()} modules, {@code true} for dark
 */
export function assemble(
  placement: DataMatrixPlacement,
  symbolInfo: DataMatrixSymbolInfo,
): boolean[][] {
  const regionRows = symbolInfo.matrixHeight();
  const regionColumns = symbolInfo.matrixWidth();
  const regionsDown = Math.trunc(symbolInfo.symbolDataHeight() / regionRows);
  const regionsAcross = Math.trunc(symbolInfo.symbolDataWidth() / regionColumns);

  const height = symbolInfo.symbolHeight();
  const width = symbolInfo.symbolWidth();
  const symbol = new Array<boolean[]>(height);
  for (let row = 0; row < height; row++) {
    symbol[row] = new Array<boolean>(width).fill(false);
  }

  for (let tileRow = 0; tileRow < regionsDown; tileRow++) {
    for (let tileColumn = 0; tileColumn < regionsAcross; tileColumn++) {
      drawRegion(symbol, placement, tileRow, tileColumn, regionRows, regionColumns);
    }
  }
  return symbol;
}

function drawRegion(
  symbol: boolean[][],
  placement: DataMatrixPlacement,
  tileRow: number,
  tileColumn: number,
  regionRows: number,
  regionColumns: number,
): void {
  const originRow = tileRow * (regionRows + 2);
  const originColumn = tileColumn * (regionColumns + 2);
  const span = regionColumns + 2;

  // Top border: alternating modules, dark at even offsets.
  for (let offset = 0; offset < span; offset++) {
    symbol[originRow][originColumn + offset] = offset % 2 === 0;
  }

  for (let localRow = 0; localRow < regionRows; localRow++) {
    const dataRow = tileRow * regionRows + localRow;
    const symbolRow = originRow + 1 + localRow;

    symbol[symbolRow][originColumn] = true; // left border: solid dark
    for (let localColumn = 0; localColumn < regionColumns; localColumn++) {
      const dataColumn = tileColumn * regionColumns + localColumn;
      symbol[symbolRow][originColumn + 1 + localColumn] = placement.getBit(dataColumn, dataRow);
    }
    symbol[symbolRow][originColumn + span - 1] = dataRow % 2 === 0; // right border
  }

  // Bottom border: solid dark.
  for (let offset = 0; offset < span; offset++) {
    symbol[originRow + regionRows + 1][originColumn + offset] = true;
  }
}
