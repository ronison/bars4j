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

public class EAN5Encoder extends EANEncoder {

	private static EAN5Encoder instance;
	
	protected BarSet [] DIGIT_PARITY = new BarSet[]{
			new BarSet("11000"), new BarSet("10100"), new BarSet("10010"),
			new BarSet("10001"), new BarSet("01100"), new BarSet("00110"),
			new BarSet("00011"), new BarSet("01010"), new BarSet("01001"),
			new BarSet("00101")
		};
	
	protected static final BarSet START = new BarSet("01011"); 
	
	protected static final BarSet SEPARATOR = new BarSet("01"); 

	   
    private EAN5Encoder() {
        
    }
    
    public static EAN5Encoder getInstance(){
        if(instance == null){
            instance = new EAN5Encoder();
        }
        return instance;
    }

	public BarSet[] encode(String texto) throws InvalidAtributeException {
		if( texto.length() != 5  ){
			throw new InvalidAtributeException("[EAN5] Invalid text length (" + texto.length() + ").");
		}
		
		BarSet [] result = new BarSet[10];
		try{		
            int checksum = Integer.valueOf(computeCheckSum(texto));
			//tres barras iniciais
			result[0] = START;
			//gera a primeira parte do codigo
			for (int i = 1; i <= 5; i++) {
				int atual = charToInt(texto.charAt(i-1));
				if( DIGIT_PARITY[checksum].get(i-1) ){			
					result[(2*i)-1] = CODES[atual].reverse();
					
				} else {
					result[(2*i)-1] = CODES[atual].xorTrue();
				}
				if(i<5) {
					result[2*i] = SEPARATOR;
				}
			}
		} catch( NumberFormatException nfexc ){
			throw new InvalidAtributeException("[EAN5] Only numbers suported.");
		}
		return result;
	}
	
	public String computeCheckSum(String text) throws InvalidAtributeException{
		
		int sum = 0;
		boolean odd = true;
		for(int charPos = 0; charPos < text.length(); charPos++) {
			if( odd ){
				sum += 3*charToInt(text.charAt(charPos));
			} else {
				sum += 9*charToInt(text.charAt(charPos));
			}
			odd = !odd; 
		}

		return Integer.toString(sum%10);
	}
    
    public String toString(){
        return "EAN 5";
    }

}
