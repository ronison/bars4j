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
 * @since 0.2
 */
public class Code11Encoder implements BarcodeEncoder {
	private static Code11Encoder instance;

	protected BarSet[] CODES_WIDTH = new BarSet[] { new BarSet("101011"),
			new BarSet("1101011"), new BarSet("1001011"),
			new BarSet("1100101"), new BarSet("1011011"),
			new BarSet("1101101"), new BarSet("1001101"),
			new BarSet("1010011"), new BarSet("1101001"), new BarSet("110101"),
			new BarSet("101101") };

	protected BarSet INTER_CHAR = new BarSet("0");

	protected BarSet START_STOP_CHAR = new BarSet("1011001");

	protected Code11Encoder() {

	}

	public static BarcodeEncoder getInstance() {
		if (instance == null) {
			instance = new Code11Encoder();
		}
		return instance;
	}


	public String computeCheckSum(String text) throws InvalidAtributeException {
		if (text == null) {
			throw new InvalidAtributeException("[Code11] Text must not be null.");
		}
		// Calc 'C' checkdigit char (modulo 11, weights 1..10 from the right)
		int check = 0;
		for (int i = text.length() - 1; i >= 0; i--) {
			int weight = (text.length() - i) % 10;
			weight = weight == 0 ? 10 : weight;
			check += getCharIndex(text.charAt(i)) * weight;
		}
		String result = checkDigitToString(check % 11);

		if (text.length() >= 10) {
			// Calc 'K' checkdigit char (modulo 11, weights 1..9 from the right, with the C
			// check digit included as the right-most character).
			check = 0;
			String temp = text + result;
			for (int i = temp.length() - 1; i >= 0; i--) {
				int weight = (temp.length() - i) % 9;
				weight = weight == 0 ? 9 : weight;
				check += getCharIndex(temp.charAt(i)) * weight;
			}
			result += checkDigitToString(check % 11);
		}
		return result;
	}

	/**
	 * Renders a check digit value: the value 10 is the dash character, per the Code 11
	 * specification.
	 */
	private static String checkDigitToString(int value) {
		return value == 10 ? "-" : Integer.toString(value);
	}

	public BarSet[] encode(String text) throws InvalidAtributeException {
		if( text == null || text.length() < 1  ){
            throw new InvalidAtributeException("[Code11] Invalid text length (" + (text == null ? "null" : text.length()) + ").");
        }
        
        BarSet [] result = new BarSet[(text.length()*2) + 3];
        result[0] = START_STOP_CHAR;
        result[1] = INTER_CHAR;
        for (int i = 0; i < text.length(); i++) {
            result[(i*2) + 2] = CODES_WIDTH[getCharIndex(text.charAt(i))];
            result[(i*2) + 3] = INTER_CHAR;
        }
        result[result.length - 1] = START_STOP_CHAR;
        return result;
	}

	private int getCharIndex(char val) throws InvalidAtributeException {
		if (val >= '0' && val <= '9') {
			return val - '0';
		} else if (val == '-') {
			return 10;
		}
		throw new InvalidAtributeException("[Code11] The text contains unsuported chars.");
	}
	
	public String toString(){
        return "CODE 11";
    }

}
