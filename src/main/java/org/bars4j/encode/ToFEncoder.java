/* ================================================================
 * Bars4J : Java Barcode Library
 * ================================================================
 *
 * Project Info:  https://github.com/ronison/bars4j
 * Project Lead:  Flavio Sampaio (contato@ronison.com.br);
 *
 * (C) Copyright 2005, by Favio Sampaio
 *
 * This library is free software; you can redistribute it and/or modify it underthe terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package org.bars4j.encode;

public abstract class ToFEncoder implements BarcodeEncoder {
	
	
	protected final BarSet [] CODES_WIDTH = new BarSet[]{
            new BarSet("00110"), new BarSet("10001"), new BarSet("01001"),
            new BarSet("11000"), new BarSet("00101"), new BarSet("10100"),
            new BarSet("01100"), new BarSet("00011"), new BarSet("10010"),
            new BarSet("01010")
	};
	
	protected BarSet START_CHAR = new BarSet("101000");
	
	protected BarSet STOP_CHAR = new BarSet("10001");
    
    protected BarSet INTER_CHAR = new BarSet("0");
    
	/* (non-Javadoc)
	 * @see org.jbarcode.BarcodeEncoder#computeCheckSum(java.lang.String)
	 */
	@Override
	public String computeCheckSum(String text) throws InvalidAtributeException {
        int sum = 0; 
        for(int charPos = 0; charPos < text.length(); charPos++) {
            if( charPos % 2 == 0 ){
                sum += (charToInt(text.charAt(charPos))*3);
            } else {
                sum += charToInt(text.charAt(charPos));
            }   
        }

        int result = sum%10;
        if( result == 0 ){
            return "0";
        } else{
            return String.valueOf(10 - result);
        }
	}
    
	protected static int charToInt(char val) throws NumberFormatException{
    	if(!Character.isDigit(val)){
    		throw new NumberFormatException("Invalid number");
    	}
        return val - '0';
    }

}
