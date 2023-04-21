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

public class Matrix2of5Encoder extends Standard2of5Encoder {

	private static Matrix2of5Encoder instance;

	public Matrix2of5Encoder() {
		super.START_CHAR = new BarSet("100000");
		super.STOP_CHAR = new BarSet("10000");
	}
	
	public static Matrix2of5Encoder getInstance(){
        if(instance == null){
            instance = new Matrix2of5Encoder();
        }
        return instance;
    }
	
	/* (non-Javadoc)
	 * @see org.jbarcode.BarcodeEncoder#encode(java.lang.String)
	 */
	public BarSet[] encode(String text) throws InvalidAtributeException {
		if( text.length() < 1 ){
			throw new InvalidAtributeException("[Matrix2of5] Invalid text length (" + text.length() + ").");
		}
				
		BarSet [] result = new BarSet[text.length() + 2];
		try{		
            int codeSize = 6;
			//tres barras iniciais
			result[0] = START_CHAR;
			//gera a primeira parte do codigo
			for (int i = 0; i < text.length(); i++) {
				int ind = charToInt(text.charAt(i));
                result[i+1] = new BarSet(codeSize);
                for(int j = 0; j < CODES_WIDTH[ind].length(); j++){
                    if(CODES_WIDTH[ind].get(j)){
                        result[i+1].set(j);
                    }
                }
			}
			//barras centrais que dividem o codigo
			result[result.length - 1] = STOP_CHAR;
		} catch( NumberFormatException nfexc ){
			throw new InvalidAtributeException("[Matrix2of5] Only numbers suported.");
		}
		return result;
	}
	
    public String toString(){
        return "Matrix 2of5";
    }

}
