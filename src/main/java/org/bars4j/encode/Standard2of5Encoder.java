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


/**
 * TODO: Description.
 * 
 * @author Flavio Sampaio
 * @since 0.1
 */
public class Standard2of5Encoder extends ToFEncoder {
    
    private static Standard2of5Encoder instance;
    
    
    protected Standard2of5Encoder(){
        
    }
    
    public static Standard2of5Encoder getInstance(){
        if(instance == null){
            instance = new Standard2of5Encoder();
        }
        return instance;
    }

	/* (non-Javadoc)
	 * @see org.jbarcode.BarcodeEncoder#encode(java.lang.String)
	 */
	public BarSet[] encode(String text) throws InvalidAtributeException {
		if( text.length() < 1 ){
			throw new InvalidAtributeException("[Standard2of5] Invalid text length (" + text.length() + ").");
		}
				
		BarSet [] result = new BarSet[text.length() + 2];
		try{		
            int codeSize = 10;
			//tres barras iniciais
			result[0] = START_CHAR;
			//gera a primeira parte do codigo
			for (int i = 0; i < text.length(); i++) {
				int ind = charToInt(text.charAt(i));
                result[i+1] = new BarSet(codeSize);
                int z = 0;
                for(int j = 0; j < CODES_WIDTH[ind].length(); j++){
                    if(CODES_WIDTH[ind].get(j)){
                        result[i+1].set(z);
                    }
                    z+=2;
                }
			}
			//barras centrais que dividem o codigo
			result[result.length - 1] = STOP_CHAR;
		} catch( NumberFormatException nfexc ){
			throw new InvalidAtributeException("[Standard2of5] Only numbers suported.");
		}
		return result;
	}
    
    public String toString(){
        return "Standard/Industrial 2of5";
    }

}
